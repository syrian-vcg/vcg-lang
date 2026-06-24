package com.syrianvcg.editor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * VcgBuildActivity — شاشة بناء التطبيق
 *
 * تُفتح تلقائياً عندما يُشغِّل المستخدم ملف Generate_Stack.apk.yml
 * في VCG Editor. تعرض شريط التقدم والسجل، وعند اكتمال البناء
 * تُتيح مشاركة الملفات الناتجة (APK + ZIP + PDF).
 */
public class VcgBuildActivity extends AppCompatActivity {

    public static final String EXTRA_PROJECT_ID = "project_id";

    private ProgressBar  progressBar;
    private TextView     tvPercent;
    private TextView     tvStatus;
    private LinearLayout logContainer;
    private ScrollView   logScroll;
    private Button       btnShare;
    private Button       btnClose;
    private TextView     tvTitle;

    private VcgAppBuilder.BuildResult buildResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(buildLayout());
        startBuild();
    }

    // ── بدء البناء ──────────────────────────────────────────────────
    private void startBuild() {
        String projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);

        VcgStorage storage = VcgStorage.getInstance(this);
        VcgProject project = storage.getProjectById(projectId);
        if (project == null) {
            appendLog("❌ لم يُعثر على المشروع: " + projectId);
            return;
        }

        tvTitle.setText("🔨 بناء: " + project.getName());

        VcgAppBuilder.build(this, storage, project, new VcgAppBuilder.BuildListener() {
            @Override
            public void onProgress(int percent, String message) {
                progressBar.setProgress(percent);
                tvPercent.setText(percent + "%");
                tvStatus.setText(message);
                appendLog(message);
                logScroll.post(() -> logScroll.fullScroll(ScrollView.FOCUS_DOWN));
            }

            @Override
            public void onSuccess(VcgAppBuilder.BuildResult result) {
                buildResult = result;
                appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                appendLog("✅ اكتمل البناء في " + result.buildTimeMs + "ms");
                if (result.apkFile != null)
                    appendLog("📱 APK: " + result.apkFile.getName());
                if (result.zipFile != null)
                    appendLog("📦 ZIP: " + result.zipFile.getName());
                if (result.pdfFile != null)
                    appendLog("📄 PDF: " + result.pdfFile.getName());

                btnShare.setVisibility(View.VISIBLE);
                btnClose.setText("✔ إغلاق");
            }

            @Override
            public void onError(String error) {
                appendLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                appendLog("❌ فشل البناء: " + error);
                btnClose.setText("إغلاق");
            }
        });
    }

    // ── مشاركة الملفات ──────────────────────────────────────────────
    private void shareResults() {
        if (buildResult == null) return;
        List<Uri> uris = buildResult.getAllUris(this);
        if (uris.isEmpty()) {
            Toast.makeText(this, "لا توجد ملفات للمشاركة", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(
            Intent.EXTRA_STREAM, new java.util.ArrayList<>(uris));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "مشاركة ملفات البناء"));
    }

    // ── سجل البناء ──────────────────────────────────────────────────
    private void appendLog(String msg) {
        runOnUiThread(() -> {
            TextView line = new TextView(this);
            line.setText(msg);
            line.setTextColor(msg.startsWith("❌") ? 0xFFE05252
                : msg.startsWith("✅") ? 0xFF4DC95A
                : msg.startsWith("━") ? 0xFF556655
                : 0xFFB0C8B0);
            line.setTextSize(12.5f);
            line.setPadding(0, 2, 0, 2);
            line.setTypeface(android.graphics.Typeface.MONOSPACE);
            logContainer.addView(line);
        });
    }

    // ── بناء الواجهة برمجياً (بدون XML) ────────────────────────────
    private View buildLayout() {
        // جذر
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF060C0E);
        root.setPadding(dp(16), dp(20), dp(16), dp(16));

        // عنوان
        tvTitle = new TextView(this);
        tvTitle.setText("🔨 VCG App Builder");
        tvTitle.setTextColor(0xFF4DC95A);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dp(12));
        root.addView(tvTitle);

        // شريط التقدم
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        pbParams.bottomMargin = dp(6);
        root.addView(progressBar, pbParams);

        // نسبة + حالة
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        tvPercent = new TextView(this);
        tvPercent.setText("0%");
        tvPercent.setTextColor(0xFF4DC95A);
        tvPercent.setTextSize(13f);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPercent.setMinWidth(dp(42));

        tvStatus = new TextView(this);
        tvStatus.setText("جارٍ التهيئة...");
        tvStatus.setTextColor(0xFFB0C8B0);
        tvStatus.setTextSize(12f);

        row.addView(tvPercent);
        row.addView(tvStatus);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(10);
        root.addView(row, rowParams);

        // حاوي السجل مع ScrollView
        logScroll = new ScrollView(this);
        logScroll.setBackgroundColor(0xFF0F1E10);
        // تعيين padding داخلي
        logScroll.setPadding(dp(10), dp(8), dp(10), dp(8));

        logContainer = new LinearLayout(this);
        logContainer.setOrientation(LinearLayout.VERTICAL);
        logScroll.addView(logContainer);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(300));
        scrollParams.bottomMargin = dp(12);
        root.addView(logScroll, scrollParams);

        // زرّا المشاركة والإغلاق
        btnShare = new Button(this);
        btnShare.setText("📤 مشاركة الملفات (APK + ZIP + PDF)");
        btnShare.setVisibility(View.GONE);
        btnShare.setBackgroundColor(0xFF1F7A3D);
        btnShare.setTextColor(0xFFFFFFFF);
        btnShare.setOnClickListener(v -> shareResults());
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        shareParams.bottomMargin = dp(8);
        root.addView(btnShare, shareParams);

        btnClose = new Button(this);
        btnClose.setText("إلغاء");
        btnClose.setBackgroundColor(0xFF2A2A2A);
        btnClose.setTextColor(0xFFCCCCCC);
        btnClose.setOnClickListener(v -> finish());
        root.addView(btnClose, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
