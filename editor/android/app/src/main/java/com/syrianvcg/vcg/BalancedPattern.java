package com.syrianvcg.vcg;

import java.util.LinkedHashMap;
import java.util.Map;

/** Implements VCG's `pattern Balanced "name" { ... }` and `render <Type|"name"> [{ ... }]`
 *  statements: computes a balanced spacing/color scale from a few seed values, and emits
 *  a <style> block (collected via html()) containing CSS variables + utility classes. */
public final class BalancedPattern {
    private BalancedPattern() {}

    private static final Map<String, VcgStruct> REGISTRY = new LinkedHashMap<>();

    public static void declare(Interpreter interp, Node.PatternDecl decl, Environment env) {
        VcgStruct pat = new VcgStruct("Pattern");
        pat.set("type", decl.typeName);
        pat.set("name", decl.patternName);
        // defaults
        pat.set("unit", 8.0);
        pat.set("radius", 14.0);
        pat.set("primary", "#2f6fed");
        pat.set("surface", "#0d1320");
        pat.set("ink", "#eaf2ff");
        pat.set("muted", "#7f93b3");
        pat.set("danger", "#e25b4f");
        for (int i = 0; i < decl.keys.size(); i++) {
            pat.set(decl.keys.get(i), interp.eval(decl.values.get(i), env));
        }
        REGISTRY.put(decl.patternName, pat);
        // `pattern Balanced "name" { ... }` binds the pattern's name to the variable `Type`
        env.define(decl.typeName, decl.patternName);
    }

    public static void render(Interpreter interp, Node.RenderStmt stmt, Environment env) {
        Object targetVal = interp.eval(stmt.target, env);
        String patternName = Interpreter.stringifyStatic(targetVal);
        VcgStruct pat = REGISTRY.get(patternName);
        if (pat == null) {
            throw new Environment.VcgRuntimeError("Pattern '" + patternName + "' غير معرّف / not declared. استخدم pattern Balanced \"" + patternName + "\" { ... } أولاً.");
        }
        String slug = "bp-" + slugify(patternName);
        interp.out.html(buildCss(pat, slug));

        if (stmt.body != null) {
            Environment renderEnv = new Environment(env);
            renderEnv.define("slug", slug);
            interp.execBlockStmts(stmt.body.stmts, renderEnv);
        }
    }

    private static String slugify(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
            else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') sb.append('-');
        }
        String s = sb.toString();
        return s.isEmpty() ? "pattern" : s;
    }

    private static String buildCss(VcgStruct pat, String slug) {
        double unit = Ops.toDouble(pat.get("unit"));
        double radius = Ops.toDouble(pat.get("radius"));
        String primary = Builtins.str(pat.get("primary"));
        String surface = Builtins.str(pat.get("surface"));
        String ink = Builtins.str(pat.get("ink"));
        String muted = Builtins.str(pat.get("muted"));
        String danger = Builtins.str(pat.get("danger"));

        String primaryHover = ColorLib.tone(primary, -12);
        String primarySoft = ColorLib.mixColor(primary, surface, 0.85);
        String surfaceAlt = ColorLib.mixColor(surface, primary, 0.08);
        String border = ColorLib.mixColor(surface, primary, 0.22);

        double xs = unit * 0.5, sm = unit * 1, md = unit * 2, lg = unit * 3, xl = unit * 5;

        StringBuilder css = new StringBuilder();
        css.append("<style>\n");
        css.append(".").append(slug).append(" {\n");
        css.append("  --bp-xs:").append(px(xs)).append("; --bp-sm:").append(px(sm))
           .append("; --bp-md:").append(px(md)).append("; --bp-lg:").append(px(lg))
           .append("; --bp-xl:").append(px(xl)).append(";\n");
        css.append("  --bp-radius:").append(px(radius)).append(";\n");
        css.append("  --bp-primary:").append(primary).append("; --bp-primary-hover:").append(primaryHover)
           .append("; --bp-primary-soft:").append(primarySoft).append(";\n");
        css.append("  --bp-surface:").append(surface).append("; --bp-surface-alt:").append(surfaceAlt)
           .append("; --bp-border:").append(border).append(";\n");
        css.append("  --bp-ink:").append(ink).append("; --bp-muted:").append(muted)
           .append("; --bp-danger:").append(danger).append(";\n");
        css.append("  background:var(--bp-surface); color:var(--bp-ink); font-family:system-ui,Arial,sans-serif;\n");
        css.append("}\n");
        css.append(".").append(slug).append(" .bp-section{background:var(--bp-surface-alt);border:1px solid var(--bp-border);border-radius:var(--bp-radius);padding:var(--bp-lg);margin-bottom:var(--bp-md);}\n");
        css.append(".").append(slug).append(" .bp-card{background:var(--bp-surface-alt);border:1px solid var(--bp-border);border-radius:var(--bp-radius);padding:var(--bp-md);}\n");
        css.append(".").append(slug).append(" .bp-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:var(--bp-md);margin-bottom:var(--bp-md);}\n");
        css.append(".").append(slug).append(" .bp-row{display:flex;justify-content:space-between;align-items:center;padding:var(--bp-sm) 0;border-bottom:1px dashed var(--bp-border);}\n");
        css.append(".").append(slug).append(" .bp-btn{display:inline-block;background:var(--bp-primary);color:#fff;border:none;border-radius:var(--bp-radius);padding:var(--bp-sm) var(--bp-lg);cursor:pointer;}\n");
        css.append(".").append(slug).append(" .bp-btn:hover{background:var(--bp-primary-hover);}\n");
        css.append(".").append(slug).append(" .bp-btn.bp-ghost{background:transparent;border:1px solid var(--bp-primary);color:var(--bp-primary);}\n");
        css.append(".").append(slug).append(" .bp-badge{background:var(--bp-primary-soft);color:var(--bp-primary);border-radius:999px;padding:2px var(--bp-sm);font-size:0.8em;}\n");
        css.append(".").append(slug).append(" .bp-title{font-weight:700;font-size:1.1em;}\n");
        css.append(".").append(slug).append(" .bp-muted{color:var(--bp-muted);font-size:0.9em;}\n");
        css.append("</style>");
        return css.toString();
    }

    private static String px(double v) {
        if (v == Math.floor(v)) return ((long) v) + "px";
        return v + "px";
    }
}
