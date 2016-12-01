package com.cohesiveintegrations.px.android.audit;

import android.os.Parcelable;

import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.util.AppUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by jvettraino on 5/14/2015.
 */
public class IngestResponseAudit extends AbstractAuditor{

    private static final String AUDIT_TEMPLATE = "CondorAuditIngest-Response.xml";

    private static String[] replacementList = new String[]{
            "$UUID$",
            "$HOST_NAME$",
            "$EMID$",
            "$DATE_TIME$",
            "$USER_NAME$",
            "$USER_ORG$",
            "$URL$",
            "$SERVICE_IP$",
            "$DEVICE_IP$",
            "$DEVICE_ID$",
            "$HEADERS$",
            "$CONTENT_URL$"
    };

    public IngestResponseAudit(){
        super(IngestResponseAudit.class.getName());
    }

    @Override
    protected String getAuditMessage( Server server, String urlPath, String[] extras, List<Parcelable> headers  ) {
        String auditMessage = AppUtils.getFileAssetAsString(getApplicationContext().getAssets(), AUDIT_TEMPLATE);
        auditMessage = AppUtils.replaceEach( auditMessage, replacementList, getReplacementValues( server, urlPath, extras, headers ) );
        return auditMessage;
    }

    @Override
    public String getAuditLogType(){
        return AbstractAuditor.AUDIT_CLIENT_INGEST_RESPONSE;
    }

    public String[] getReplacementValues( Server server, String urlPath, String[] extras, List<Parcelable> headers ){
        String[] values = new String[12];
        values[0] = UUID.randomUUID().toString();
        values[1] = android.os.Build.MODEL;
        values[2] = UUID.randomUUID().toString();
        values[3] = AppUtils.formatAuditDate(new Date());
        values[4] = formatUserForAudit();
        values[5] = "Unknown";
        values[6] = AppUtils.getUrl(server, urlPath);
        values[7] = server.getAddress();
        values[8] = AppUtils.getFirstIPAddress();
        values[9] = AppUtils.getDeviceId(getApplicationContext() );
        values[10] = getHeaderXML(headers );
        values[11] = extras[0];
        return values;
    }

}
