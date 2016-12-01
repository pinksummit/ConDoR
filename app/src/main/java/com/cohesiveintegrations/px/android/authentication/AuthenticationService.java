package com.cohesiveintegrations.px.android.authentication;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cohesiveintegrations.px.android.BuildConfig;
import com.cohesiveintegrations.px.android.audit.AbstractAuditor;
import com.cohesiveintegrations.px.android.audit.LoginAudit;
import com.cohesiveintegrations.px.android.audit.SearchRequestAudit;
import com.cohesiveintegrations.px.android.util.AppUtils;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.auth.openidconnect.IdTokenResponse;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs authentication operations over the network using OpenID Connect
 */
public class AuthenticationService extends IntentService {


    public static final String USERID = "USERID";

    public static final String AUTH_CODE = "authCode";
    public static final String ACCESS_TOKEN = "accessToken";

    public static final String PAYLOAD = "payload";
    public static final String USER_ATTRIBUTES = "userAttributes";

    public static final String BROADCAST_ACTION = AuthenticationService.class.getSimpleName();

    public AuthenticationService() {
        super(AuthenticationService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String authCode = intent.getStringExtra(AUTH_CODE);
        if (authCode != null && !authCode.isEmpty()) {
            IdToken.Payload payload = getPayload(authCode);
            HashMap<String, Object> payloadMap = new HashMap<>();
            String authResult = "FAILURE";
            if (payload != null) {
                payloadMap.putAll(payload.getUnknownKeys());
                payloadMap.put("subject", payload.getSubject());
                String[] subjectParts = payload.getSubject().split(",");
                for (String part : subjectParts) {
                    if (part.startsWith("CN=")) {
                        String nameParts[] = part.split("=");
                        payloadMap.put("user", nameParts[1]);
                    }
                }
                Map<String, String> userCredentials = UserStore.getInstance().getUserCredentials();
                for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                    userCredentials.put(entry.getKey(), entry.getValue().toString());
                }
                authResult = "SUCCESS";
            }

            Intent auditIntent = new Intent(getApplicationContext(), LoginAudit.class);
            auditIntent.putExtra(AbstractAuditor.SERVICE_URL_PATH, AuthenticationConstants.PING_AUTHZ_ENDPOINT);
            auditIntent.putExtra(AbstractAuditor.AUDIT_LIST, new String[]{ authResult });
            startService(auditIntent);

            sendNotification(PAYLOAD, payloadMap);
        }

        String accessToken = intent.getStringExtra(ACCESS_TOKEN);
        if (accessToken != null && !accessToken.isEmpty()) {
            Map<String, Object> attributes = getUserAttributes(accessToken);
        }
    }

    private IdToken.Payload getPayload(String authCode) {
        // make a transport for ssl
        HttpTransport transport;
        try {
            transport = new NetHttpTransport.Builder().doNotValidateCertificate().build();
        } catch (GeneralSecurityException gse) {
            // should not get here, but just in case...
            Log.e(AuthenticationService.class.getSimpleName(), "Could not create a new transport with certificate and hostname validation disabled. Using default transport.", gse);
            transport = AndroidHttp.newCompatibleTransport();
        }
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                transport,
                new GsonFactory(),
                new GenericUrl(AuthenticationConstants.PING_TOKEN_ENDPOINT),
                new BasicAuthentication(AuthenticationConstants.PING_CLIENT_ID, AuthenticationConstants.PING_SECRET),
                AuthenticationConstants.PING_CLIENT_ID,
                AuthenticationConstants.PING_AUTHZ_ENDPOINT
        ).build();

        TokenRequest request = flow.newTokenRequest(authCode);

        // Again, we need to set the `redirect_uri` parameter. This time a dedicated method
        // setRedirectUri() doesn't exist for some reason.
        request.set("redirect_uri", AuthenticationConstants.PING_REDIRECT_URL);

        try {
            IdTokenResponse response = IdTokenResponse.execute(request);
            IdToken idToken = response.parseIdToken();
            IdToken.Payload payload = idToken.getPayload();
            Collection<Object> values = payload.values();
            Map<String, Object> unknownKeys = payload.getUnknownKeys();
            String subject = payload.getSubject();
            // long secondsLive = payload.getExpirationTimeSeconds() - payload.getAuthorizationTimeSeconds();
            values.size();

            return payload;
        } catch (Exception e) {
            Log.e(AuthenticationService.class.getSimpleName(), "Could not get idToken.", e);
            return null;
        }
    }

    private Map<String, Object> getUserAttributes(String accessToken) {
        Map<String, Object> attributes = new HashMap<>();

        return attributes;
    }

    private void sendNotification(String contentName, Serializable content) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BROADCAST_ACTION);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(contentName, content);
        sendBroadcast(broadcastIntent);
    }

}
