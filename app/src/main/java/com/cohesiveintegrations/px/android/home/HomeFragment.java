package com.cohesiveintegrations.px.android.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.authentication.AuthenticationService;
import com.cohesiveintegrations.px.android.authentication.UserStore;

import java.util.Map;

public class HomeFragment extends Fragment {

    private UserStatusReceiver statusReceiver;

    private TextView loginText;

    private ImageButton signInButton;

    private ImageButton signOutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        loginText = (TextView) rootView.findViewById(R.id.loginText);
        signInButton = (ImageButton) rootView.findViewById(R.id.signInButton);
        signOutButton = (ImageButton) rootView.findViewById(R.id.signOutButton);
        statusReceiver = new UserStatusReceiver();
        IntentFilter filter = new IntentFilter(AuthenticationService.BROADCAST_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getActivity().registerReceiver(statusReceiver, filter);
        refreshLoginText();
        return rootView;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(statusReceiver);
        super.onDestroy();
    }

    private void refreshLoginText() {
        String user = "Anonymous";
        Map<String, String> userCredentials = UserStore.getInstance().getUserCredentials();
        if (userCredentials != null && !userCredentials.isEmpty()) {
            user = userCredentials.get("user");
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);

        } else {
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
        }
        loginText.setText("Logged in as " + user);
    }

    public class UserStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshLoginText();
        }
    }

}
