package com.cohesiveintegrations.px.android.data.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ServerCollection {

    INSTANCE;

    private static final Map<String, Server> serverMap = new HashMap<>();

    public static ServerCollection getInstance() {
        return INSTANCE;
    }

    public Map<String, Server> getServerMap() {
        return serverMap;
    }

    public Server getServer(String name) {
        return serverMap.get(name);
    }

    public List<Server> getServerList() {
        return new ArrayList<>(serverMap.values());
    }

}
