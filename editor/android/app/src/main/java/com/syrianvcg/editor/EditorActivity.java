package com.syrianvcg.editor;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

public class EditorActivity extends AppCompatActivity {

    private VcgCodeEditor codeEditor;
    private TextView lineNumbers;
    private String filename;
    private VcgStorage storage;
    private boolean modified = false;
    private LinearLayout toolbar_keys;

    // Quick-insert keys for mobile
    private static final String[] QUICK_KEYS = {
        "show", "let", "func", "if", "else", "while", "for", "in",
        "return", "h(", "l(", "btn(", "url(", "key(", "img(",
        "youtube(", "facebook(", "instagram(", "xsocial(",
        "$set(", "$get(", "watch(", "c ", "w ", "public",
        "repeat", "match", "when", "try", "catch", "throw",
        "true", "false", "nil", "and", "or", "not",
        "{", "}", "(", ")", "[", "]", "\"\"", "→"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        filename = getIntent().getStringExtra("filename");
        storage  = new VcgStorage(this);

        setSupportActionBar(findViewById(R.id.editor_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(filename);
        }

        codeEditor  = findViewById(R.id.code_editor);
        lineNumbers = findViewById(R.id.line_numbers);

        // Setup editor
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setTextSize(14f);

        // Load file content
        VcgFile file = storage.getFile(filename);
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
            }
        });

        updateLineNumbers();
        buildQuickKeyboard();

        // Sync scroll
        ScrollView editorScroll = findViewById(R.id.editor_scroll);
        ScrollView lineScroll   = findViewById(R.id.line_scroll);
        editorScroll.setOnScrollChangeListener((v, x, y, ox, oy) ->
            lineScroll.scrollTo(0, y));

        // Run button
        findViewById(R.id.btn_run).setOnClickListener(v -> runCode());
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
        HorizontalScrollView scroll = findViewById(R.id.quick_keyboard_scroll);
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

        // Smart insert: add () or newline where appropriate
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
        // Move cursor inside parens
        if (insert.endsWith("(")) {
            // position stays after (
        }
    }

    private void runCode() {
        saveFile();
        String code = codeEditor.getText() != null ? codeEditor.getText().toString() : "";
        Intent intent = new Intent(this, OutputActivity.class);
        intent.putExtra("code", code);
        intent.putExtra("filename", filename);
        startActivity(intent);
    }

    private void saveFile() {
        if (codeEditor.getText() == null) return;
        String content = codeEditor.getText().toString();
        VcgFile file = new VcgFile(filename, content);
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
        if (id == R.id.action_undo) {
            // basic undo
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_share) { shareCode(); return true; }
        return super.onOptionsItemSelected(item);
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
    public void onBackPressed() {
        if (modified) {
            new androidx.appcompat.app.AlertDialog.Builder(this, R.style.VCGDialog)
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
