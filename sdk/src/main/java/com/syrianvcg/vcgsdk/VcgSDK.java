package com.syrianvcg.vcgsdk;

/**
 * ═══════════════════════════════════════════════════════════════
 *  Syrian VCG Language SDK  —  v2.0.1
 *  حزمة تطوير لغة VCG السورية
 *
 *  Usage:
 *    VcgSDK sdk = new VcgSDK();
 *    String html  = sdk.compile("show(\"Hello VCG!\")");
 *    String html2 = sdk.compile(code, "MyApp", "midnight");
 *    boolean ok   = sdk.validate(code);
 *    String[]  tokens = sdk.tokenize(code);
 *
 *  Thread-safe: each compile() call is stateless.
 * ═══════════════════════════════════════════════════════════════
 */
public class VcgSDK {

    // ── Version ────────────────────────────────────────────────
    public static final String VERSION       = "2.0.1";
    public static final String LANGUAGE_VER  = "2.0";
    public static final String RELEASE_DATE  = "2026-06-24";
    public static final String EDITION       = "Full Edition";
    public static final String COPYRIGHT     = "Syrian VCG Project 2026";

    // ── Available themes ────────────────────────────────────────
    public static final String THEME_OLIVE    = "olive";
    public static final String THEME_MIDNIGHT = "midnight";
    public static final String THEME_AMOLED   = "amoled";
    public static final String THEME_SAND     = "sand";
    public static final String THEME_WHITE    = "white";

    private String defaultTheme = THEME_OLIVE;

    // ── Constructor ─────────────────────────────────────────────
    public VcgSDK() {}

    public VcgSDK(String defaultTheme) {
        this.defaultTheme = defaultTheme;
    }

    // ── Primary API ─────────────────────────────────────────────

    /**
     * Compile VCG source code to a standalone HTML page.
     * @param vcgCode  VCG source
     * @return         Full HTML string ready to display in WebView or save to file
     */
    public String compile(String vcgCode) {
        return VcgInterpreter.buildHtml(vcgCode, "VCG App", "{}", defaultTheme);
    }

    /**
     * Compile VCG source with title and theme.
     * @param vcgCode  VCG source
     * @param title    Page title shown in browser/WebView
     * @param theme    olive | midnight | amoled | sand | white
     */
    public String compile(String vcgCode, String title, String theme) {
        return VcgInterpreter.buildHtml(vcgCode, title, "{}", theme);
    }

    /**
     * Compile VCG source with assets (base64-encoded media).
     * @param vcgCode     VCG source
     * @param title       Page title
     * @param assetsJson  JSON map: {"asset:ID":"data:image/png;base64,..."}
     * @param theme       Theme name
     */
    public String compile(String vcgCode, String title, String assetsJson, String theme) {
        return VcgInterpreter.buildHtml(vcgCode, title, assetsJson, theme);
    }

    /**
     * Quick-run validation: wraps compile and catches exceptions.
     * @return true if the code compiled without throwing
     */
    public boolean validate(String vcgCode) {
        try {
            compile(vcgCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the embedded JS runtime as a standalone string.
     * Useful for injecting into an existing web page.
     */
    public String getRuntime() {
        return VcgInterpreter.getPublicRuntime();
    }

    /**
     * Returns CSS styles for the given theme.
     */
    public String getStyles(String theme) {
        return VcgInterpreter.getPublicStyles(theme);
    }

    /**
     * Returns a list of all VCG keywords.
     */
    public String[] getKeywords() {
        return VcgKeywords.ALL;
    }

    /**
     * Returns a list of all VCG built-in functions.
     */
    public String[] getBuiltins() {
        return VcgKeywords.BUILTINS;
    }

    /**
     * Returns version string.
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Returns metadata map as formatted string.
     */
    public String getMetadata() {
        return "VCG SDK v" + VERSION +
               " | Language: " + LANGUAGE_VER +
               " | Edition: " + EDITION +
               " | " + COPYRIGHT;
    }
}
