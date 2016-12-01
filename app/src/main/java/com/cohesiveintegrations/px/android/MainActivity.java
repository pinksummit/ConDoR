package com.cohesiveintegrations.px.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.cohesiveintegrations.px.android.authentication.AuthenticationActivity;
import com.cohesiveintegrations.px.android.authentication.AuthenticationService;
import com.cohesiveintegrations.px.android.authentication.UserStore;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.cohesiveintegrations.px.android.feed.FeedFragment;
import com.cohesiveintegrations.px.android.home.HomeFragment;
import com.cohesiveintegrations.px.android.status.StatusFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static final String SERVER_PREFERENCE_KEY = "servers";
    public static final String USER_PREFERENCE_KEY = "user";

    private static final int AUTHENTICATION_RESULT = 7171;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        populateServerData();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment containerFragment;
        switch (position) {
            case 0:
                containerFragment = new HomeFragment();
                mTitle = getString(R.string.title_home);
                break;
            case 1:
                containerFragment = new FeedFragment();
                mTitle = getString(R.string.title_search);
                break;
            case 2:
                containerFragment = new StatusFragment();
                mTitle = getString(R.string.title_status);
                break;
            default:
                //use home
                containerFragment = new HomeFragment();
                mTitle = getString(R.string.title_home);
                break;
        }
        fragmentManager.beginTransaction().replace(R.id.container, containerFragment).commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, PrefActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        persistServerData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTHENTICATION_RESULT && resultCode == RESULT_OK && data != null) {
            String token = data.getStringExtra("token");
            Intent intent = new Intent(Intent.ACTION_SYNC, null, this, AuthenticationService.class);
            intent.putExtra(AuthenticationService.AUTH_CODE, token);
            startService(intent);
        }
    }

    public void performLogin(View view) {
        Intent intent = new Intent(this, AuthenticationActivity.class);
        startActivityForResult(intent, AUTHENTICATION_RESULT);
    }

    public void performLogout(View view) {
        UserStore.getInstance().getUserCredentials().clear();
        Intent intent = new Intent();
        intent.setAction(AuthenticationService.BROADCAST_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(intent);
    }

    // saves the servers and user credentials to sharedpreferences
    private void persistServerData() {
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // servers
        String jsonServerMap = gson.toJson(ServerCollection.getInstance().getServerMap());
        editor.putString(SERVER_PREFERENCE_KEY, jsonServerMap);
        // user
        String jsonUserMap = gson.toJson(UserStore.getInstance().getUserCredentials());
        editor.putString(USER_PREFERENCE_KEY, jsonUserMap);
        editor.apply();
    }

    // retrieves the servers and user credentials from sharedpreferences
    private void populateServerData() {
        Map<String, Server> currentMap = ServerCollection.getInstance().getServerMap();
        Map<String, String> userCredentials = UserStore.getInstance().getUserCredentials();
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        // servers
        String jsonServerMap = sharedPreferences.getString(SERVER_PREFERENCE_KEY, null);
        if (jsonServerMap != null) {
            Type serverType = new TypeToken<HashMap<String, Server>>() {
            }.getType();
            Map<String, Server> storedServerMap = gson.fromJson(jsonServerMap, serverType);
            currentMap.clear();
            currentMap.putAll(storedServerMap);
        }
        // user
        String jsonUserMap = sharedPreferences.getString(USER_PREFERENCE_KEY, null);
        if (jsonUserMap != null) {
            Type userType = new TypeToken<HashMap<String, String>>() {
            }.getType();
            Map<String, String> storedUserCredentials = gson.fromJson(jsonUserMap, userType);
            userCredentials.clear();
            userCredentials.putAll(storedUserCredentials);
        }
    }


}
