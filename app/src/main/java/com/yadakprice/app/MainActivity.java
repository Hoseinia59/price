package com.yadakprice.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
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

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    private static final String VIRTUAL_DOMAIN =
            "appassets.androidplatform.net";

    private static final int MIC_PERMISSION_REQUEST = 1001;
    private static final int FILE_CHOOSER_REQUEST = 1002;

    private ValueCallback<Uri[]> fileChooserCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .setDomain(VIRTUAL_DOMAIN)
                        .addPathHandler(
                                "/assets/",
                                new WebViewAssetLoader.AssetsPathHandler(this)
                        )
                        .build();

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);

        // اجازه دسترسی به URI فایل انتخاب‌شده از فایل‌منیجر
        settings.setAllowContentAccess(true);

        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                return assetLoader.shouldInterceptRequest(
                        request.getUrl()
                );
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(
                    @NonNull PermissionRequest request
            ) {
                runOnUiThread(() ->
                        request.grant(request.getResources())
                );
            }

            // بازکردن انتخاب‌گر فایل اندروید برای ورودی‌های HTML
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params
            ) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }

                fileChooserCallback = callback;

                try {
                    Intent intent = params.createIntent();

                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                    );

                    return true;

                } catch (Exception e) {
                    fileChooserCallback = null;
                    callback.onReceiveValue(null);

                    Toast.makeText(
                            MainActivity.this,
                            "بازکردن انتخاب‌گر فایل ممکن نشد.",
                            Toast.LENGTH_LONG
                    ).show();

                    return false;
                }
            }
        });

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION_REQUEST
            );
        }

        webView.loadUrl(
                "https://" + VIRTUAL_DOMAIN
                        + "/assets/www/index.html"
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQUEST) {

            if (fileChooserCallback != null) {

                Uri[] results = null;

                if (resultCode == Activity.RESULT_OK
                        && data != null) {

                    Uri uri = data.getData();

                    if (uri != null) {
                        results = new Uri[]{uri};

                    } else if (data.getClipData() != null) {

                        int count =
                                data.getClipData().getItemCount();

                        results = new Uri[count];

                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData()
                                    .getItemAt(i)
                                    .getUri();
                        }
                    }
                }

                fileChooserCallback.onReceiveValue(results);
                fileChooserCallback = null;
            }
        }
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
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == MIC_PERMISSION_REQUEST
                && (grantResults.length == 0
                || grantResults[0]
                != PackageManager.PERMISSION_GRANTED)) {

            Toast.makeText(
                    this,
                    "بدون دسترسی میکروفون، جست‌وجوی صوتی کار نمی‌کند.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}