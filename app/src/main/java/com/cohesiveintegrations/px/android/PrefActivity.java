package com.cohesiveintegrations.px.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cohesiveintegrations.px.android.data.metacard.Metacard;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PrefActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preference licenses = findPreference("licenses");
        final Activity thisActivity = this;
        licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                WebView webView = new WebView(thisActivity);
                webView.loadUrl("file:///android_asset/licenses.html");
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });
                AlertDialog licenseDialog = new AlertDialog.Builder(thisActivity, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                        .setTitle("Software Licenses")
                        .setView(webView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                licenseDialog.show();
                return true;
            }
        });

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_mock_key))) {
            mockServerInfo(sharedPreferences.getBoolean(key, false));
            mockFeedInfo(sharedPreferences.getBoolean(key, false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void mockServerInfo(boolean shouldMockServers) {
        Map<String, Server> serverMap = ServerCollection.getInstance().getServerMap();
        if (shouldMockServers) {
            if (serverMap.containsKey("DDF_ONE") && serverMap.containsKey("DDF_TWO") && serverMap.containsKey("DDF_THREE") && serverMap.containsKey("DDF_FOUR")) {
                //mock data is already added
                return;
            } else {
                serverMap.put("DDF_ONE", new Server("DDF_ONE", "http", "127.0.0.1", 8181, true, true, true));
                serverMap.put("DDF_TWO", new Server("DDF_TWO", "https", "192.168.1.1", 80, true, true, false));
                serverMap.put("DDF_THREE", new Server("DDF_THREE", "https", "192.168.1.2", 8080, false, false, false, "Not Connected"));
                serverMap.put("DDF_FOUR", new Server("DDF_FOUR", "http", "192.168.1.3", 8181, true, false, false));
            }
        } else {
            serverMap.remove("DDF_ONE");
            serverMap.remove("DDF_TWO");
            serverMap.remove("DDF_THREE");
            serverMap.remove("DDF_FOUR");
        }
    }

    private void mockFeedInfo(boolean shouldMockFeed) {
        //TODO fix this to actually populate data!
        Map<String, Metacard> metacardMap = new HashMap<>();
        if (shouldMockFeed) {
            if (metacardMap.containsKey("101") && metacardMap.containsKey("102") && metacardMap.containsKey("103") && metacardMap.containsKey("104")) {
                //mock data is already added
                return;
            } else {
                Date now = new Date();
                long dayInMillis = 86400000;
                long weekInMillis = dayInMillis * 7;
                long yearInMillis = weekInMillis * 52;
                // 1 day prior
                metacardMap.put("102", new Metacard("102", "Metacard 2", "This is the second metacard", "DDF_ONE", new Date(now.getTime() - dayInMillis), "", "", String.valueOf(R.drawable.f22_thumbnail), 3.0, Metacard.ActionType.VIDEO, null));
                // 1 week prior
                metacardMap.put("103", new Metacard("103", "Metacard 3", "This is the third metacard", "DDF_TWO", new Date(now.getTime() - (weekInMillis)), "", "", String.valueOf(R.drawable.navy_thumbnail), 6.0, Metacard.ActionType.IMAGE, null));
                // 1 year prior
                metacardMap.put("104", new Metacard("104", "Metacard 4", "This is the fourth metacard", "DDF_FOUR", new Date(now.getTime() - (yearInMillis)), "", "", String.valueOf(R.drawable.army_thumbnail), 4.0, Metacard.ActionType.OTHER, null));
                // now
                metacardMap.put("101", new Metacard("101", "Metacard 1", "This is the first metacard", "DDF_ONE", now, "", "", String.valueOf(R.drawable.marines_thumbnail), 5.0, Metacard.ActionType.IMAGE, null));
            }
        } else {
            metacardMap.remove("101");
            metacardMap.remove("102");
            metacardMap.remove("103");
            metacardMap.remove("104");
        }
    }

        /*
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }


    public static class PrefFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
    */
}
