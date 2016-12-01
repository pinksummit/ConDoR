package com.cohesiveintegrations.px.android.data.server;

public class Server implements Comparable<Server> {

    public static final String SERVER_ID = "serverId";
    public static final String STATUS_CONNECTED = "Connected";
    public static final String STATUS_CONNECTING = "Connecting...";
    public static final String STATUS_DISABLED = "Disabled";

    private String name;
    private String protocol;
    private String address;
    private int port;
    private boolean isAvailable;
    private boolean isEnabled;
    private boolean isIngest;
    private String message;

    public Server(String serverName, String protocol, String serverAddress, int serverPort, boolean isAvailable, boolean isEnabled, boolean isIngest) {
        this(serverName, protocol, serverAddress, serverPort, isAvailable, isEnabled, isIngest, "");
    }

    public Server(String serverName, String protocol, String serverAddress, int serverPort, boolean isAvailable, boolean isEnabled, boolean isIngest, String message) {
        name = serverName;
        this.protocol = protocol;
        address = serverAddress;
        port = serverPort;
        this.isAvailable = isAvailable;
        this.isEnabled = isEnabled;
        this.isIngest = isIngest;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAddress() {
        return address;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isIngest() { return isIngest; }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setIngest( boolean isIngest ) { this.isIngest = isIngest; }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int compareTo(Server another) {
        return name.compareTo(another.getName());
    }

}
