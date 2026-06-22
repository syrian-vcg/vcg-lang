package com.syrianvcg.editor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class OutputActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private VcgSkeletonView skeleton;
    private String code;
    private String filename;
    private String projectId;
    private String assetsJson;
    private String theme;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_output);

        code       = getIntent().getStringExtra("code");
        filename   = getIntent().getStringExtra("filename");
        projectId  = getIntent().getStringExtra("projectId");
        assetsJson = getIntent().getStringExtra("assetsJson");
        theme      = getIntent().getStringExtra("theme");
        if (theme == null) theme = "olive";

        setSupportActionBar(findViewById(R.id.output_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("▶ " + filename);
        }

        webView     = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        skeleton    = findViewById(R.id.output_skeleton);
        skeleton.setDark(VcgThemeHelper.isDark(new VcgSettings(this).getAppTheme()));

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                skeleton.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                skeleton.setVisibility(View.GONE);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith("data:")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == 100) progressBar.setVisibility(View.GONE);
            }
        });

        VcgJsInterface jsInterface = new VcgJsInterface(this, new VcgJsInterface.TerminalListener() {
            @Override public void onLog(String line) { logToTerminal(true, line); }
            @Override public void onError(String message) { logToTerminal(false, message); }
            @Override public void onSuccess(int outputCount) { logToTerminal(true, String.valueOf(outputCount)); }
        });
        webView.addJavascriptInterface(jsInterface, "VcgAndroid");

        runCode();
    }

    private boolean notifiedThisSession = false;

    /** نفس حد السطور المستخدَم في TerminalActivity، لتجنّب نمو سجل التيرمينال بلا حدود. */
    private static final int MAX_LOG_LINES = 500;

    private void logToTerminal(boolean success, String detail) {
        if (success && !notifiedThisSession) {
            notifiedThisSession = true;
            VcgNotifications.notifyRunSuccess(this, filename);
        }
        getSharedPreferences("vcg_terminal", MODE_PRIVATE).edit()
            .putString("last_run_" + (projectId == null ? "global" : projectId),
                (success ? "OK:" : "ERR:") + filename + ":" + detail + ":" + System.currentTimeMillis())
            .apply();

        // Append directly to the running terminal log buffer for this project, if present.
        String key = "log_" + (projectId == null ? "global" : projectId);
        String existing = getSharedPreferences("vcg_terminal", MODE_PRIVATE).getString(key, "");
        String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
        String color = success ? "#4DC95A" : "#F87171";
        String line = success
            ? "[" + time + "] ▶ " + filename + " — تم التشغيل (" + detail + " سطر مخرجات)"
            : "[" + time + "] ✗ " + filename + " — خطأ: " + detail;
        String appended = existing + "<font color='" + color + "'>" +
            android.text.TextUtils.htmlEncode(line) + "</font><br/>";
        appended = trimLogLines(appended);
        getSharedPreferences("vcg_terminal", MODE_PRIVATE).edit().putString(key, appended).apply();
    }

    /** يحافظ على آخر MAX_LOG_LINES سطر فقط من سجل النصوص المُنسَّق بـ HTML. */
    private static String trimLogLines(String log) {
        String marker = "<br/>";
        int count = 0, idx = -1;
        while ((idx = log.indexOf(marker, idx + 1)) != -1) count++;
        if (count <= MAX_LOG_LINES) return log;

        int toRemove = count - MAX_LOG_LINES;
        int cut = -1;
        for (int i = 0; i < toRemove; i++) {
            cut = log.indexOf(marker, cut + 1);
            if (cut == -1) break;
        }
        return cut != -1 ? log.substring(cut + marker.length()) : log;
    }

    private void runCode() {
        String html = VcgInterpreter.buildHtml(code, filename,
            assetsJson == null || assetsJson.isEmpty() ? "{}" : assetsJson, theme);

        // انظر التعليق المماثل في EditorActivity.updatePreview(): loadDataWithBaseURL
        // مع UTF-8 صريح أكثر استقراراً من base64 بلا padding لمحتوى عربي/يونيكود.
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
