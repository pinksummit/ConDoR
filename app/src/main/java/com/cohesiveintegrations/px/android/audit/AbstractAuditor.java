package com.cohesiveintegrations.px.android.audit;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.util.Log;

import com.cohesiveintegrations.px.android.BuildConfig;
import com.cohesiveintegrations.px.android.authentication.AuthenticationService;
import com.cohesiveintegrations.px.android.authentication.UserStore;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.cohesiveintegrations.px.android.util.AppUtils;
import com.google.api.client.util.SslUtils;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public abstract class AbstractAuditor extends IntentService {

    public static final String LOGGER = "CondorAuditServiceLog";
    public static final String AUDIT_LOG_PARAMETER = "AuditLog";
    public static final String AUDIT_MESSAGE_PARAMETER = "AuditMessage";
    public static final String SERVICE_URL_PATH = "ServiceUrlPath";
    public static final String AUDIT_LIST = "AuditList";
    public static final String AUDIT_HEADERS = "AuditHeaders";

    public static final String AUDIT_CLIENT_LOGIN = "AuditClientLogin";
    public static final String AUDIT_CLIENT_SEARCH_REQUEST = "AuditClientSearchRequest";
    public static final String AUDIT_CLIENT_SEARCH_RESPONSE = "AuditClientSearchResponse";
    public static final String AUDIT_CLIENT_RETRIEVE_REQUEST = "AuditClientRetrieveRequest";
    public static final String AUDIT_CLIENT_INGEST_REQUEST = "AuditClientIngestRequest";
    public static final String AUDIT_CLIENT_INGEST_RESPONSE = "AuditClientIngestResponse";

    public static final String AUDIT_HEADER_HOLDER =
    "<di2e-audit-http:Header><di2e-audit-http:Name>$HEADER_NAME$</di2e-audit-http:Name><di2e-audit-http:Value>$HEADER_VALUE$</di2e-audit-http:Value></di2e-audit-http:Header>";

    public AbstractAuditor(String name) {
        super(name);
    }

    protected static final int NETWORK_TIMEOUT = 1000;
    private static final String URL_SUFFIX = "/services/audit/endpoint";

    protected ServerCollection serverCollection = ServerCollection.getInstance();

    protected abstract String getAuditMessage( Server server, String urlPath, String[] extras, List<Parcelable> headers );

    protected abstract String getAuditLogType();


    @Override
    protected void onHandleIntent(Intent intent) {
        List<Server> servers = serverCollection.getServerList();
        if (servers != null && !servers.isEmpty()) {
            Server server = servers.get(0);
            audit(server, intent.getStringExtra(SERVICE_URL_PATH), intent.getStringArrayExtra(AUDIT_LIST), (ArrayList<Parcelable>)intent.getParcelableArrayListExtra(AUDIT_HEADERS) );
        }
    }

    protected String formatUserForAudit() {
        String user = UserStore.getInstance().getUserId();
        if (user == null || user.isEmpty() ){
            user = "Unknown";
        }
        return user;
    }

    public String getHeaderXML( List<Parcelable> headers ) {
        StringBuilder sb = new StringBuilder();
        if (headers != null){
            for (Parcelable parcelable : headers) {
                HeaderItem header = (HeaderItem) parcelable;
                sb.append(AppUtils.replaceEach(AbstractAuditor.AUDIT_HEADER_HOLDER, new String[]{"$HEADER_NAME$", "$HEADER_VALUE$"}, new String[]{header.getHeaderName(), header.getHeaderValue()}));
            }
        }
        return sb.toString();
    }



    private void audit(Server server, String auditUrlPath, String[] extras, List<Parcelable> headers ) {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                URL serverUrl = new URL(server.getProtocol() + "://" + server.getAddress() + ":" + server.getPort() + URL_SUFFIX);
                HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
                urlConnection.setRequestProperty(AUDIT_LOG_PARAMETER, getAuditLogType());

                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                urlConnection.setDoOutput(true);

                DataOutputStream out = new DataOutputStream(
                        urlConnection.getOutputStream());
                out.writeBytes( getAuditMessage( server, auditUrlPath, extras, headers ) );
                out.flush();
                out.close();
                if (serverUrl.getProtocol().equalsIgnoreCase("https")) {
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(SslUtils.trustAllSSLContext().getSocketFactory());
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(SslUtils.trustAllHostnameVerifier());
                }
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();

                if (BuildConfig.DEBUG ){
                    Log.d(LOGGER, "Audit Message " + getAuditLogType() + " was published and returned: " + responseCode );
                }
            } catch (Exception e) {
                Log.e(LOGGER, "Could not publish Audit: " + e.getMessage(), e);
            }
        }
    }
}
