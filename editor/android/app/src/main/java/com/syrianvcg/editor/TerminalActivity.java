package com.syrianvcg.editor;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TerminalActivity — وحدة تحكم لعرض سجل التشغيل (logs/errors) وتشغيل أوامر VCG سريعة (REPL)
 */
public class TerminalActivity extends AppCompatActivity {

    private TextView logView;
    private ScrollView logScroll;
    private EditText input;
    private StringBuilder buffer = new StringBuilder();
    private VcgStorage storage;
    private String projectId;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss", Locale.US);
    /**
     * ⚠️ كان buffer ينمو بلا أي حدّ مع طول استخدام التيرمينال، ويُكتب بالكامل
     * في SharedPreferences عند كل سطر جديد. مع الاستخدام الطويل هذا يسبب
     * بطئاً متزايداً (نسخ نص أكبر فأكبر في كل عملية كتابة)، وقد يقترب من حدّ
     * SharedPreferences لحجم القيمة الواحدة (~1MB) ويرمي
     * TransactionTooLargeException. نحافظ على آخر عدد محدود من الأسطر فقط.
     */
    private static final int MAX_LOG_LINES = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_terminal);

        storage = new VcgStorage(this);
        projectId = getIntent().getStringExtra("projectId");

        setSupportActionBar(findViewById(R.id.terminal_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("⌨ Terminal");
        }

        logView   = findViewById(R.id.terminal_log);
        logScroll = findViewById(R.id.terminal_scroll);
        input     = findViewById(R.id.terminal_input);

        logView.setTypeface(Typeface.MONOSPACE);

        // Load any previously saved log, or show welcome message if none exists
        String saved = getPreviousLog();
        if (saved != null && !saved.isEmpty()) {
            buffer.append(saved);
            refresh();
        } else {
            printWelcome();
        }

        ImageButton btnRun = findViewById(R.id.btn_terminal_run);
        btnRun.setOnClickListener(v -> executeInput());

        ImageButton btnClear = findViewById(R.id.btn_terminal_clear);
        btnClear.setOnClickListener(v -> clearLog());

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                executeInput();
                return true;
            }
            return false;
        });
    }

    private void printWelcome() {
        appendLine("$ Syrian VCG Terminal — v2.1.0", "#4DC95A");
        appendLine("# اكتب كود VCG واضغط Enter للتشغيل المباشر", "#4A6A4A");
        appendLine("# مثال: show(\"Hello\", 1+2)", "#4A6A4A");
        appendLine("", "#4A6A4A");
    }

    private void executeInput() {
        String code = input.getText() != null ? input.getText().toString().trim() : "";
        if (code.isEmpty()) return;
        appendLine("› " + code, "#6AB0FF");
        input.setText("");

        // Run via headless mini-eval using the same interpreter runtime, capturing show() output as plain text.
        String result = VcgHeadlessRunner.run(code, projectId);
        for (String line : result.split("\n")) {
            if (line.startsWith("ERR:")) {
                appendLine(line.substring(4), "#F87171");
            } else if (!line.isEmpty()) {
                appendLine(line, "#A8E080");
            }
        }
        persistLog();
    }

    public void appendFromRun(String filename, boolean success, String detail) {
        String time = TIME_FMT.format(new Date());
        if (success) {
            appendLine("[" + time + "] ▶ " + filename + " — تم التشغيل (" + detail + " سطر مخرجات)", "#4DC95A");
        } else {
            appendLine("[" + time + "] ✗ " + filename + " — خطأ: " + detail, "#F87171");
        }
        persistLog();
    }

    private void appendLine(String text, String hexColor) {
        buffer.append("<font color='").append(hexColor).append("'>")
              .append(android.text.TextUtils.htmlEncode(text))
              .append("</font><br/>");
        trimBufferIfNeeded();
        refresh();
    }

    /** يحافظ على آخر MAX_LOG_LINES سطر فقط، فيتجنّب نمو buffer بلا حدود. */
    private void trimBufferIfNeeded() {
        // عدّ تقريبي عبر "<br/>" بما أن كل سطر يُنهى به دوماً في appendLine.
        String marker = "<br/>";
        int count = 0, idx = -1;
        while ((idx = buffer.indexOf(marker, idx + 1)) != -1) count++;
        if (count <= MAX_LOG_LINES) return;

        int toRemove = count - MAX_LOG_LINES;
        int cut = -1;
        for (int i = 0; i < toRemove; i++) {
            cut = buffer.indexOf(marker, cut + 1);
            if (cut == -1) break;
        }
        if (cut != -1) {
            buffer.delete(0, cut + marker.length());
        }
    }

    private void refresh() {
        logView.setText(android.text.Html.fromHtml(buffer.toString(), android.text.Html.FROM_HTML_MODE_LEGACY));
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void clearLog() {
        buffer = new StringBuilder();
        VcgHeadlessRunner.resetSession(projectId);
        printWelcome();
        persistLog();
    }

    private void persistLog() {
        getSharedPreferences("vcg_terminal", MODE_PRIVATE)
            .edit().putString("log_" + (projectId == null ? "global" : projectId), buffer.toString()).apply();
    }

    private String getPreviousLog() {
        return getSharedPreferences("vcg_terminal", MODE_PRIVATE)
            .getString("log_" + (projectId == null ? "global" : projectId), "");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
