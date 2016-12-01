package com.cohesiveintegrations.px.android.status;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.cohesiveintegrations.px.android.R;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.data.server.ServerCollection;
import com.cohesiveintegrations.px.android.status.server.ResponseListenerServer;
import com.cohesiveintegrations.px.android.status.server.ServerAdapter;
import com.cohesiveintegrations.px.android.status.server.ServerProbeService;
import com.cohesiveintegrations.px.android.status.server.ServerStatusService;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Fragment that shows the status of the active servers.
 */
public class StatusFragment extends Fragment {

    private ServerAdapter serverAdapter;
    private ServerCollection serverCollection = ServerCollection.getInstance();
    private LayoutInflater inflater;
    private ViewGroup container;
    private ServerStatusReceiver statusReceiver;
    private SwipeRefreshLayout refreshLayout;
    private ResponseListenerServer listenerServer = null;
    private String localIpAddress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        this.container = container;
        final View rootView = inflater.inflate(R.layout.fragment_status, container, false);
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.statusList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        serverAdapter = new ServerAdapter(getActivity());
        recyclerView.setAdapter(serverAdapter);
        configureManualAddButton(rootView);
        configureDynamicAddButton(rootView);
        setSwipeRefresh(rootView);
        IntentFilter filter = new IntentFilter(ServerStatusService.BROADCAST_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        statusReceiver = new ServerStatusReceiver();
        getActivity().registerReceiver(statusReceiver, filter);

        localIpAddress = getLocalIp();
        //check servers when page loads
        //refreshServers(rootView.getContext());
        return rootView;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(statusReceiver);
        if (listenerServer != null && listenerServer.isAlive()) {
            listenerServer.stop();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        serverAdapter.notifyDataSetChanged();
    }

    private void setSwipeRefresh(final View rootView) {
        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.statusSwipeRefreshLayout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshServers(rootView.getContext());

            }
        });
    }

    private void refreshServers(Context context) {
        refreshLayout.setRefreshing(true);
        Intent intent = new Intent(Intent.ACTION_SYNC, null, context, ServerStatusService.class);
        getActivity().startService(intent);
    }

    // manual add button
    private void configureManualAddButton(final View rootView) {
        final FloatingActionButton addServerButton = (FloatingActionButton) rootView.findViewById(R.id.statusManualAdd);
        final FloatingActionsMenu addMenu = (FloatingActionsMenu) rootView.findViewById(R.id.statusAddMenu);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set up the input
                final Context context = rootView.getContext();
                final View inputLayout = inflater.inflate(R.layout.view_status_dialog, container, false);
                final EditText nameEditText = (EditText) inputLayout.findViewById(R.id.statusDialogName);
                final EditText addressEditText = (EditText) inputLayout.findViewById(R.id.statusDialogAddress);
                final EditText portEditText = (EditText) inputLayout.findViewById(R.id.statusDialogPort);
                final CheckBox checkBox = (CheckBox) inputLayout.findViewById(R.id.statusDialogIngestServer);
                final Spinner protocolSpinner = (Spinner) inputLayout.findViewById(R.id.statusDialogProtocol);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_text, new String[]{"http", "https"});
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                protocolSpinner.setAdapter(adapter);

                final AlertDialog dialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setTitle("Add Server")
                        .setView(inputLayout)
                        .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        //validate ip format
                        String ipAddress = addressEditText.getText().toString();
                        String name = nameEditText.getText().toString();
                        String port = portEditText.getText().toString();
                        boolean ingest = checkBox.isChecked();
                        String protocol = protocolSpinner.getSelectedItem().toString();
                        if (port.equals("") || port.isEmpty()) {
                            port = "8181";
                        }
                        boolean isValidName = isValidName(name);
                        if (isValidName) {
                            Server server = new Server(name, protocol, ipAddress, Integer.parseInt(port), false, false, ingest);
                            server.setMessage(Server.STATUS_CONNECTING);
                            serverCollection.getServerMap().put(name, server);
                            // check status for specific server
                            Intent intent = new Intent(Intent.ACTION_SYNC, null, context, ServerStatusService.class);
                            intent.putExtra(Server.SERVER_ID, name);
                            getActivity().startService(intent);
                            dialog.dismiss();
                            addMenu.collapse();
                        } else {
                            nameEditText.setError("Name cannot be blank.");
                        }
                    }
                });

                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        addMenu.collapse();
                    }

                });
            }
        };
        addServerButton.setOnClickListener(listener);
    }

    // dynamic add button
    private void configureDynamicAddButton(final View rootView) {
        final FloatingActionButton addServerButton = (FloatingActionButton) rootView.findViewById(R.id.statusDetectAdd);
        final FloatingActionsMenu addMenu = (FloatingActionsMenu) rootView.findViewById(R.id.statusAddMenu);
        final Context context = rootView.getContext();

        View.OnClickListener listener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {

                    // start response listener
                    if (listenerServer == null) {
                        listenerServer = new ResponseListenerServer(localIpAddress, 8080, getActivity());
                    }
                    if (!listenerServer.isAlive()) {
                        listenerServer.start();
                    }

                    // send probe
                    Intent intent = new Intent(Intent.ACTION_SYNC, null, context, ServerProbeService.class);
                    intent.putExtra(ServerProbeService.IP_ADDRESS, localIpAddress);
                    getActivity().startService(intent);
                    Toast.makeText(getActivity(), "Probe Sent.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "Could not send out probe due to error.", Toast.LENGTH_LONG).show();
                    Log.e(StatusFragment.class.getName(), "Error while sending probe.", e);
                }
                addMenu.collapse();
            }
        };

        addServerButton.setOnClickListener(listener);
    }

    private static boolean isValidName(String name) {
        //validate name
        return (name != null && !name.isEmpty());
    }

    private String getLocalIp() {
        String ipAddress = null;
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaceEnumeration.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
                if (!networkInterface.isLoopback() && networkInterface.isUp() && networkInterface.supportsMulticast()) {
                    Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
                    while (inetAddressEnumeration.hasMoreElements()) {
                        InetAddress inetAddress = inetAddressEnumeration.nextElement();
                        if (inetAddress instanceof Inet4Address) {
                            ipAddress = inetAddress.getHostAddress();
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(StatusFragment.class.getName(), "Could not get local IP address.", e);
        }
        return ipAddress;
    }

    public class ServerStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            serverAdapter.notifyDataSetChanged();
            refreshLayout.setRefreshing(false);
        }
    }

}
