package com.cohesiveintegrations.px.android.status.server;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import ws.argo.ProbeGenerator.Probe;
import ws.argo.ProbeGenerator.ProbeGenerator;

public class ServerProbeService extends IntentService {

    public static final String IP_ADDRESS = "IP_ADDRESS";

    public ServerProbeService() {
        super(ServerProbeService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String hostAddress = intent.getStringExtra(IP_ADDRESS);
            ProbeGenerator probeGenerator = new ProbeGenerator("230.0.0.1", 4003);
            Probe probe = new Probe("http://" + hostAddress + ":8080", Probe.JSON);
            probe.addServiceContractID("urn:uuid:b10b8c34-76f4-4e1d-938d-20eee3377d22");
            probe.addServiceContractID("urn:uuid:d1034cef-76f4-4e1d-938d-20eee3377d22");
            probeGenerator.sendProbe(probe);
        } catch (Exception e) {
            Log.e(ServerProbeService.class.getName(), "Error while sending probe.", e);
        }
    }

}
