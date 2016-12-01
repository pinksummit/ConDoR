package com.cohesiveintegrations.px.android.status.server;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ResponseListenerServer extends NanoHTTPD {

    Activity activity;

    public ResponseListenerServer(String hostname, int port, Activity activity) {
        super(hostname, port);
        this.activity = activity;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        Map<String, String> data = new HashMap<>();
        try {
            session.parseBody(data);
            if (!data.isEmpty()) {
                String json = data.get("postData");
                JsonObject jsonObj = new JsonParser().parse(json).getAsJsonObject();
                JsonArray responses = jsonObj.getAsJsonArray("responses");
                JsonObject site = responses.get(0).getAsJsonObject();
                String siteName = site.get("id").getAsString();
                String siteAddress = site.get("ipAddress").getAsString();
                int port = site.get("port").getAsInt();
                Uri uri = Uri.parse(site.get("url").getAsString());
                String protocol = uri.getScheme();
                boolean ingest = hasIngestService( jsonObj );
                Server server = new Server(siteName, protocol, siteAddress, port, false, false, ingest);
                ServerCollection.getInstance().getServerMap().put(siteName, server);
                // send server to the status service to be checked
                Intent intent = new Intent(Intent.ACTION_SYNC, null, activity, ServerStatusService.class);
                intent.putExtra(Server.SERVER_ID, siteName);
                activity.startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasIngestService( JsonObject obj ){ return true; }


}
