package com.cohesiveintegrations.px.android.authentication;

public final class AuthenticationConstants {

    public static final String PING_REDIRECT_URL = "cohesive-px-android://auth_callback";

    public static final String PING_CLIENT_ID = "cointegrate_id";

    public static final String PING_SECRET = "REDACTED";

    public static final String PING_AUTHZ_ENDPOINT = "https://ping/as/authorization.oauth2";

    public static final String PING_AUTHZ_SUFFIX = "?response_type=code&scope=openid%20profile&client_id=cointegrate_id&redirect_uri=cohesive-px-android%3A%2F%2Fauth_callback";

    public static final String PING_TOKEN_ENDPOINT = "https://ping/as/token.oauth2";

    private AuthenticationConstants() {

    }

}
