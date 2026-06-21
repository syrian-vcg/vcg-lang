package com.syrianvcg.editor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.List;

public class EditorActivity extends AppCompatActivity {

    private VcgCodeEditor codeEditor;
    private TextView lineNumbers;
    private String filename;
    private String projectId;
    private String projectName;
    private VcgStorage storage;
    private VcgSettings settings;
    private boolean modified = false;
    private boolean previewVisible = true;

    private WebView previewWebView;
    private VcgSkeletonView previewSkeleton;
    private View previewContainer;
    private View editorContainer;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debouncedPreview;

    // Quick-insert keys for mobile
    private static final String[] QUICK_KEYS = {
        "show(", "let ", "const ", "func ", "return",
        "if ", "else", "while ", "for ", "in ", "repeat ",
        "break", "continue",
        "class ", "extends ", "new ", "self.", "super",
        "module ", "export ", "from ", "import ", "as ",
        "async func ", "await ", "defer ",
        "type ", "enum ", "union ",
        "try", "catch ", "throw ", "safe", "guard ", "assert(",
        "match ", "when ",
        "$set(", "$get(", "watch(",
        "c ", "send(", "recv(",
        "map(", "filter(", "reduce(", "find(",
        "test ", "assert_eq(", "assert_true(",
        "h(", "l(", "btn(", "url(", "key(", "img(", "video(",
        "youtube(", "facebook(", "instagram(", "xsocial(",
        "sum(", "avg(", "unique(", "merge(", "has(",
        "gcd(", "lcm(", "fib(", "factorial(", "is_prime(",
        "uuid()", "hash(", "copy(", "type_of(",
        "public ", "w ", "x ",
        "true", "false", "nil", "and", "or", "not",
        "{", "}", "(", ")", "[", "]", "\"\"", "->", "|>"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_editor);

        filename    = getIntent().getStringExtra("filename");
        projectId   = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");
        storage     = new VcgStorage(this);
        settings    = new VcgSettings(this);

