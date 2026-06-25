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
        skeleton.setDark(VcgThemeHelper.isDark(VcgThemeHelper.resolve(new VcgSettings(this).getAppTheme(), this)));

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
        // ─────────────────────────────────────────────────────────────────────
        // نستخدم المفسِّر الحقيقي المكتوب بـ Java (VcgRealRunner) بدلاً من
        // توليد HTML/JavaScript عبر VcgInterpreter القديم. النتيجة هي نص خام
        // يُعرض داخل WebView بصفحة HTML بسيطة منسّقة (دون أي جلسة JS).
        // ─────────────────────────────────────────────────────────────────────
        long t0 = System.currentTimeMillis();

        // نصفّر الجلسة أولاً لضمان بيئة نظيفة عند كل تشغيل ملف كامل
        VcgRealRunner.resetSession("output_" + projectId);
        String rawOutput = VcgRealRunner.run(code, "output_" + projectId);

        long elapsed = System.currentTimeMillis() - t0;

        boolean hasError = rawOutput.startsWith("خطأ") || rawOutput.startsWith("ERR:");
        int lineCount = rawOutput.split("\n").length;

        String html = buildOutputHtml(rawOutput, filename, elapsed, hasError, theme);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        if (hasError) {
            logToTerminal(false, rawOutput.split("\n")[0]);
        } else {
            logToTerminal(true, String.valueOf(lineCount));
        }
    }

    /**
     * يبني صفحة HTML خفيفة لعرض مخرجات المفسِّر Java النصية بشكل منسَّق.
     */
    private static String buildOutputHtml(String output, String title, long elapsedMs,
                                          boolean hasError, String theme) {
        String bg, panel, border, accent, text, muted;
        switch (theme == null ? "olive" : theme) {
            case "midnight":
                bg="#0a0e1a"; panel="#10162a"; border="#1c2542";
                accent="#5b8cff"; text="#e6ecff"; muted="#5a6a8a"; break;
            case "amoled":
                bg="#000000"; panel="#0a0a0a"; border="#1a1a1a";
                accent="#4dc95a"; text="#f0f0f0"; muted="#555555"; break;
            case "sand":
                bg="#1c1812"; panel="#26211a"; border="#3a3226";
                accent="#e0a84d"; text="#f2e8d8"; muted="#7a6f5a"; break;
            case "white":
                bg="#ffffff"; panel="#f7f9f6"; border="#e2e6e1";
                accent="#1f7a3d"; text="#1b221c"; muted="#6b7568"; break;
            default: // olive
                bg="#060c0e"; panel="#0f1e10"; border="#1a3a1a";
                accent="#4dc95a"; text="#e8f5e0"; muted="#4a6a4a"; break;
        }

        String statusColor = hasError ? "#e25b4f" : accent;
        String statusLabel = hasError ? "✗ فشل التنفيذ" : "● تم التشغيل بنجاح";

        StringBuilder lines = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.isEmpty()) continue;
            String escaped = line
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
            String lineColor = (line.startsWith("خطأ") || line.startsWith("ERR:"))
                ? "#e25b4f" : text;
            lines.append("<span style=\"color:").append(lineColor)
                 .append(";display:block;padding:2px 4px\">")
                 .append(escaped).append("</span>\n");
        }

        return "<!DOCTYPE html>\n<html lang='ar' dir='rtl'>\n<head>\n"
            + "<meta charset='UTF-8'>\n"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>\n"
            + "<title>" + title + "</title>\n"
            + "<style>\n"
            + "body{background:" + bg + ";color:" + text + ";font-family:monospace;"
            +      "margin:0;padding:0.8rem;font-size:0.85rem;line-height:1.8}\n"
            + ".header{display:flex;align-items:center;gap:0.6rem;flex-wrap:wrap;"
            +         "background:" + panel + ";border:1px solid " + border + ";"
            +         "border-radius:10px;padding:0.5rem 0.7rem;margin-bottom:0.7rem;"
            +         "font-size:0.8rem}\n"
            + ".status{font-weight:700;color:" + statusColor + "}\n"
            + ".meta{color:" + muted + ";font-size:0.75rem;margin-right:auto}\n"
            + ".output{background:" + panel + ";border:1px solid " + border + ";"
            +          "border-radius:8px;padding:0.6rem 0.8rem}\n"
            + "</style>\n</head>\n<body>\n"
            + "<div class='header'>"
            + "<span class='status'>" + statusLabel + "</span>"
            + "<span class='meta'>" + elapsedMs + "ms &nbsp;•&nbsp; " + title + "</span>"
            + "</div>\n"
            + "<div class='output'>" + lines + "</div>\n"
            + "</body>\n</html>";
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
