package com.yadakprice.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

/**
 * پوشش بومی اندروید برای یدک‌پرایس.
 *
 * عمداً به‌جای WebView.loadUrl("file:///android_asset/...") از WebViewAssetLoader
 * استفاده شده: بارگذاری مستقیم از file:// باعث می‌شود fetch()، IndexedDB و Service
 * Worker در برنامه (app.js) به دلیل محدودیت‌های امنیتی مرورگر روی file:// درست کار
 * نکنند. WebViewAssetLoader فایل‌های assets را روی یک دامنه مجازی https (بدون نیاز
 * به اینترنت واقعی یا سرور) سرو می‌کند تا همان رفتار یک وب‌سایت واقعی را داشته باشد.
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String VIRTUAL_DOMAIN = "appassets.androidplatform.net";
    private static final int MIC_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(VIRTUAL_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        // اجازه دسترسی میکروفون به صفحه وب برای جست‌وجوی صوتی (Web Speech API)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(@NonNull PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
        }

        webView.loadUrl("https://" + VIRTUAL_DOMAIN + "/assets/www/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST
                && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "بدون دسترسی میکروفون، جست‌وجوی صوتی کار نمی‌کند.", Toast.LENGTH_SHORT).show();
        }
    }
}
