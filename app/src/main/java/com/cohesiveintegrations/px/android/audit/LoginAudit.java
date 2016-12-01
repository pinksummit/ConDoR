package com.cohesiveintegrations.px.android.audit;

import android.os.Parcelable;
import android.util.Log;

import com.cohesiveintegrations.px.android.BuildConfig;
import com.cohesiveintegrations.px.android.data.server.Server;
import com.cohesiveintegrations.px.android.util.AppUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by jvettraino on 5/14/2015.
 */
public class LoginAudit extends AbstractAuditor{

    private static final String AUDIT_TEMPLATE = "CondorAuditLogin.xml";

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
            "$LOGIN_RESULT$"
    };

    public LoginAudit(){
        super(LoginAudit.class.getName());
    }

    @Override
    protected String getAuditMessage( Server server, String urlPath, String[] extras, List<Parcelable> headers ) {
        String auditMessage = AppUtils.getFileAssetAsString(getApplicationContext().getAssets(), AUDIT_TEMPLATE);
        auditMessage = AppUtils.replaceEach( auditMessage, replacementList, getReplacementValues( server, urlPath, extras ) );
        return auditMessage;
    }

    @Override
    public String getAuditLogType(){
        return AbstractAuditor.AUDIT_CLIENT_LOGIN;
    }

    public String[] getReplacementValues( Server server, String urlPath, String[] extras){
        String[] values = new String[11];
        values[0] = UUID.randomUUID().toString();
        values[1] = android.os.Build.MODEL;
        values[2] = UUID.randomUUID().toString();
        values[3] = AppUtils.formatAuditDate(new Date());
        values[4] = formatUserForAudit();
        values[5] = "Unknown";
        values[6] = AppUtils.getUrl(server, urlPath);
        values[7] = server.getAddress();
        values[8] = AppUtils.getFirstIPAddress();
        values[9] = AppUtils.getDeviceId(getApplicationContext());
        values[10] = extras[0];
        if (BuildConfig.DEBUG ){
            Log.d("LoginAudit", "Returning values");
        }
        return values;
    }
}
