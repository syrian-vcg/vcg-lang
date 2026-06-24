package com.syrianvcg.vcgsdk;

/**
 * ═══════════════════════════════════════════════════════
 *  VcgCompiler  —  Extended Compiler API for VCG SDK
 *
 *  Wraps VcgInterpreter with safe error handling and
 *  returns VcgResult objects for structured use.
 * ═══════════════════════════════════════════════════════
 */
public class VcgCompiler {

    private String theme = VcgSDK.THEME_OLIVE;
    private String title = "VCG App";
    private String assetsJson = "{}";

    // ── Builder-style setters ───────────────────────────

    public VcgCompiler setTheme(String theme) {
        this.theme = theme != null ? theme : VcgSDK.THEME_OLIVE;
        return this;
    }

    public VcgCompiler setTitle(String title) {
        this.title = title != null ? title : "VCG App";
        return this;
    }

    public VcgCompiler setAssets(String assetsJson) {
        this.assetsJson = assetsJson != null ? assetsJson : "{}";
        return this;
    }

    // ── Compile ─────────────────────────────────────────

    /**
     * Compile VCG source and return a VcgResult.
     */
    public VcgResult compile(String vcgCode) {
        if (vcgCode == null || vcgCode.trim().isEmpty()) {
            return new VcgResult(false, null, "Empty source code");
        }
        try {
            String html = VcgInterpreter.buildHtml(vcgCode, title, assetsJson, theme);
            return new VcgResult(true, html, null);
        } catch (Exception e) {
            return new VcgResult(false, null, e.getMessage());
        }
    }

    /**
     * Compile and return the raw HTML string, or null on failure.
     */
    public String compileToHtml(String vcgCode) {
        VcgResult r = compile(vcgCode);
        return r.isSuccess() ? r.getOutput() : null;
    }

    /**
     * Validate-only: returns true if code compiles without error.
     */
    public boolean validate(String vcgCode) {
        return compile(vcgCode).isSuccess();
    }

    // ── Static convenience methods ───────────────────────

    /**
     * One-shot compile with defaults (olive theme, "VCG App" title).
     */
    public static String quickCompile(String vcgCode) {
        return VcgInterpreter.buildHtml(vcgCode, "VCG App", "{}", VcgSDK.THEME_OLIVE);
    }

    /**
     * One-shot compile with theme.
     */
    public static String quickCompile(String vcgCode, String theme) {
        return VcgInterpreter.buildHtml(vcgCode, "VCG App", "{}", theme);
    }
}
