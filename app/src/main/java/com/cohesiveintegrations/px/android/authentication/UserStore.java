package com.cohesiveintegrations.px.android.authentication;

import java.util.HashMap;
import java.util.Map;

public enum UserStore {

    INSTANCE;

    private static final String IDENTIFIER = "digitalidentifier";
    private static final String CLEARANCE = "clearance";
    private static final String CITIZENSHIP = "countryofaffiliation";

    // details provided by id token:
    // sub, digitalidentifier, clearance, countryofaffiliation
    private static final Map<String, String> userCredentials = new HashMap<>();

    public static UserStore getInstance() {
        return INSTANCE;
    }

    public Map<String, String> getUserCredentials() {
        return userCredentials;
    }

    public String getUserId(){
        return userCredentials.get(IDENTIFIER);
    }



}