        setSupportActionBar(findViewById(R.id.editor_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(filename);
            getSupportActionBar().setSubtitle(projectName);
        }

        codeEditor  = findViewById(R.id.code_editor);
        lineNumbers = findViewById(R.id.line_numbers);
        previewWebView   = findViewById(R.id.preview_webview);
        previewSkeleton  = findViewById(R.id.preview_skeleton);
        previewContainer = findViewById(R.id.preview_container);
        editorContainer  = findViewById(R.id.editor_code_container);
        previewSkeleton.setDark(VcgThemeHelper.isDark(settings.getAppTheme()));

        applySettingsToEditor();
        setupPreviewWebView();

        // Load file content
        VcgFile file = storage.getFile(projectId, filename);
        if (file != null) {
            codeEditor.setText(file.getContent());
        }

        // Track changes
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                modified = true;
                updateLineNumbers();
                if (getSupportActionBar() != null)
                    getSupportActionBar().setTitle("• " + filename);
                schedulePreviewUpdate();
            }
        });

        updateLineNumbers();
        buildQuickKeyboard();

        // Sync scroll
        ScrollView editorScroll = findViewById(R.id.editor_scroll);
        ScrollView lineScroll   = findViewById(R.id.line_scroll);
        editorScroll.setOnScrollChangeListener((v, x, y, ox, oy) ->
            lineScroll.scrollTo(0, y));

        findViewById(R.id.btn_run).setOnClickListener(v -> runCode());
        findViewById(R.id.btn_toggle_preview).setOnClickListener(v -> togglePreview());
        findViewById(R.id.btn_insert_asset).setOnClickListener(v -> showAssetPicker());

        previewVisible = settings.getLivePreview();
        updatePreviewVisibility();
        if (previewVisible) schedulePreviewUpdate();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupPreviewWebView() {
        WebSettings ws = previewWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        previewWebView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (previewSkeleton != null) previewSkeleton.setVisibility(View.GONE);
            }
        });
    }

    private void applySettingsToEditor() {
        codeEditor.applyTheme(VcgThemeHelper.isDark(settings.getAppTheme()));

        int fontSize = settings.getFontSize();
        codeEditor.setTextSize(fontSize);
        lineNumbers.setTextSize(fontSize);

        String fontFamily = settings.getFontFamily();
        Typeface tf = "sans-serif".equals(fontFamily) ? Typeface.SANS_SERIF
                    : "serif".equals(fontFamily) ? Typeface.SERIF
                    : Typeface.MONOSPACE;
        codeEditor.setTypeface(tf);
        lineNumbers.setTypeface(Typeface.MONOSPACE);

        codeEditor.setHorizontallyScrolling(!settings.getWordWrap());
        lineNumbers.setVisibility(settings.getShowLineNumbers() ? View.VISIBLE : View.GONE);
    }

    private void schedulePreviewUpdate() {
        if (!previewVisible) return;
        if (debouncedPreview != null) debounceHandler.removeCallbacks(debouncedPreview);
        debouncedPreview = this::updatePreview;
        debounceHandler.postDelayed(debouncedPreview, 500);
    }

    private void updatePreview() {
        if (previewSkeleton != null && previewWebView.getVisibility() != View.GONE) {
            previewSkeleton.setVisibility(View.VISIBLE);
        }
        String code = codeEditor.getText() != null ? codeEditor.getText().toString() : "";
        String assetsJson = buildAssetsJson();
        String html = VcgInterpreter.buildHtml(code, filename, assetsJson, settings.getTheme());
        String encoded = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
        previewWebView.loadData(encoded, "text/html", "base64");
    }

    private String buildAssetsJson() {
        try {
            JSONObject o = new JSONObject();
            List<VcgAsset> assetList = storage.getAssetsInProject(projectId);
            for (VcgAsset a : assetList) {
                o.put(a.getAssetRef(), a.getDataUrl());
            }
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private void togglePreview() {
        previewVisible = !previewVisible;
        settings.setLivePreview(previewVisible);
        updatePreviewVisibility();
        if (previewVisible) updatePreview();
    }

    private void updatePreviewVisibility() {
        previewContainer.setVisibility(previewVisible ? View.VISIBLE : View.GONE);
        MaterialButton btn = findViewById(R.id.btn_toggle_preview);
        btn.setText(previewVisible ? "▦ إخفاء المعاينة" : "▦ معاينة مباشرة");
    }

    private void showAssetPicker() {
        List<VcgAsset> assetList = storage.getAssetsInProject(projectId);
        if (assetList.isEmpty()) {
            new AlertDialog.Builder(this, R.style.VCGDialog)
                .setTitle("لا يوجد وسائط")
                .setMessage("لم تقم برفع أي صور أو فيديو لهذا المشروع بعد. اذهب إلى \"الوسائط\" لإضافة ملفات.")
                .setPositiveButton("فتح الوسائط", (d, w) -> {
                    Intent i = new Intent(this, AssetsActivity.class);
                    i.putExtra("projectId", projectId);
                    i.putExtra("projectName", projectName);
                    startActivity(i);
                })
                .setNegativeButton("إلغاء", null)
                .show();
            return;
        }
        String[] labels = new String[assetList.size()];
        for (int i = 0; i < assetList.size(); i++) {
            VcgAsset a = assetList.get(i);
            labels[i] = (a.isVideo() ? "🎬 " : "🖼 ") + a.getName();
        }
        new AlertDialog.Builder(this, R.style.VCGDialog)
            .setTitle("إدراج وسائط")
            .setItems(labels, (d, which) -> {
                VcgAsset a = assetList.get(which);
                String snippet = a.isVideo()
                    ? "video(\"" + a.getAssetRef() + "\")"
                    : "img(\"" + a.getAssetRef() + "\")";
                insertAtCursor(snippet);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void updateLineNumbers() {
        String text = codeEditor.getText() != null ? codeEditor.getText().toString() : "";
        int lines = text.isEmpty() ? 1 : text.split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i).append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    private void buildQuickKeyboard() {
        LinearLayout container = findViewById(R.id.quick_keyboard);

        for (String key : QUICK_KEYS) {
            MaterialButton btn = new MaterialButton(this,
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(key);
            btn.setTextSize(11f);
            btn.setTypeface(Typeface.MONOSPACE);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setPaddingRelative(16, 4, 16, 4);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(4);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> insertAtCursor(key));
            container.addView(btn);
        }
    }

    private void insertAtCursor(String text) {
        int start = Math.max(codeEditor.getSelectionStart(), 0);
        int end   = Math.max(codeEditor.getSelectionEnd(), 0);
        if (start > end) { int tmp = start; start = end; end = tmp; }

        String insert = text;
        if (text.equals("\"\"")) {
            codeEditor.getEditableText().replace(start, end, "\"\"");
            codeEditor.setSelection(start + 1);
            return;
        }
        if (text.equals("{")) {
            insert = "{\n    \n}";
        }

        codeEditor.getEditableText().replace(start, end, insert);
    }

    private void runCode() {
        saveFile();
        String code = codeEditor.getText() != null ? codeEditor.getText().toString() : "";
        Intent intent = new Intent(this, OutputActivity.class);
        intent.putExtra("code", code);
        intent.putExtra("filename", filename);
        intent.putExtra("projectId", projectId);
        intent.putExtra("assetsJson", buildAssetsJson());
        intent.putExtra("theme", settings.getTheme());
        startActivity(intent);
    }

    private void saveFile() {
        if (codeEditor.getText() == null) return;
        String content = codeEditor.getText().toString();
        VcgFile file = new VcgFile(projectId, filename, content);
        storage.saveFile(file);
        modified = false;
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(filename);
        Toast.makeText(this, "✓ حُفظ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (id == R.id.action_save) { saveFile(); return true; }
        if (id == R.id.action_run)  { runCode();  return true; }
        if (id == R.id.action_terminal) { openTerminal(); return true; }
        if (id == R.id.action_share) { shareCode(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void openTerminal() {
        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    private void shareCode() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,
            codeEditor.getText() != null ? codeEditor.getText().toString() : "");
        share.putExtra(Intent.EXTRA_SUBJECT, filename);
        startActivity(Intent.createChooser(share, "مشاركة الكود"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySettingsToEditor();
        if (previewVisible) schedulePreviewUpdate();
    }

    @Override
    public void onBackPressed() {
        if (modified) {
            new AlertDialog.Builder(this, R.style.VCGDialog)
                .setTitle("حفظ التغييرات؟")
                .setMessage("هل تريد حفظ " + filename + " قبل الخروج؟")
                .setPositiveButton("حفظ", (d, w) -> { saveFile(); finish(); })
                .setNegativeButton("تجاهل", (d, w) -> finish())
                .setNeutralButton("إلغاء", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }
}
