package com.syrianvcg.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.appcompat.widget.AppCompatEditText;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom EditText with VCG syntax highlighting
 */
public class VcgCodeEditor extends AppCompatEditText {

    // VCG Colors (dark olive theme)
    private static final int COLOR_KEYWORD   = 0xFFFF9040;  // orange
    private static final int COLOR_KEYWORD2  = 0xFF6AB0FF;  // blue
    private static final int COLOR_STRING    = 0xFF7ACC6A;  // green
    private static final int COLOR_NUMBER    = 0xFFCC99FF;  // purple
    private static final int COLOR_COMMENT   = 0xFF4A6A4A;  // muted green
    private static final int COLOR_FUNCTION  = 0xFF4DC95A;  // bright green
    private static final int COLOR_UI_KW     = 0xFFF5C842;  // gold
    private static final int COLOR_REACTIVE  = 0xFF00D4FF;  // cyan

    // Keyword patterns
    private static final Pattern PAT_COMMENT  = Pattern.compile("#.*|//.*");
    private static final Pattern PAT_STRING   = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`[^`]*`");
    private static final Pattern PAT_NUMBER   = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+\\.?\\d*([eE][+-]?\\d+)?)\\b");
    private static final Pattern PAT_KEYWORD  = Pattern.compile(
        "\\b(let|const|func|return|if|else|while|for|in|repeat|break|continue" +
        "|and|or|not|true|false|nil|null|match|when|try|catch|throw|assert" +
        "|public|w|x|c|struct|new|self|import|as)\\b");
    private static final Pattern PAT_UI_KW    = Pattern.compile(
        "\\b(show|input|html|youtube|facebook|instagram|xsocial|url|btn|key" +
        "|video|img|h|l|typeof|sizeof|watch|send|recv|pipe)\\b");
    private static final Pattern PAT_REACTIVE = Pattern.compile("\\$set|\\$get|\\$x");
    private static final Pattern PAT_FUNC_DEF = Pattern.compile(
        "(?<=func\\s)(\\w+)");
    private static final Pattern PAT_FUNC_CALL= Pattern.compile(
        "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()");

    private boolean highlighting = false;

    public VcgCodeEditor(Context ctx) { super(ctx); init(); }
    public VcgCodeEditor(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public VcgCodeEditor(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle); init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(true);
        setHorizontallyScrolling(false);
        setBackgroundColor(0xFF081208);
        setTextColor(0xFFE8F5E0);
        setHighlightColor(0x334DC95A);
        setCursorVisible(true);

        // Tab stop simulation
        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {
                if (!highlighting) highlight(s);
            }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // Auto-indent on newline
                if (c == 1 && s.charAt(st) == '\n') {
                    autoIndent(st);
                }
            }
        });
    }

    private void autoIndent(int pos) {
        Editable e = getEditableText();
        if (e == null) return;
        // Find indentation of previous line
        int lineStart = pos;
        while (lineStart > 0 && e.charAt(lineStart - 1) != '\n') lineStart--;
        int indent = 0;
        while (lineStart + indent < pos && e.charAt(lineStart + indent) == ' ') indent++;
        // Extra indent after {
        if (pos > 0 && e.charAt(pos - 1) == '{') indent += 4;

        if (indent > 0) {
            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < indent; i++) spaces.append(' ');
            e.insert(pos + 1, spaces.toString());
        }
    }

    private void highlight(Editable s) {
        highlighting = true;
        try {
            String text = s.toString();

            // Remove existing spans
            ForegroundColorSpan[] old = s.getSpans(0, s.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan sp : old) s.removeSpan(sp);

            applyPattern(s, PAT_COMMENT,   COLOR_COMMENT);
            applyPattern(s, PAT_STRING,    COLOR_STRING);
            applyPattern(s, PAT_NUMBER,    COLOR_NUMBER);
            applyPattern(s, PAT_KEYWORD,   COLOR_KEYWORD);
            applyPattern(s, PAT_UI_KW,     COLOR_UI_KW);
            applyPattern(s, PAT_REACTIVE,  COLOR_REACTIVE);
            applyPattern(s, PAT_FUNC_CALL, COLOR_FUNCTION);

        } finally {
            highlighting = false;
        }
    }

    private void applyPattern(Editable s, Pattern p, int color) {
        Matcher m = p.matcher(s.toString());
        while (m.find()) {
            int grp = (m.groupCount() > 0 && p == PAT_FUNC_CALL) ? 1 : 0;
            int start = grp > 0 ? m.start(grp) : m.start();
            int end   = grp > 0 ? m.end(grp)   : m.end();
            if (start >= 0 && end <= s.length())
                s.setSpan(new ForegroundColorSpan(color),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
