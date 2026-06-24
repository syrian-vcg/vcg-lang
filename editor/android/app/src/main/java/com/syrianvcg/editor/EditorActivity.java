package com.syrianvcg.editor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
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

    // ⚠️ Android 14 (targetSdk 34): الاعتماد على Activity.onBackPressed()
    // أصبح قديماً (deprecated) ولا يتيح المشاركة في حركة "زر الرجوع التنبّؤي"
    // (Predictive Back) التي تتوقعها أنظمة Android 13/14 الحديثة. الأسلوب
    // الموصى به هو تسجيل OnBackPressedCallback عبر getOnBackPressedDispatcher()،
    // وقد فُعِّل ذلك في المانفست (enableOnBackInvokedCallback="true").
    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (modified) {
                new AlertDialog.Builder(EditorActivity.this, R.style.VCGDialog)
                    .setTitle("حفظ التغييرات؟")
                    .setMessage("هل تريد حفظ " + filename + " قبل الخروج؟")
                    .setPositiveButton("حفظ", (d, w) -> { saveFile(); finish(); })
                    .setNegativeButton("تجاهل", (d, w) -> finish())
                    .setNeutralButton("إلغاء", null)
                    .show();
            } else {
                finish();
            }
        }
    };

    // Quick-insert keys for mobile.
    // الترتيب: الأكثر استخداماً أولاً لتقليل التمرير.
    // المفاتيح ذات الأزواج ("()", "[]", "\"\"", "{}") تُعالَج في insertAtCursor
    // لتضع المؤشر داخل الزوج تلقائياً.
    private static final String[] QUICK_KEYS = {
        // ── تعليق ──────────────────────────────────────────────────────────
        "# ", "// ",
        // ── متغيرات ودوال ─────────────────────────────────────────────────
        "let ", "const ", "func ", "return ",
        // ── تحكم ──────────────────────────────────────────────────────────
        "if ", "else ", "else if ", "while ", "for ", "in ", "repeat ",
        "break", "continue",
        // ── أزواج الأقواس (المؤشر يدخل بالداخل تلقائياً) ─────────────────
        "()", "[]", "\"\"", "``", "{}",
        // ── عمليات شائعة ──────────────────────────────────────────────────
        "->", "|>", ":", "=", "+=", "-=", "!=", "==", "<=", ">=",
        // ── واجهة المستخدم ─────────────────────────────────────────────────
        "show(", "h(", "l(", "btn(", "url(", "key(", "img(", "video(",
        "youtube(", "facebook(", "instagram(", "xsocial(",
        // ── كلاسات وأنواع ──────────────────────────────────────────────────
        "class ", "extends ", "new ", "self.", "super",
        "type ", "enum ", "union ",
        // ── وحدات ─────────────────────────────────────────────────────────
        "module ", "export ", "from ", "import ", "as ",
        // ── غير متزامن ────────────────────────────────────────────────────
        "async func ", "await ", "defer ",
        // ── معالجة الأخطاء ────────────────────────────────────────────────
        "try", "catch ", "throw ", "safe", "guard ", "assert(",
        // ── مطابقة الأنماط ────────────────────────────────────────────────
        "match ", "when ",
        // ── تفاعلي ────────────────────────────────────────────────────────
        "$set(", "$get(", "watch(",
        // ── تواصل ─────────────────────────────────────────────────────────
        "c ", "send(", "recv(",
        // ── مصفوفات وعمليات عليا ──────────────────────────────────────────
        "map(", "filter(", "reduce(", "find(",
        "sum(", "avg(", "unique(", "merge(", "has(",
        "flat(", "chunk(", "zip(", "first(", "last(",
        // ── رياضيات ────────────────────────────────────────────────────────
        "gcd(", "lcm(", "fib(", "factorial(", "is_prime(",
        // ── مساعدات ───────────────────────────────────────────────────────
        "uuid()", "hash(", "copy(", "type_of(", "sleep(",
        // ── اختبار ────────────────────────────────────────────────────────
        "test ", "assert_eq(", "assert_true(", "assert_false(",
        // ── وصف ───────────────────────────────────────────────────────────
        "public ", "w ", "x ",
        // ── قيم ثابتة ─────────────────────────────────────────────────────
        "true", "false", "nil", "and", "or", "not",
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
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        setSupportActionBar(findViewById(R.id.editor_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        android.widget.TextView tvTitle = findViewById(R.id.toolbar_title);
        android.widget.TextView tvSub   = findViewById(R.id.toolbar_subtitle);
        if (tvTitle != null) tvTitle.setText(filename);
        if (tvSub   != null) tvSub.setText(projectName);

        codeEditor  = findViewById(R.id.code_editor);
        lineNumbers = findViewById(R.id.line_numbers);
        previewWebView   = findViewById(R.id.preview_webview);
        previewSkeleton  = findViewById(R.id.preview_skeleton);
        previewContainer = findViewById(R.id.preview_container);
        editorContainer  = findViewById(R.id.editor_code_container);
        previewSkeleton.setDark(VcgThemeHelper.isDark(VcgThemeHelper.resolve(settings.getAppTheme(), this)));

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
                android.widget.TextView t = findViewById(R.id.toolbar_title);
                if (t != null) t.setText("• " + filename);
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
        findViewById(R.id.nav_save).setOnClickListener(v -> saveFile());
        findViewById(R.id.nav_terminal).setOnClickListener(v -> openTerminal());
        findViewById(R.id.btn_more).setOnClickListener(this::showEditorPopupMenu);

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
        codeEditor.applyTheme(VcgThemeHelper.isDark(VcgThemeHelper.resolve(settings.getAppTheme(), this)));

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
        codeEditor.setAutoIndentEnabled(settings.getAutoIndent());
        codeEditor.setSyntaxHighlightEnabled(settings.getSyntaxHighlight());
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
        // ⚠️ كانت تُستخدم Base64.NO_PADDING مع html.getBytes() بدون ترميز صريح:
        // (1) getBytes() بلا UTF-8 يعتمد على ترميز المنصّة الافتراضي، وقد يكسر
        //     النص العربي على أجهزة بترميز افتراضي مختلف.
        // (2) NO_PADDING يولّد base64 بلا "=" نهائية، وبعض إصدارات محرّك
        //     WebView/Chromium لا تفكّك base64 بلا padding بشكل موثوق.
        // loadDataWithBaseURL مع تحديد "UTF-8" صراحة يتجنّب base64 كلياً
        // ويتعامل مع المحتوى مباشرة كنص، وهو الأسلوب الموثّق والأكثر استقراراً.
        previewWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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
        android.widget.ImageView icon = findViewById(R.id.icon_toggle_preview);
        TextView lbl = findViewById(R.id.lbl_toggle_preview);
        icon.setImageResource(previewVisible ? R.drawable.ic_nav_preview : R.drawable.ic_nav_preview_off);

        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.colorAccentSecondary, tv, true);
        int accent = tv.data;
        androidx.core.widget.ImageViewCompat.setImageTintList(icon,
                android.content.res.ColorStateList.valueOf(accent));
        lbl.setText(previewVisible ? "إخفاء" : "معاينة");
        lbl.setTextColor(accent);
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

        // ── أزواج الأقواس: أدرج الزوج وضع المؤشر بالداخل ─────────────────
        // تُعالَج الأزواج بشكل موحّد: نُدرج النص كاملاً ثم نُعيد ضبط المؤشر
        // ليقع بعد حرف الفتح مباشرةً حتى يكتب المستخدم داخل الزوج فوراً.
        switch (text) {
            case "\"\"": {
                // إذا كان هناك نص محدد، احتوِه بين علامتَي التنصيص
                if (start != end) {
                    codeEditor.getEditableText().insert(end, "\"");
                    codeEditor.getEditableText().insert(start, "\"");
                    codeEditor.setSelection(end + 2);
                } else {
                    codeEditor.getEditableText().replace(start, end, "\"\"");
                    codeEditor.setSelection(start + 1);
                }
                return;
            }
            case "``": {
                if (start != end) {
                    codeEditor.getEditableText().insert(end, "`");
                    codeEditor.getEditableText().insert(start, "`");
                    codeEditor.setSelection(end + 2);
                } else {
                    codeEditor.getEditableText().replace(start, end, "``");
                    codeEditor.setSelection(start + 1);
                }
                return;
            }
            case "()": {
                if (start != end) {
                    codeEditor.getEditableText().insert(end, ")");
                    codeEditor.getEditableText().insert(start, "(");
                    codeEditor.setSelection(end + 2);
                } else {
                    codeEditor.getEditableText().replace(start, end, "()");
                    codeEditor.setSelection(start + 1);
                }
                return;
            }
            case "[]": {
                if (start != end) {
                    codeEditor.getEditableText().insert(end, "]");
                    codeEditor.getEditableText().insert(start, "[");
                    codeEditor.setSelection(end + 2);
                } else {
                    codeEditor.getEditableText().replace(start, end, "[]");
                    codeEditor.setSelection(start + 1);
                }
                return;
            }
            case "{}": {
                // أدرج {} على سطر واحد للاستخدامات الخفيفة
                if (start != end) {
                    codeEditor.getEditableText().insert(end, "}");
                    codeEditor.getEditableText().insert(start, "{");
                    codeEditor.setSelection(end + 2);
                } else {
                    // كتلة متعددة الأسطر مع المؤشر في المنتصف
                    String block = "{\n    \n}";
                    codeEditor.getEditableText().replace(start, end, block);
                    // ضع المؤشر داخل الكتلة (بعد "{\n    ")
                    codeEditor.setSelection(start + 6);
                }
                return;
            }
        }

        // ── نص عادي: أدرجه مباشرةً ─────────────────────────────────────
        codeEditor.getEditableText().replace(start, end, text);
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
        android.widget.TextView t = findViewById(R.id.toolbar_title);
        if (t != null) t.setText(filename);
        Toast.makeText(this, "✓ حُفظ", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Actions moved to the bottom navigation bar and the custom toolbar popup menu.
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
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

    /** Shows a custom-styled popup menu (card, icon badges, animation) anchored to the toolbar button. */
    private void showEditorPopupMenu(View anchor) {
        View content = getLayoutInflater().inflate(R.layout.popup_menu_editor, null);

        PopupWindow popup = new PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(R.style.VCGPopupAnimation);

        content.findViewById(R.id.popup_item_share).setOnClickListener(v -> {
            popup.dismiss();
            shareCode();
        });
        content.findViewById(R.id.popup_item_terminal).setOnClickListener(v -> {
            popup.dismiss();
            openTerminal();
        });
        content.findViewById(R.id.popup_item_close).setOnClickListener(v -> {
            popup.dismiss();
            getOnBackPressedDispatcher().onBackPressed();
        });

        popup.showAsDropDown(anchor, -anchor.getWidth() * 4, 6, Gravity.END);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySettingsToEditor();
        if (previewVisible) schedulePreviewUpdate();
    }
}
