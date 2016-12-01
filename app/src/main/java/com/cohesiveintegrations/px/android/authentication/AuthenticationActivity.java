package com.cohesiveintegrations.px.android.authentication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AuthenticationActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = createWebView();
        setContentView(webView);


    }

    private WebView createWebView() {
        WebView webView = new WebView(this);
        webView.clearSslPreferences();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(AuthenticationConstants.PING_AUTHZ_ENDPOINT + AuthenticationConstants.PING_AUTHZ_SUFFIX);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //TODO add truststore instead of just bypassing SSL errors
                handler.proceed();
            }

            @Override
            public void onPageStarted(WebView view, String urlString, Bitmap favicon) {
                super.onPageStarted(view, urlString, favicon);
                Uri uri = Uri.parse(urlString);
                if (uri.getScheme().equals("cohesive-px-android")) {
                    view.stopLoading();
                    String authZToken = uri.getQueryParameter("code");
                    Intent intent = new Intent();
                    intent.putExtra("token", authZToken);
                    setResult(RESULT_OK, intent);
                    finish();
                }

            }

        });

        return webView;
    }

}
