package com.cohesiveintegrations.px.android.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.provider.Settings;
import android.util.Log;

import com.cohesiveintegrations.px.android.audit.HeaderItem;
import com.cohesiveintegrations.px.android.data.server.Server;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static java.util.Collections.*;

/**
 * Created by jvettraino on 5/15/2015.
 */
public class AppUtils {

    private static SimpleDateFormat auditFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static SimpleDateFormat dateOnlyFormatter = new SimpleDateFormat("yyyy-MM-dd");
    static{
        auditFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateOnlyFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String getFileAssetAsString(AssetManager manager, String fileName) {
        StringBuilder out = new StringBuilder();
        InputStream inputStream = null;
        try {
            inputStream = manager.open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }catch(Exception e){
            Log.e("CondorQueryLog", "Could not read file template" );
        }finally{
            if ( inputStream != null ){ try{ inputStream.close(); }catch(IOException e ){} }
        }
        return out.toString();
    }

    public static String formatAuditDate( Date date ){
        return auditFormatter.format(date);
    }

    public static String formatDate( Date date ){
        return dateOnlyFormatter.format(date);
    }

    public static String getUrl( Server server, String urlPath ) {
        return server.getProtocol() + "://" + server.getAddress() + ":" + server.getPort() + urlPath;
    }

    public static String getDeviceId( Context context ) {
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return id == null ? "Unknown" : id;
    }

    public static ArrayList<HeaderItem> getHeaderItems(Map<String, List<String>> headers) {
        ArrayList<HeaderItem> headerList = new ArrayList<HeaderItem>();
        if ( headers != null && !headers.isEmpty() ){
            for( Map.Entry<String,List<String>> entry : headers.entrySet() ) {
                String headerName = entry.getKey();
                if ( entry.getKey() != null && !headerName.isEmpty() ) {
                    headerList.add(new HeaderItem(headerName, entry.getValue().get(0)));
                }
            }
        }
        return headerList;
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public static String getFirstIPAddress(){
        String ip = getIPAddress(true);
        return ip == null || ip.isEmpty() ? getIPAddress(false) : ip;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public static String replaceEach(
            String text, String search, String replacement ) {
        return replaceEach(text, new String[]{search}, new String[]{replacement});
    }

    //MEthod copied from Apache Commons StringUtils
    public static String replaceEach(
            String text, String[] searchList, String[] replacementList ) {

        if (text == null || text.length() == 0 || searchList == null ||
                searchList.length == 0 || replacementList == null || replacementList.length == 0) {
            return text;
        }

        int searchLength = searchList.length;
        int replacementLength = replacementList.length;

        // make sure lengths are ok, these need to be equal
        if (searchLength != replacementLength) {
            throw new IllegalArgumentException("Search and Replace array lengths don't match: "
                    + searchLength
                    + " vs "
                    + replacementLength);
        }

        // keep track of which still have matches
        boolean[] noMoreMatchesForReplIndex = new boolean[searchLength];

        // index on index that the match was found
        int textIndex = -1;
        int replaceIndex = -1;
        int tempIndex = -1;

        // index of replace array that will replace the search string found
        // NOTE: logic duplicated below START
        for (int i = 0; i < searchLength; i++) {
            if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                    searchList[i].length() == 0 || replacementList[i] == null) {
                continue;
            }
            tempIndex = text.indexOf(searchList[i]);

            // see if we need to keep searching for this
            if (tempIndex == -1) {
                noMoreMatchesForReplIndex[i] = true;
            } else {
                if (textIndex == -1 || tempIndex < textIndex) {
                    textIndex = tempIndex;
                    replaceIndex = i;
                }
            }
        }
        // NOTE: logic mostly below END

        // no search strings found, we are done
        if (textIndex == -1) {
            return text;
        }

        int start = 0;

        // get a good guess on the size of the result buffer so it doesn't have to double if it goes over a bit
        int increase = 0;

        // count the replacement text elements that are larger than their corresponding text being replaced
        for (int i = 0; i < searchList.length; i++) {
            if (searchList[i] == null || replacementList[i] == null) {
                continue;
            }
            int greater = replacementList[i].length() - searchList[i].length();
            if (greater > 0) {
                increase += 3 * greater; // assume 3 matches
            }
        }
        // have upper-bound at 20% increase, then let Java take over
        increase = Math.min(increase, text.length() / 5);

        StringBuilder buf = new StringBuilder(text.length() + increase);

        while (textIndex != -1) {

            for (int i = start; i < textIndex; i++) {
                buf.append(text.charAt(i));
            }
            buf.append(replacementList[replaceIndex]);

            start = textIndex + searchList[replaceIndex].length();

            textIndex = -1;
            replaceIndex = -1;
            tempIndex = -1;
            // find the next earliest match
            // NOTE: logic mostly duplicated above START
            for (int i = 0; i < searchLength; i++) {
                if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                        searchList[i].length() == 0 || replacementList[i] == null) {
                    continue;
                }
                tempIndex = text.indexOf(searchList[i], start);

                // see if we need to keep searching for this
                if (tempIndex == -1) {
                    noMoreMatchesForReplIndex[i] = true;
                } else {
                    if (textIndex == -1 || tempIndex < textIndex) {
                        textIndex = tempIndex;
                        replaceIndex = i;
                    }
                }
            }
            // NOTE: logic duplicated above END

        }
        int textLength = text.length();
        for (int i = start; i < textLength; i++) {
            buf.append(text.charAt(i));
        }
        String result = buf.toString();

        return result;
    }


}
