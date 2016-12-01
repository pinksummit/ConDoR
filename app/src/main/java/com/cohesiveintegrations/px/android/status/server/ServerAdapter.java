package com.cohesiveintegrations.px.android.status.server;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.cohesiveintegrations.px.android.status.StatusDetailActivity;

import static android.support.v4.app.ActivityCompat.startActivity;

public class ServerAdapter extends RecyclerView.Adapter<ServerHolder> {

    private final FragmentActivity activity;

    private ServerCollection serverCollection = ServerCollection.getInstance();

    public ServerAdapter(FragmentActivity activity) {
        this.activity = activity;
    }

    @Override
    public ServerHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.fragment_status_server, viewGroup, false);
        return new ServerHolder(view, new ServerHolder.ServerClickListener() {

            @Override
            public void showDetails(String serverName, View view) {
                Intent intent = new Intent(activity, StatusDetailActivity.class);
                intent.putExtra(Server.SERVER_ID, serverName);
                startActivity(activity, intent, null);
            }
        }, new ServerHolder.ServerLongClickListener() {

            @Override
            public void showRemoveDialog(final String serverName, View view) {
                final AlertDialog removeDialog = new AlertDialog.Builder(view.getContext())
                        .setTitle("Remove Server")
                        .setMessage("This will remove the server from this page and prevent items showing up in the feed.")
                        .setPositiveButton("REMOVE", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                removeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        serverCollection.getServerMap().remove(serverName);
                        removeDialog.dismiss();
                        ServerAdapter.this.notifyDataSetChanged();
                    }
                });

                removeDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        removeDialog.dismiss();
                    }
                });
            }
        },
                viewGroup.getContext());
    }

    @Override
    public void onBindViewHolder(ServerHolder serverHolder, int i) {
        Server currentServer = serverCollection.getServerList().get(i);
        serverHolder.setSeverInfo(currentServer);
    }

    @Override
    public int getItemCount() {
        return serverCollection.getServerMap().size();
    }
}
