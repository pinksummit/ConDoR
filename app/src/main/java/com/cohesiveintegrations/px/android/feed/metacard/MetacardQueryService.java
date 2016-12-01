package com.cohesiveintegrations.px.android.feed.metacard;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.cohesiveintegrations.px.android.AbstractNotificationService;
import com.cohesiveintegrations.px.android.audit.AbstractAuditor;
import com.cohesiveintegrations.px.android.audit.HeaderItem;
import com.cohesiveintegrations.px.android.audit.SearchRequestAudit;
import com.cohesiveintegrations.px.android.audit.SearchResponseAudit;
import com.cohesiveintegrations.px.android.authentication.AuthenticationService;
import com.cohesiveintegrations.px.android.authentication.UserStore;
import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.feed.FeedParser;
import com.cohesiveintegrations.px.android.util.AppUtils;
import com.google.api.client.http.UrlEncodedParser;
import com.google.api.client.util.SslUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class MetacardQueryService extends AbstractNotificationService {

    public static final String BROADCAST_PREFIX = MetacardQueryService.class.getName();
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    public static final String QUERY_TYPE = "queryType";

    /**
     * JSON-encoded query criteria
     */
    public static final String QUERY_CRITERIA = "queryCriteria";

    public static final String RESULT_KEY = "results";

    public enum QueryType {
        SPATIAL,
        LATEST
    }

    public MetacardQueryService() {
        super(MetacardQueryService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<Metacard> metacardList = new ArrayList<>();
        String serverName = intent.getStringExtra(Server.SERVER_ID);
        // create the query parameters
        QueryType queryType = (QueryType) intent.getSerializableExtra(QUERY_TYPE);
        String jsonCriteria = intent.getStringExtra(QUERY_CRITERIA);
        String queryStr;
        queryStr = createQueryParameters(queryType, jsonCriteria);
        if (serverName == null || serverName.isEmpty()) {
            // get results from ALL enabled servers
            for (Server curServer : serverCollection.getServerList()) {
                if (curServer.isEnabled()) {
                    handleSearch(metacardList, queryStr, curServer);
                }
            }
        } else {
            // get results from a single server
            Server server = serverCollection.getServer(serverName);
            if (server != null && server.isEnabled()) {
                handleSearch(metacardList, queryStr, server);
            }
        }

        // replace current metacards with ones that were just returned
        HashMap<String, Metacard> metacardMap = new HashMap<>();
        metacardMap.clear();
        for (Metacard curMetacard : metacardList) {
            metacardMap.put(curMetacard.getId(), curMetacard);
        }

        // send notification to UI with the updated metacards.
        sendNotification(BROADCAST_PREFIX + queryType.toString(), RESULT_KEY, metacardMap);

    }

    private void handleSearch(List<Metacard> metacardList, String queryStr, Server curServer) {
        QueryResponseExtras extras = new QueryResponseExtras();
        List<Metacard> resultList = performSearch(curServer, queryStr, extras);
        metacardList.addAll(resultList);
        Intent auditIntent = new Intent(getApplicationContext(), SearchRequestAudit.class);
        auditIntent.putParcelableArrayListExtra(AbstractAuditor.AUDIT_HEADERS, extras.getRequestHeaders());
        auditIntent.putExtra(AbstractAuditor.SERVICE_URL_PATH, URL_SUFFIX);
        auditIntent.putExtra(AbstractAuditor.AUDIT_LIST,
                new String[]{AppUtils.getUrl(curServer, URL_SUFFIX) + AppUtils.replaceEach(queryStr, new String[]{"&"}, new String[]{"&amp;"})
                });
        startService(auditIntent);

        auditIntent = new Intent(getApplicationContext(), SearchResponseAudit.class);
        auditIntent.putExtra(AbstractAuditor.SERVICE_URL_PATH, URL_SUFFIX);
        auditIntent.putParcelableArrayListExtra(AbstractAuditor.AUDIT_HEADERS, extras.getResponseHeaders());
        auditIntent.putExtra(AbstractAuditor.AUDIT_LIST,
                new String[]{
                        AppUtils.getUrl(curServer, URL_SUFFIX) + AppUtils.replaceEach(queryStr, new String[]{"&"}, new String[]{"&amp;"}),
                        extras.getContentSize(),
                        extras.getStatusCode(),
                        "" + resultList.size()
                });
        startService(auditIntent);
    }

    /**
     * Perform a search on the given server
     *
     * @param server Server to perform the search over
     */
    private List<Metacard> performSearch(Server server, String queryStr, QueryResponseExtras extras) {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        List<Metacard> metacardList = new ArrayList<>();
        if (networkInfo != null && networkInfo.isConnected()) {
            InputStream inputStream = null;
            try {
                URL url = new URL(AppUtils.getUrl(server, URL_SUFFIX) + queryStr);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //urlConnection.setReadTimeout(NETWORK_TIMEOUT);
                urlConnection.setConnectTimeout(NETWORK_TIMEOUT);
                urlConnection.setRequestProperty(Metacard.QUERY_IDENTIFIER, UUID.randomUUID().toString());
                if (url.getProtocol().equalsIgnoreCase("https")) {
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(SslUtils.trustAllSSLContext().getSocketFactory());
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(SslUtils.trustAllHostnameVerifier());
                }
                // add user credentials if a user is logged in
                String user = UserStore.getInstance().getUserId();
                if (user != null && !user.isEmpty()) {
                    urlConnection.setRequestProperty(AuthenticationService.USERID, user);
                }
                extras.setRequestHeaders(AppUtils.getHeaderItems(urlConnection.getRequestProperties()));
                urlConnection.connect();

                inputStream = urlConnection.getInputStream();
                metacardList = parseFeed(inputStream);

                extras.setStatusCode("" + urlConnection.getResponseCode());
                String contentSize = urlConnection.getHeaderField(CONTENT_LENGTH_HEADER);
                extras.setContentSize(contentSize == null ? "Unknown" : contentSize);

                extras.setResponseHeaders(AppUtils.getHeaderItems(urlConnection.getHeaderFields()));
            } catch (Exception e) {
                Log.w(MetacardQueryService.class.getSimpleName(), "Error while trying to query server.", e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        } else {
            //no network connectivity
            Log.w(MetacardQueryService.class.getSimpleName(), "Not connected to a network, cannot perform query.");
        }
        return metacardList;
    }

    private List<Metacard> parseFeed(InputStream is) {
        FeedParser feedParser = new FeedParser();
        List<Metacard> metacardList = new ArrayList<>();
        try {
            List<FeedParser.Entry> entryList = feedParser.parse(is);
            for (FeedParser.Entry curEntry : entryList) {
                //TODO pull in real content instead of putting title twice
                String description = getDescriptionText(curEntry.type, curEntry.title);
                Metacard newMetacard = new Metacard(curEntry.id, curEntry.title, description, curEntry.site, new Date(curEntry.published), curEntry.lat,
                        curEntry.lon, curEntry.thumbnail, curEntry.score, convertType(curEntry.type), new Metacard.Product(curEntry.product, curEntry.productSize, null));
                metacardList.add(newMetacard);
            }
        } catch (Exception e) {
            Log.w(MetacardQueryService.class.getSimpleName(), "Error while trying to parse feed.", e);
        }
        return metacardList;
    }

    //TODO figure out a better thing to put there
    private String getDescriptionText(String text, String title) {
        if (text != null){
            String[] textArray = text.split("(?=\\p{Upper})");
            String newValue = "";
            for( String s : textArray ){
                newValue += s + " ";
            }
            text = newValue.trim();
        }else{
            text = title;
        }
        return text;
    }

    private Metacard.ActionType convertType(String contentType) {
        Metacard.ActionType type;
        //TODO get actual conversions
        switch (contentType) {
            case "MobileImagery":
                type = Metacard.ActionType.IMAGE;
                break;
            case "MobileVideo":
                type = Metacard.ActionType.VIDEO;
                break;
            default:
                type = Metacard.ActionType.OTHER;
        }
        return type;
    }

    private String createQueryParameters(QueryType queryType, String criteriaStr) {
        JsonObject jsonCriteria = new JsonParser().parse(criteriaStr).getAsJsonObject();
        StringBuilder stringBuilder = new StringBuilder();
        boolean isFirstParam = true;
        stringBuilder.append("?");
        for (Map.Entry<String, JsonElement> entry : jsonCriteria.entrySet()) {
            if (!isFirstParam) {
                stringBuilder.append("&");
            } else {
                isFirstParam = false;
            }
            stringBuilder.append(entry.getKey() + "=" + entry.getValue().getAsString());
        }
        switch (queryType) {
            case LATEST:
                stringBuilder.append("&sortKeys=entry/date");
                break;
            case SPATIAL:
                stringBuilder.append("&sortKeys=distance&count=20");
                break;
            default:
                stringBuilder.append("q=*");
                break;
        }
        return stringBuilder.toString();
    }

    private class QueryResponseExtras {
        private String statusCode;
        private String contentSize;
        private ArrayList<HeaderItem> requestHeaders;
        private ArrayList<HeaderItem> responseHeaders;

        public String getContentSize() {
            return contentSize;
        }

        public void setContentSize(String contentSize) {
            this.contentSize = contentSize;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public ArrayList<HeaderItem> getRequestHeaders() {
            return requestHeaders;
        }

        public void setRequestHeaders(ArrayList<HeaderItem> requestHeaders) {
            this.requestHeaders = requestHeaders;
        }

        public ArrayList<HeaderItem> getResponseHeaders() {
            return responseHeaders;
        }

        public void setResponseHeaders(ArrayList<HeaderItem> responseHeaders) {
            this.responseHeaders = responseHeaders;
        }
    }


}
