package com.checkapp.panel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView web;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);          // the panel is a JS app
        s.setDomStorageEnabled(true);          // localStorage (login fallback + saved apps)
        s.setDatabaseEnabled(true);

        // --- Hardening ---------------------------------------------------------
        // The panel only loads its own bundled asset and talks to api.github.com /
        // the Cloudflare Worker over HTTPS via fetch(). Everything else is locked
        // down so a stolen device or injected content cannot read files or exfiltrate.
        // Note: assets under file:///android_asset still load with file access off.
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Secrets (GitHub token / admin key) live in app-private storage reachable
        // only through this bridge — not in WebView web-storage and not in backups.
        web.addJavascriptInterface(new SecureStore(this), "SecureStore");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                return external(req.getUrl() == null ? null : req.getUrl().toString());
            }
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return external(url);
            }
        });
        web.setWebChromeClient(new WebChromeClient());

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    // Keep the token-bearing WebView on our bundled page; hand any other URL to a
    // real browser instead of navigating the panel there.
    private boolean external(String url) {
        if (url == null) return false;
        if (url.startsWith("file:///android_asset/")) return false; // load inside the WebView
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
        return true; // handled — do not load a remote page in the panel
    }

    // Minimal key/value store backed by app-private SharedPreferences. Every method
    // is defensive so the panel never breaks if storage misbehaves.
    private static class SecureStore {
        private final SharedPreferences sp;
        SecureStore(Context c) { sp = c.getSharedPreferences("secure_store", Context.MODE_PRIVATE); }
        @JavascriptInterface public String get(String k) { try { return sp.getString(k, null); } catch (Exception e) { return null; } }
        @JavascriptInterface public void set(String k, String v) { try { sp.edit().putString(k, v).apply(); } catch (Exception e) {} }
        @JavascriptInterface public void remove(String k) { try { sp.edit().remove(k).apply(); } catch (Exception e) {} }
        @JavascriptInterface public boolean available() { return true; }
    }

    // Android back button navigates WebView history before leaving the app.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
