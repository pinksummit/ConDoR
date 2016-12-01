package com.cohesiveintegrations.px.android.status;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class StatusDetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String serverId = intent.getStringExtra(Server.SERVER_ID);
        final Server server = ServerCollection.getInstance().getServer(serverId);
        setContentView(R.layout.activity_status_detail);
        TextView serverName = (TextView)findViewById(R.id.serverName);
        TextView serverAddress = (TextView)findViewById(R.id.serverAddress);
        Switch serverSwitch = (Switch)findViewById(R.id.serverSwitch);
        LinearLayout servicesLayout = (LinearLayout)findViewById(R.id.servicesLayout);

        serverName.setText(server.getName());
        serverAddress.setText(server.getAddress());
        serverSwitch.setChecked(server.isEnabled());
        serverSwitch.setClickable(server.isAvailable());
        serverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                server.setEnabled(isChecked);
            }
        });
    }
}
