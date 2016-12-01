package com.cohesiveintegrations.px.android.status.server;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.data.server.Server;

public class ServerHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    private TextView serverName;
    private TextView ipAddress;
    private TextView port;
    private TextView serverStatus;
    private ImageButton serverStatusIcon;
    private ImageButton serverUploadIcon;

    private Context context;
    private ServerClickListener clickListener;
    private ServerLongClickListener longClickListener;

    public ServerHolder(View itemView, ServerClickListener clickListener, ServerLongClickListener longClickListener, Context context) {
        super(itemView);
        serverName = (TextView) itemView.findViewById(R.id.statusName);
        ipAddress = (TextView) itemView.findViewById(R.id.statusAddress);
        port = (TextView) itemView.findViewById(R.id.statusPort);
        serverStatus = (TextView) itemView.findViewById(R.id.statusText);
        serverStatusIcon = (ImageButton) itemView.findViewById(R.id.statusIcon);
        serverUploadIcon = (ImageButton) itemView.findViewById(R.id.statusUploadIcon);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.context = context;

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void setSeverInfo(Server currentServer) {
        serverName.setText(currentServer.getName());
        ipAddress.setText(currentServer.getAddress());
        port.setText(Integer.toString(currentServer.getPort()));
        int background;
        int image;
        int uploadBackground;

        if (currentServer.isAvailable()) {
            if (currentServer.isEnabled()) {
                background = R.drawable.status_notification_good;
                image = R.drawable.ic_action_accept;
                serverStatus.setText(Server.STATUS_CONNECTED);
                if ( currentServer.isIngest() ){
                    uploadBackground = R.drawable.status_notification_good;
                }else{
                    uploadBackground = R.drawable.status_notification_error;
                }
            } else {
                background = R.drawable.status_notification_warn;
                image = R.drawable.ic_action_warning;
                serverStatus.setText(Server.STATUS_DISABLED);
                uploadBackground = R.drawable.status_notification_error;
            }
        } else {
            background = R.drawable.status_notification_error;
            uploadBackground = R.drawable.status_notification_error;
            image = R.drawable.ic_action_error;
            serverStatus.setText(currentServer.getMessage());

        }
        serverStatusIcon.setBackgroundResource(background);
        serverStatusIcon.setImageDrawable(context.getResources().getDrawable(image));
        serverUploadIcon.setBackgroundResource(uploadBackground);
    }


    @Override
    public void onClick(View view) {
        clickListener.showDetails(serverName.getText().toString(), view);
    }

    @Override
    public boolean onLongClick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        longClickListener.showRemoveDialog(serverName.getText().toString(), view);
        return false;
    }

    public static interface ServerClickListener {
        public void showDetails(String serverName, View view);
    }

    public static interface ServerLongClickListener {
        public void showRemoveDialog(String serverName, View view);
    }
}
