package com.cohesiveintegrations.px.android.feed.metacard;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cohesiveintegrations.px.android.audit.AbstractAuditor;
import com.cohesiveintegrations.px.android.audit.IngestRequestAudit;
import com.cohesiveintegrations.px.android.audit.IngestResponseAudit;
import com.cohesiveintegrations.px.android.audit.SearchRequestAudit;
import com.cohesiveintegrations.px.android.audit.SearchResponseAudit;
import com.cohesiveintegrations.px.android.authentication.AuthenticationService;
import com.cohesiveintegrations.px.android.authentication.UserStore;
import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.cohesiveintegrations.px.android.util.AppUtils;
import com.google.api.client.util.SslUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class MetacardIngestService extends IntentService {

    public static final String BROADCAST_ACTION = MetacardIngestService.class.getName();

    //TODO tech-debt of hardcoded URLs
    protected static final String URL_PREFIX = "http://";
    protected static final String URL_SUFFIX = "/services";
    protected static final String CATALOG_CONTEXT = "/catalog";
    protected static final String CONTENT_CONTEXT = "/content";

    public static final String JSON_DATA_KEY = "JSON";
    public static final String MEDIA_PATH = "MEDIA_PATH";
    public static final String MEDIA_TYPE = "MEDIA_TYPE";

    // multipart constants
    private static final String BOUNDRY_PREFIX = "--";
    private static final String NEW_LINE = "\r\n";

    private static final String RESOURCE_KEY = "$RESOURCE$";
    private static final String RESOURCE_SIZE_KEY = "$RES_SIZE$";

    public MetacardIngestService() {
        super(MetacardIngestService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        String jsonData = intent.getStringExtra(JSON_DATA_KEY);
        String imagePath = intent.getStringExtra(MEDIA_PATH);
        String mediaType = intent.getStringExtra(MEDIA_TYPE);
        String serverId = intent.getStringExtra(Server.SERVER_ID);
        Server server = ServerCollection.getInstance().getServer(serverId);
        if (server != null) {
            try {
                File imageFile = new File(imagePath);
                String imageSize = "";
                if (imageFile.isFile()) {
                    imageSize = Long.toString(imageFile.length());
                }

                String imageUrl = ingestImage(imageFile, mediaType, server);

                String metacardId = ingestMetacard(jsonData, imageUrl, imageSize, server);
                sendNotification(BROADCAST_ACTION, metacardId);
            } catch (Exception e) {
                Log.e(MetacardIngestService.class.getSimpleName(), "Could not create new metacard from image.", e);
            }
        } else {
            Log.w(MetacardIngestService.class.getSimpleName(), "No servers to ingest to, cannot perform ingest.");
        }

    }

    /**
     * Ingests the full image into the content framework and gets back a URL to the image.
     *
     * @param imageFile Location of the image on disk.
     * @param server    Server to ingest to.
     * @return URL to the image.
     */
    private String ingestImage(File imageFile, String mediaType, Server server) throws Exception {
        String boundary = UUID.randomUUID().toString();
        DataOutputStream outputStream = null;
        try {
            HttpURLConnection urlConnection = createURLConnection(server.getProtocol() + "://" + server.getAddress() + ":" + server.getPort() + URL_SUFFIX + CONTENT_CONTEXT, "multipart/form-data; boundary=" + boundary);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setChunkedStreamingMode(1024);

            Intent auditIntent = new Intent(getApplicationContext(), IngestRequestAudit.class);
            auditIntent.putParcelableArrayListExtra( AbstractAuditor.AUDIT_HEADERS, AppUtils.getHeaderItems( urlConnection.getRequestProperties() ) );
            auditIntent.putExtra(AbstractAuditor.SERVICE_URL_PATH, URL_SUFFIX + CONTENT_CONTEXT );
            auditIntent.putExtra(AbstractAuditor.AUDIT_LIST,  new String[]{ mediaType == null ? "application/unknown" : mediaType } );
            startService(auditIntent);

            //urlConnection.connect();
            outputStream = new DataOutputStream(urlConnection.getOutputStream());

            // directive
            outputStream.writeBytes(BOUNDRY_PREFIX + boundary + NEW_LINE);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"directive\"" + NEW_LINE);
            outputStream.writeBytes(NEW_LINE);
            outputStream.writeBytes("STORE" + NEW_LINE);

            // image
            outputStream.writeBytes(BOUNDRY_PREFIX + boundary + NEW_LINE);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + AppUtils.replaceEach(imageFile.getName(), ":", "") + "\"" + " Content-Type: " + mediaType + NEW_LINE);
            outputStream.writeBytes(NEW_LINE);
            //TODO clean up memory usage
            FileInputStream inputStream = new FileInputStream(imageFile);
            byte[] imageBytes = new byte[(int) imageFile.length()];
            inputStream.read(imageBytes);
            outputStream.write(imageBytes);
            outputStream.writeBytes(NEW_LINE);

            outputStream.writeBytes(BOUNDRY_PREFIX + boundary + BOUNDRY_PREFIX + NEW_LINE);
            outputStream.flush();
            outputStream.close();
            int responseCode = urlConnection.getResponseCode();
            String imageUrl = null;
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                imageUrl = urlConnection.getHeaderField("Location");
            } else {
                Log.w(MetacardIngestService.class.getName(), "Did not get a proper response from server.");
            }

            auditIntent = new Intent(getApplicationContext(), IngestResponseAudit.class);
            auditIntent.putParcelableArrayListExtra( AbstractAuditor.AUDIT_HEADERS, AppUtils.getHeaderItems( urlConnection.getHeaderFields() ) );
            auditIntent.putExtra(AbstractAuditor.SERVICE_URL_PATH, URL_SUFFIX + CONTENT_CONTEXT );
            auditIntent.putExtra(AbstractAuditor.AUDIT_LIST,  new String[]{ imageUrl } );
            startService(auditIntent);

            return imageUrl;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }



    /**
     * Ingests the newly created metacard into the catalog framework.
     *
     * @param jsonData Metacard encoded in json
     * @param imageUrl URL to the previously-ingested image
     * @param server   Server to ingest to
     * @return ID of the ingested metacard.
     * @throws Exception
     */
    private String ingestMetacard(String jsonData, String imageUrl, String imageSize, Server server) throws Exception {
        OutputStream outputStream = null;
        try {
            HttpURLConnection urlConnection = createURLConnection(URL_PREFIX + server.getAddress() + ":" + server.getPort() + URL_SUFFIX + CATALOG_CONTEXT, "application/json");
            urlConnection.connect();
            outputStream = urlConnection.getOutputStream();
            jsonData = AppUtils.replaceEach(jsonData, RESOURCE_KEY, imageUrl);
            jsonData = AppUtils.replaceEach(jsonData, RESOURCE_SIZE_KEY, imageSize);
            outputStream.write(jsonData.getBytes());
            int responseCode = urlConnection.getResponseCode();

            // post should return 201 if metacard is created successfully
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                return urlConnection.getHeaderField("Location");
            } else {
                throw new Exception("Did not get a CREATED (201) response from server. Got " + responseCode + " instead");
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    private HttpURLConnection createURLConnection(String urlStr, String contentType) throws Exception {
        URL serverUrl = new URL(urlStr);
        HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        //urlConnection.setConnectTimeout(NETWORK_TIMEOUT);
        //urlConnection.setReadTimeout(NETWORK_TIMEOUT);
        urlConnection.setRequestProperty("Content-Type", contentType);
        urlConnection.setRequestProperty(Metacard.QUERY_IDENTIFIER, UUID.randomUUID().toString());
        if (serverUrl.getProtocol().equalsIgnoreCase("https")) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(SslUtils.trustAllSSLContext().getSocketFactory());
            ((HttpsURLConnection) urlConnection).setHostnameVerifier(SslUtils.trustAllHostnameVerifier());
        }
        // add user credentials if a user is logged in
        String user = UserStore.getInstance().getUserId();
        if ( user != null && !user.isEmpty() ){
            urlConnection.setRequestProperty(AuthenticationService.USERID, user);
        }
        return urlConnection;
    }

    protected void sendNotification(String actionName, String id) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(actionName);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra("ID", id);
        sendBroadcast(broadcastIntent);
    }

}
