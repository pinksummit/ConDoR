package com.cohesiveintegrations.px.android;

import android.app.IntentService;
import android.content.Intent;

import com.cohesiveintegrations.px.android.data.server.ServerCollection;

import java.io.Serializable;

public abstract class AbstractNotificationService extends IntentService {

    // timeout for network operations
    protected static final int NETWORK_TIMEOUT = 1000;

    //TODO tech-debt of hardcoded URLs
    protected static final String URL_SUFFIX = "/services/cdr/search/rest";

    protected ServerCollection serverCollection = ServerCollection.getInstance();

    public AbstractNotificationService(String name) {
        super(name);
    }

    /**
     * Sends a broadcast notification that the service has completed an operation.
     *
     * @param actionName Name specifying which operation was completed.
     */
    protected void sendNotification(String actionName) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(actionName);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(broadcastIntent);
    }

    /**
     * Sends a broadcast notification that the service has completed an operation.
     *
     * @param actionName Name specifiying which operation was completed
     * @param contentName Name of content to add to the notification
     * @param content Content to add to the notification
     */
    protected void sendNotification(String actionName, String contentName, Serializable content) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(actionName);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(contentName, content);
        sendBroadcast(broadcastIntent);
    }
}
