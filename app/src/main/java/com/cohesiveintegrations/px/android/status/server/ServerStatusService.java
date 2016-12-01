package com.cohesiveintegrations.px.android.status.server;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.cohesiveintegrations.px.android.AbstractNotificationService;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.google.api.client.util.SslUtils;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ServerStatusService extends AbstractNotificationService {

    public static final String BROADCAST_ACTION = ServerStatusService.class.getName();

    public ServerStatusService() {
        super(ServerStatusService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String serverId = intent.getStringExtra(Server.SERVER_ID);
        if (serverId == null || serverId.isEmpty()) {
            // get status for ALL servers
            for (Server curServer : serverCollection.getServerList()) {
                checkStatusHead(curServer);
            }
        } else {
            // get status for ONE server
            Server server = serverCollection.getServer(serverId);

            // update server status
            checkStatusHead(server);
        }

        // send broadcast to UI that servers have been updated
        sendNotification(BROADCAST_ACTION);

    }

    private void checkStatusHead(Server server) {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                URL serverUrl = new URL(server.getProtocol() + "://" + server.getAddress() + ":" + server.getPort() + URL_SUFFIX);
                HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();
                urlConnection.setRequestMethod("HEAD");
                urlConnection.setConnectTimeout(NETWORK_TIMEOUT);
                urlConnection.setReadTimeout(NETWORK_TIMEOUT);
                if (serverUrl.getProtocol().equalsIgnoreCase("https")) {
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(SslUtils.trustAllSSLContext().getSocketFactory());
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(SslUtils.trustAllHostnameVerifier());
                }
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                // head should only return 200 if service is up
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    setServerSuccessful(server);
                } else {
                    setServerFailed(server, responseCode + " Server Error");
                }

            } catch (Exception mue) {
                setServerFailed(server, "Could Not Connect");
            }
        } else {
            setServerFailed(server, "Network Unavailable");

        }
    }

    private void setServerFailed(Server server, String errorMessage) {
        server.setAvailable(false);
        server.setEnabled(false);
        server.setMessage(errorMessage);
    }

    private void setServerSuccessful(Server server) {
        if (!server.isAvailable()) {
            server.setEnabled(true);
        }
        server.setAvailable(true);
        server.setMessage("");
    }

}
