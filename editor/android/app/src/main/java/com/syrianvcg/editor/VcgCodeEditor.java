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

    // VCG Colors — two palettes, tuned for contrast on light vs dark backgrounds
    private int COLOR_KEYWORD   = 0xFFB35900;  // amber/orange
    private int COLOR_KEYWORD2  = 0xFF1565C0;  // blue
    private int COLOR_STRING    = 0xFF1E8E3E;  // green
    private int COLOR_NUMBER    = 0xFF8E24AA;  // purple
    private int COLOR_COMMENT   = 0xFF8A9286;  // muted gray-green
    private int COLOR_FUNCTION  = 0xFF1F7A3D;  // primary green
    private int COLOR_UI_KW     = 0xFFB4790C;  // gold/brown
    private int COLOR_REACTIVE  = 0xFF0097A7;  // teal

    // Keyword patterns
    private static final Pattern PAT_COMMENT  = Pattern.compile("#.*|//.*");
    private static final Pattern PAT_STRING   = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|`[^`]*`");
    private static final Pattern PAT_NUMBER   = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+\\.?\\d*([eE][+-]?\\d+)?)\\b");
    private static final Pattern PAT_KEYWORD  = Pattern.compile(
        "\\b(let|const|func|return|if|else|while|for|in|repeat|break|continue" +
        "|and|or|not|true|false|nil|null|match|when|try|catch|throw|assert" +
        "|public|w|x|c|struct|new|self|import|as" +
        "|class|extends|implements|interface|super|this" +
        "|module|export|from" +
        "|async|await|promise|defer" +
        "|type|enum|union|generic" +
        "|ref|ptr|alloc|free" +
        "|safe|unsafe|guard" +
        "|doc|test|expect|mock" +
        "|with|case|pipeline)\\b");
    private static final Pattern PAT_UI_KW    = Pattern.compile(
        "\\b(show|input|html|youtube|facebook|instagram|xsocial|url|btn|key" +
        "|video|img|h|l|typeof|sizeof|watch|send|recv|pipe" +
        "|map|filter|reduce|find" +
        "|sum|avg|unique|flat|chunk|zip|first|last" +
        "|merge|has|del|entries|keys|values" +
        "|gcd|lcm|fib|factorial|is_prime" +
        "|JSON_stringify|JSON_parse|uuid|hash|copy|type_of|sleep" +
        "|assert_eq|assert_ne|assert_true|assert_false)\\b");
    private static final Pattern PAT_REACTIVE = Pattern.compile("\\$set|\\$get|\\$x");
    private static final Pattern PAT_FUNC_DEF = Pattern.compile(
        "(?<=func\\s)(\\w+)");
    private static final Pattern PAT_FUNC_CALL= Pattern.compile(
        "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?=\\()");

    private boolean highlighting = false;
    private boolean autoIndentEnabled = true;
    private boolean syntaxHighlightEnabled = true;

    public VcgCodeEditor(Context ctx) { super(ctx); init(); }
    public VcgCodeEditor(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public VcgCodeEditor(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle); init();
    }

    /** يفعّل/يعطّل إدراج المسافات التلقائي بعد سطر جديد، حسب إعدادات المستخدم. */
    public void setAutoIndentEnabled(boolean enabled) { this.autoIndentEnabled = enabled; }

    /** يفعّل/يعطّل تلوين الصياغة. عند التعطيل تُزال كل الألوان الحالية فوراً. */
    public void setSyntaxHighlightEnabled(boolean enabled) {
        this.syntaxHighlightEnabled = enabled;
        if (!enabled && getText() != null) {
            Editable s = getEditableText();
            ForegroundColorSpan[] old = s.getSpans(0, s.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan sp : old) s.removeSpan(sp);
        } else if (enabled && getText() != null) {
            highlight(getEditableText());
        }
    }

    private void init() {
        setHorizontalScrollBarEnabled(true);
        setHorizontallyScrolling(false);
        setBackgroundColor(0xFFFFFFFF);
        setTextColor(0xFF1B221C);
        setHighlightColor(0x331F7A3D);
        setCursorVisible(true);

        // Tab stop simulation
        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {
                if (!highlighting && syntaxHighlightEnabled) highlight(s);
            }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // Auto-indent on newline.
                // ⚠️ لا نعدّل الـ Editable هنا مباشرة: onTextChanged يُستدعى أثناء
                // معالجة التغيير الحالي، وأي insert() الآن يشغّل TextWatcher من
                // جديد بشكل متداخل وقد يسبب تكراراً غير متوقع أو IndexOutOfBounds
                // إذا كان المؤشر بآخر النص. نؤجل التنفيذ لما بعد انتهاء هذه الدورة.
                if (autoIndentEnabled && c == 1 && st >= 0 && st < s.length() && s.charAt(st) == '\n') {
                    final int insertPos = st;
                    post(() -> autoIndent(insertPos));
                }
            }
        });
    }

    private void autoIndent(int pos) {
        Editable e = getEditableText();
        if (e == null) return;
        // النص قد يتغيّر بين جدولة post() وتنفيذها (مثلاً المستخدم حذف حرفاً بسرعة)
        // لذلك نتحقق من الحدود مرة أخرى قبل أي وصول للنص.
        if (pos < 0 || pos >= e.length() || e.charAt(pos) != '\n') return;

        // Find indentation of previous line
        int lineStart = pos;
        while (lineStart > 0 && e.charAt(lineStart - 1) != '\n') lineStart--;
        int indent = 0;
        while (lineStart + indent < pos && e.charAt(lineStart + indent) == ' ') indent++;
        // Extra indent after {
        if (pos > 0 && e.charAt(pos - 1) == '{') indent += 4;

        if (indent > 0) {
            int insertAt = pos + 1;
            if (insertAt > e.length()) return; // حماية إضافية ضد تغيّر النص بين الجدولة والتنفيذ
            StringBuilder spaces = new StringBuilder();
            for (int i = 0; i < indent; i++) spaces.append(' ');
            e.insert(insertAt, spaces.toString());
        }
    }

    /**
     * يطبّق لوحة ألوان مناسبة لسمة التطبيق الحالية على خلفية/نص المحرر
     * وعلى ألوان تلوين الكود، حتى تبقى مقروءة على الخلفيات الداكنة أيضاً.
     */
    public void applyTheme(boolean dark) {
        if (dark) {
            setBackgroundColor(0xFF1A1D1F);
            setTextColor(0xFFF1F3F1);
            setHighlightColor(0x553FBF6B);
            COLOR_KEYWORD  = 0xFFE6A23C;
            COLOR_KEYWORD2 = 0xFF5B9CFF;
            COLOR_STRING   = 0xFF4DD97A;
            COLOR_NUMBER   = 0xFFC792EA;
            COLOR_COMMENT  = 0xFF6F7A72;
            COLOR_FUNCTION = 0xFF4DD97A;
            COLOR_UI_KW    = 0xFFE0A93D;
            COLOR_REACTIVE = 0xFF4FD6E6;
        } else {
            setBackgroundColor(0xFFFFFFFF);
            setTextColor(0xFF1B221C);
            setHighlightColor(0x331F7A3D);
            COLOR_KEYWORD  = 0xFFB35900;
            COLOR_KEYWORD2 = 0xFF1565C0;
            COLOR_STRING   = 0xFF1E8E3E;
            COLOR_NUMBER   = 0xFF8E24AA;
            COLOR_COMMENT  = 0xFF8A9286;
            COLOR_FUNCTION = 0xFF1F7A3D;
            COLOR_UI_KW    = 0xFFB4790C;
            COLOR_REACTIVE = 0xFF0097A7;
        }
        if (getText() != null) highlight(getEditableText());
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
