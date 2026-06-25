package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BalancedPattern — لغة Style المدمجة في VCG.
 *
 * الصيغة:
 *   pattern Balanced "اسم" {
 *       primary:  "#2f6fed"
 *       surface:  "#0d1320"
 *       ink:      "#eaf2ff"
 *       unit:     8           // وحدة المسافة بالـ px (الافتراضي 8)
 *       radius:   14          // نصف قطر الحواف بالـ px
 *       font:     "Cairo"     // الخط الأساسي
 *       scale:    1.0         // مقياس الخط (1.0 = عادي)
 *       dir:      "rtl"       // اتجاه النص rtl | ltr
 *   }
 *
 *   render اسم {
 *       // داخل render يمكن استخدام دوال المكونات:
 *       page("عنوان الصفحة") {
 *           section("عنوان القسم") {
 *               card {
 *                   h(1, "عنوان")
 *                   p("فقرة نصية")
 *                   row { badge("تسمية") }
 *                   grid(2) { card { p("عمود 1") }  card { p("عمود 2") } }
 *                   btn("زر أساسي")
 *                   btn_ghost("زر شفاف")
 *                   divider()
 *                   list(["بند 1", "بند 2", "بند 3"])
 *                   img("مسار أو url")
 *                   code_block("let x = 42")
 *                   alert("تحذير", "warning")   // info | warning | danger | success
 *                   tag("نص", "#hex")            // وسم ملوّن
 *                   stat("العنوان", "القيمة", "+12%")  // بطاقة إحصائية
 *               }
 *           }
 *       }
 *   }
 *
 *  كل دالة مكوِّن تستدعي interp.out.html(...)  →  VcgOutputRenderer يعرضها.
 */
public final class BalancedPattern {
    private BalancedPattern() {}

    // ── سجل الأنماط المُعرَّفة ─────────────────────────────────────────────
    private static final Map<String, VcgStruct> REGISTRY = new LinkedHashMap<>();

    // ── المراحل ─────────────────────────────────────────────────────────────

    /** يُسجِّل نمطاً جديداً ويُعرِّف اسم النوع كمتغيّر. */
    public static void declare(Interpreter interp, Node.PatternDecl decl, Environment env) {
        VcgStruct pat = defaults();
        pat.set("typeName", decl.typeName);
        pat.set("name", decl.patternName);
        for (int i = 0; i < decl.keys.size(); i++) {
            Object val = interp.eval(decl.values.get(i), env);
            // قيمة Color struct → استخرج hex
            if (val instanceof VcgStruct st && "Color".equals(st.kind)) {
                val = Builtins.str(st.get("hex"));
            }
            pat.set(decl.keys.get(i), val);
        }
        REGISTRY.put(decl.patternName, pat);
        env.define(decl.typeName, decl.patternName);
    }

    /** يُنفِّذ render: يُولِّد CSS + يُعرِّف دوال المكونات + يُشغِّل الجسم. */
    public static void render(Interpreter interp, Node.RenderStmt stmt, Environment env) {
        Object targetVal = interp.eval(stmt.target, env);
        String patternName = Interpreter.stringifyStatic(targetVal);
        VcgStruct pat = REGISTRY.get(patternName);
        if (pat == null) throw new Environment.VcgRuntimeError(
            "Pattern '" + patternName + "' غير معرّف / not declared.");

        String slug = "bp-" + slugify(patternName);
        Palette p = new Palette(pat);

        // 1) أرسل CSS الكامل
        interp.out.html(buildFullCss(p, slug));

        // 2) افتح div الصفحة الجذر
        interp.out.html("<div class='" + slug + " bp-root' dir='" + p.dir + "'>");

        if (stmt.body != null) {
            // 3) عرِّف دوال المكونات في بيئة render
            Environment rEnv = new Environment(env);
            installComponents(interp, rEnv, slug, p);
            interp.execBlockStmts(stmt.body.stmts, rEnv);
        }

        // 4) أغلق div الجذر
        interp.out.html("</div><!-- /" + slug + " -->");
    }

    // ── القيم الافتراضية ──────────────────────────────────────────────────

    private static VcgStruct defaults() {
        VcgStruct s = new VcgStruct("Pattern");
        s.set("primary",  "#2f6fed");
        s.set("secondary","#7c4dff");
        s.set("surface",  "#0d1320");
        s.set("surface2", "");         // محسوب تلقائياً
        s.set("ink",      "#eaf2ff");
        s.set("muted",    "#7f93b3");
        s.set("danger",   "#e25b4f");
        s.set("warning",  "#e0a84d");
        s.set("success",  "#4dc95a");
        s.set("info",     "#38b2f0");
        s.set("unit",     8.0);
        s.set("radius",   14.0);
        s.set("font",     "Cairo, system-ui, sans-serif");
        s.set("mono",     "JetBrains Mono, monospace");
        s.set("scale",    1.0);
        s.set("dir",      "rtl");
        return s;
    }

    // ── Palette: حساب جميع الألوان المشتقة ──────────────────────────────

    static final class Palette {
        final String primary, secondary, surface, surface2, border,
                     ink, muted, danger, warning, success, info,
                     primaryHover, primarySoft, secondaryHover,
                     inkDim, surfaceCard;
        final double unit, radius, scale;
        final String font, mono, dir;
        // مقاسات مشتقة
        final String xs, sm, md, lg, xl, xxl;
        final double fs, fsLg, fsXl, fsXxl, fsSm, fsXs;

        Palette(VcgStruct pat) {
            primary   = str(pat, "primary",   "#2f6fed");
            secondary = str(pat, "secondary", "#7c4dff");
            surface   = str(pat, "surface",   "#0d1320");
            ink       = str(pat, "ink",       "#eaf2ff");
            muted     = str(pat, "muted",     "#7f93b3");
            danger    = str(pat, "danger",    "#e25b4f");
            warning   = str(pat, "warning",   "#e0a84d");
            success   = str(pat, "success",   "#4dc95a");
            info      = str(pat, "info",      "#38b2f0");
            font      = str(pat, "font",      "Cairo, system-ui, sans-serif");
            mono      = str(pat, "mono",      "JetBrains Mono, monospace");
            dir       = str(pat, "dir",       "rtl");
            unit      = dbl(pat, "unit",      8.0);
            radius    = dbl(pat, "radius",    14.0);
            scale     = dbl(pat, "scale",     1.0);

            // ألوان مشتقة
            primaryHover   = ColorLib.tone(primary,   -12);
            primarySoft    = ColorLib.mixColor(primary, surface, 0.82);
            secondaryHover = ColorLib.tone(secondary, -12);
            surface2       = ColorLib.mixColor(surface, primary, 0.07);
            surfaceCard    = ColorLib.mixColor(surface, primary, 0.10);
            border         = ColorLib.mixColor(surface, primary, 0.22);
            inkDim         = ColorLib.mixColor(ink, surface, 0.30);

            // مسافات
            xs  = px(unit * 0.5);
            sm  = px(unit);
            md  = px(unit * 2);
            lg  = px(unit * 3);
            xl  = px(unit * 5);
            xxl = px(unit * 8);

            // أحجام الخط
            fs    = 1.0  * scale;
            fsSm  = 0.875 * scale;
            fsXs  = 0.78 * scale;
            fsLg  = 1.15 * scale;
            fsXl  = 1.35 * scale;
            fsXxl = 1.75 * scale;
        }

        private static String str(VcgStruct p, String k, String def) {
            Object v = p.get(k);
            return (v == null || v.toString().isEmpty()) ? def : v.toString();
        }
        private static double dbl(VcgStruct p, String k, double def) {
            Object v = p.get(k);
            return v == null ? def : Ops.toDouble(v);
        }
        private static String px(double v) {
            return (v == Math.floor(v)) ? ((long)v) + "px" : v + "px";
        }
    }

    // ── CSS الكامل ──────────────────────────────────────────────────────────

    private static String buildFullCss(Palette p, String slug) {
        String r = "var(--bp-radius)";
        StringBuilder css = new StringBuilder();
        css.append("<style>\n");

        // ── CSS Variables ──
        css.append(".").append(slug).append(" {\n");
        css.append("  --bp-primary:").append(p.primary).append(";\n");
        css.append("  --bp-primary-h:").append(p.primaryHover).append(";\n");
        css.append("  --bp-primary-soft:").append(p.primarySoft).append(";\n");
        css.append("  --bp-secondary:").append(p.secondary).append(";\n");
        css.append("  --bp-secondary-h:").append(p.secondaryHover).append(";\n");
        css.append("  --bp-surface:").append(p.surface).append(";\n");
        css.append("  --bp-surface2:").append(p.surface2).append(";\n");
        css.append("  --bp-surface-card:").append(p.surfaceCard).append(";\n");
        css.append("  --bp-border:").append(p.border).append(";\n");
        css.append("  --bp-ink:").append(p.ink).append(";\n");
        css.append("  --bp-ink-dim:").append(p.inkDim).append(";\n");
        css.append("  --bp-muted:").append(p.muted).append(";\n");
        css.append("  --bp-danger:").append(p.danger).append(";\n");
        css.append("  --bp-warning:").append(p.warning).append(";\n");
        css.append("  --bp-success:").append(p.success).append(";\n");
        css.append("  --bp-info:").append(p.info).append(";\n");
        css.append("  --bp-xs:").append(p.xs).append(";  --bp-sm:").append(p.sm).append(";\n");
        css.append("  --bp-md:").append(p.md).append(";  --bp-lg:").append(p.lg).append(";\n");
        css.append("  --bp-xl:").append(p.xl).append(";  --bp-xxl:").append(p.xxl).append(";\n");
        css.append("  --bp-radius:").append(px(p.radius)).append(";\n");
        css.append("  --bp-font:").append(p.font).append(";\n");
        css.append("  --bp-mono:").append(p.mono).append(";\n");
        css.append("  --bp-fs:").append(p.fs).append("rem;\n");
        css.append("  --bp-fs-sm:").append(p.fsSm).append("rem;\n");
        css.append("  --bp-fs-xs:").append(p.fsXs).append("rem;\n");
        css.append("  --bp-fs-lg:").append(p.fsLg).append("rem;\n");
        css.append("  --bp-fs-xl:").append(p.fsXl).append("rem;\n");
        css.append("  --bp-fs-xxl:").append(p.fsXxl).append("rem;\n");
        css.append("}\n");

        // ── Base ──
        css.append(".bp-root{background:var(--bp-surface);color:var(--bp-ink);");
        css.append("font-family:var(--bp-font);font-size:var(--bp-fs);line-height:1.7;");
        css.append("min-height:100vh;padding:var(--bp-md);box-sizing:border-box}\n");

        // ── Page / Section / Card ──
        css.append(".bp-page{max-width:900px;margin:0 auto}\n");
        css.append(".bp-page-title{font-size:var(--bp-fs-xxl);font-weight:800;");
        css.append("color:var(--bp-ink);margin-bottom:var(--bp-md);line-height:1.2}\n");
        css.append(".bp-section{background:var(--bp-surface2);border:1px solid var(--bp-border);");
        css.append("border-radius:").append(r).append(";padding:var(--bp-lg);margin-bottom:var(--bp-md)}\n");
        css.append(".bp-section-title{font-size:var(--bp-fs-lg);font-weight:700;");
        css.append("color:var(--bp-primary);margin-bottom:var(--bp-sm);");
        css.append("padding-bottom:var(--bp-xs);border-bottom:2px solid var(--bp-border)}\n");
        css.append(".bp-card{background:var(--bp-surface-card);border:1px solid var(--bp-border);");
        css.append("border-radius:").append(r).append(";padding:var(--bp-md);margin-bottom:var(--bp-sm)}\n");

        // ── Typography ──
        css.append(".bp-h1{font-size:var(--bp-fs-xxl);font-weight:800;color:var(--bp-ink);margin:var(--bp-sm) 0}\n");
        css.append(".bp-h2{font-size:var(--bp-fs-xl);font-weight:700;color:var(--bp-ink);margin:var(--bp-sm) 0}\n");
        css.append(".bp-h3{font-size:var(--bp-fs-lg);font-weight:700;color:var(--bp-primary);margin:var(--bp-xs) 0}\n");
        css.append(".bp-h4,.bp-h5,.bp-h6{font-size:var(--bp-fs);font-weight:600;color:var(--bp-ink-dim);margin:var(--bp-xs) 0}\n");
        css.append(".bp-p{color:var(--bp-ink-dim);margin:var(--bp-xs) 0;line-height:1.8}\n");
        css.append(".bp-muted{color:var(--bp-muted);font-size:var(--bp-fs-sm)}\n");
        css.append(".bp-bold{font-weight:700}\n");
        css.append(".bp-mono{font-family:var(--bp-mono);font-size:var(--bp-fs-sm)}\n");

        // ── Buttons ──
        css.append(".bp-btn{display:inline-flex;align-items:center;gap:var(--bp-xs);");
        css.append("background:var(--bp-primary);color:#fff;border:none;");
        css.append("border-radius:").append(r).append(";padding:var(--bp-sm) var(--bp-lg);");
        css.append("font-size:var(--bp-fs-sm);font-weight:700;cursor:pointer;");
        css.append("font-family:var(--bp-font);transition:background 0.18s,transform 0.1s;margin:var(--bp-xs)}\n");
        css.append(".bp-btn:hover{background:var(--bp-primary-h);transform:translateY(-1px)}\n");
        css.append(".bp-btn-ghost{background:transparent;border:1.5px solid var(--bp-primary);color:var(--bp-primary)}\n");
        css.append(".bp-btn-ghost:hover{background:var(--bp-primary-soft)}\n");
        css.append(".bp-btn-secondary{background:var(--bp-secondary)}\n");
        css.append(".bp-btn-secondary:hover{background:var(--bp-secondary-h)}\n");
        css.append(".bp-btn-danger{background:var(--bp-danger)}\n");
        css.append(".bp-btn-sm{padding:var(--bp-xs) var(--bp-sm);font-size:var(--bp-fs-xs)}\n");
        css.append(".bp-btn-lg{padding:var(--bp-md) var(--bp-xl);font-size:var(--bp-fs-lg)}\n");

        // ── Badge / Tag ──
        css.append(".bp-badge{display:inline-block;background:var(--bp-primary-soft);");
        css.append("color:var(--bp-primary);border-radius:999px;");
        css.append("padding:2px var(--bp-sm);font-size:var(--bp-fs-xs);font-weight:600;margin:2px}\n");
        css.append(".bp-badge-success{background:rgba(77,201,90,0.15);color:var(--bp-success)}\n");
        css.append(".bp-badge-danger{background:rgba(226,91,79,0.15);color:var(--bp-danger)}\n");
        css.append(".bp-badge-warning{background:rgba(224,168,77,0.15);color:var(--bp-warning)}\n");
        css.append(".bp-badge-info{background:rgba(56,178,240,0.15);color:var(--bp-info)}\n");
        css.append(".bp-tag{display:inline-block;border-radius:5px;");
        css.append("padding:2px var(--bp-xs);font-size:var(--bp-fs-xs);font-weight:600;margin:2px}\n");

        // ── Layout ──
        css.append(".bp-row{display:flex;align-items:center;gap:var(--bp-sm);");
        css.append("flex-wrap:wrap;padding:var(--bp-xs) 0}\n");
        css.append(".bp-row-between{justify-content:space-between}\n");
        css.append(".bp-col{display:flex;flex-direction:column;gap:var(--bp-xs)}\n");
        css.append(".bp-grid{display:grid;gap:var(--bp-md);margin:var(--bp-sm) 0}\n");
        css.append(".bp-divider{border:none;border-top:1px solid var(--bp-border);margin:var(--bp-md) 0}\n");
        css.append(".bp-spacer{height:var(--bp-md)}\n");

        // ── List ──
        css.append(".bp-list{list-style:none;padding:0;margin:var(--bp-xs) 0}\n");
        css.append(".bp-list-item{padding:var(--bp-sm);border-bottom:1px solid var(--bp-border);");
        css.append("color:var(--bp-ink-dim);display:flex;align-items:center;gap:var(--bp-sm)}\n");
        css.append(".bp-list-item:last-child{border-bottom:none}\n");
        css.append(".bp-list-item::before{content:'▸';color:var(--bp-primary);font-size:0.8em}\n");

        // ── Alert ──
        css.append(".bp-alert{border-radius:").append(r).append(";padding:var(--bp-sm) var(--bp-md);");
        css.append("margin:var(--bp-xs) 0;font-weight:600;display:flex;align-items:flex-start;gap:var(--bp-sm)}\n");
        css.append(".bp-alert-info{background:rgba(56,178,240,0.12);color:var(--bp-info);border:1px solid rgba(56,178,240,0.3)}\n");
        css.append(".bp-alert-success{background:rgba(77,201,90,0.12);color:var(--bp-success);border:1px solid rgba(77,201,90,0.3)}\n");
        css.append(".bp-alert-warning{background:rgba(224,168,77,0.12);color:var(--bp-warning);border:1px solid rgba(224,168,77,0.3)}\n");
        css.append(".bp-alert-danger{background:rgba(226,91,79,0.12);color:var(--bp-danger);border:1px solid rgba(226,91,79,0.3)}\n");

        // ── Stat card ──
        css.append(".bp-stat{background:var(--bp-surface-card);border:1px solid var(--bp-border);");
        css.append("border-radius:").append(r).append(";padding:var(--bp-md);text-align:center}\n");
        css.append(".bp-stat-value{font-size:var(--bp-fs-xxl);font-weight:800;color:var(--bp-primary);line-height:1}\n");
        css.append(".bp-stat-label{font-size:var(--bp-fs-sm);color:var(--bp-muted);margin-top:var(--bp-xs)}\n");
        css.append(".bp-stat-change{font-size:var(--bp-fs-xs);font-weight:600;margin-top:4px}\n");
        css.append(".bp-stat-change.up{color:var(--bp-success)}\n");
        css.append(".bp-stat-change.down{color:var(--bp-danger)}\n");

        // ── Code block ──
        css.append(".bp-code{background:rgba(0,0,0,0.35);border:1px solid var(--bp-border);");
        css.append("border-radius:8px;padding:var(--bp-md);font-family:var(--bp-mono);");
        css.append("font-size:var(--bp-fs-sm);color:var(--bp-success);overflow-x:auto;");
        css.append("white-space:pre;margin:var(--bp-sm) 0}\n");

        // ── Image ──
        css.append(".bp-img{border-radius:").append(r).append(";max-width:100%;");
        css.append("display:block;margin:var(--bp-sm) 0;border:1px solid var(--bp-border)}\n");

        // ── Input ──
        css.append(".bp-input{background:var(--bp-surface2);border:1.5px solid var(--bp-border);");
        css.append("border-radius:").append(r).append(";color:var(--bp-ink);padding:var(--bp-sm) var(--bp-md);");
        css.append("font-family:var(--bp-font);font-size:var(--bp-fs);width:100%;box-sizing:border-box;");
        css.append("outline:none;transition:border 0.15s}\n");
        css.append(".bp-input:focus{border-color:var(--bp-primary)}\n");

        // ── Progress ──
        css.append(".bp-progress-wrap{background:var(--bp-surface2);border-radius:999px;");
        css.append("height:8px;overflow:hidden;margin:var(--bp-xs) 0}\n");
        css.append(".bp-progress-bar{height:100%;background:var(--bp-primary);border-radius:999px;transition:width 0.4s}\n");

        // ── Avatar ──
        css.append(".bp-avatar{width:40px;height:40px;border-radius:50%;background:var(--bp-primary-soft);");
        css.append("color:var(--bp-primary);display:inline-flex;align-items:center;justify-content:center;");
        css.append("font-weight:800;font-size:var(--bp-fs-sm);flex-shrink:0}\n");

        // ── Tooltip (via title attr hover) ──
        css.append(".bp-tooltip{position:relative;cursor:help}\n");
        css.append(".bp-tooltip:hover::after{content:attr(data-tip);position:absolute;");
        css.append("bottom:110%;left:50%;transform:translateX(-50%);background:#222;color:#fff;");
        css.append("font-size:var(--bp-fs-xs);padding:4px 10px;border-radius:6px;white-space:nowrap;z-index:10}\n");

        css.append("</style>\n");
        return css.toString();
    }

    // ── دوال المكونات المُعرَّفة داخل بيئة render ───────────────────────────

    private static void installComponents(Interpreter interp,
                                          Environment env,
                                          String slug,
                                          Palette p) {
        // ── page(title) { ... } ──────────────────────────────────────────
        env.define("page", callable((i2, args) -> {
            String title = args.isEmpty() ? "" : Builtins.str(args.get(0));
            interp.out.html("<div class='bp-page'>");
            if (!title.isEmpty())
                interp.out.html("<div class='bp-page-title'>" + esc(title) + "</div>");
            return null;
        }));

        // ── section(title?) { ... } ──────────────────────────────────────
        env.define("section", callable((i2, args) -> {
            String title = args.isEmpty() ? "" : Builtins.str(args.get(0));
            interp.out.html("<div class='bp-section'>");
            if (!title.isEmpty())
                interp.out.html("<div class='bp-section-title'>" + esc(title) + "</div>");
            return null;
        }));

        // ── card { ... } ─────────────────────────────────────────────────
        env.define("card", callable((i2, args) -> {
            interp.out.html("<div class='bp-card'>");
            return null;
        }));

        // ── end() — يُغلق آخر div مفتوح ─────────────────────────────────
        env.define("end", callable((i2, args) -> {
            interp.out.html("</div>");
            return null;
        }));

        // ── h(level, text) ────────────────────────────────────────────────
        env.define("h", callable((i2, args) -> {
            int level = args.isEmpty() ? 2 : (int) Ops.toDouble(args.get(0));
            level = Math.max(1, Math.min(6, level));
            String text = args.size() > 1 ? Builtins.str(args.get(1)) : "";
            interp.out.html("<div class='bp-h" + level + "'>" + esc(text) + "</div>");
            return null;
        }));

        // ── p(text) ──────────────────────────────────────────────────────
        env.define("p", callable((i2, args) -> {
            String text = args.isEmpty() ? "" : Builtins.str(args.get(0));
            interp.out.html("<div class='bp-p'>" + esc(text) + "</div>");
            return null;
        }));

        // ── muted(text) ──────────────────────────────────────────────────
        env.define("muted", callable((i2, args) -> {
            String text = args.isEmpty() ? "" : Builtins.str(args.get(0));
            interp.out.html("<div class='bp-muted'>" + esc(text) + "</div>");
            return null;
        }));

        // ── btn(label, variant?) → "primary" | "ghost" | "secondary" | "danger" | "sm" | "lg"
        env.define("btn", callable((i2, args) -> {
            String label   = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String variant = args.size() > 1 ? Builtins.str(args.get(1)) : "primary";
            String cls = "bp-btn";
            if ("ghost".equals(variant))     cls += " bp-btn-ghost";
            else if ("secondary".equals(variant)) cls += " bp-btn-secondary";
            else if ("danger".equals(variant)) cls += " bp-btn-danger";
            else if ("sm".equals(variant))   cls += " bp-btn-sm";
            else if ("lg".equals(variant))   cls += " bp-btn-lg";
            interp.out.html("<button class='" + cls + "'>" + esc(label) + "</button>");
            return null;
        }));

        // ── btn_ghost(label) ──────────────────────────────────────────────
        env.define("btn_ghost", callable((i2, args) -> {
            String label = args.isEmpty() ? "" : Builtins.str(args.get(0));
            interp.out.html("<button class='bp-btn bp-btn-ghost'>" + esc(label) + "</button>");
            return null;
        }));

        // ── badge(text, variant?) → "primary"|"success"|"danger"|"warning"|"info"
        env.define("badge", callable((i2, args) -> {
            String text    = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String variant = args.size() > 1 ? Builtins.str(args.get(1)) : "primary";
            String cls = "bp-badge";
            if (!"primary".equals(variant)) cls += " bp-badge-" + variant;
            interp.out.html("<span class='" + cls + "'>" + esc(text) + "</span>");
            return null;
        }));

        // ── tag(text, color?) ─────────────────────────────────────────────
        env.define("tag", callable((i2, args) -> {
            String text  = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String color = args.size() > 1 ? Builtins.str(args.get(1)) : p.primary;
            String bg    = ColorLib.mixColor(color, p.surface, 0.82);
            interp.out.html("<span class='bp-tag' style='background:" + bg +
                ";color:" + color + "'>" + esc(text) + "</span>");
            return null;
        }));

        // ── row { ... } ──────────────────────────────────────────────────
        env.define("row", callable((i2, args) -> {
            String variant = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String cls = "bp-row" + ("between".equals(variant) ? " bp-row-between" : "");
            interp.out.html("<div class='" + cls + "'>");
            return null;
        }));

        // ── col { ... } ──────────────────────────────────────────────────
        env.define("col", callable((i2, args) -> {
            interp.out.html("<div class='bp-col'>");
            return null;
        }));

        // ── grid(cols) { ... } ────────────────────────────────────────────
        env.define("grid", callable((i2, args) -> {
            int cols = args.isEmpty() ? 2 : (int) Ops.toDouble(args.get(0));
            interp.out.html("<div class='bp-grid' style='grid-template-columns:repeat(" +
                cols + ",1fr)'>");
            return null;
        }));

        // ── divider() ─────────────────────────────────────────────────────
        env.define("divider", callable((i2, args) -> {
            interp.out.html("<hr class='bp-divider'>");
            return null;
        }));

        // ── spacer() ──────────────────────────────────────────────────────
        env.define("spacer", callable((i2, args) -> {
            interp.out.html("<div class='bp-spacer'></div>");
            return null;
        }));

        // ── list(items) → List<Object> أو نص مفصول بفواصل ───────────────
        env.define("list", callable((i2, args) -> {
            if (args.isEmpty()) return null;
            Object first = args.get(0);
            List<Object> items;
            if (first instanceof List<?> lst) {
                items = (List<Object>) lst;
            } else {
                items = new ArrayList<>();
                for (Object a : args) items.add(a);
            }
            StringBuilder html = new StringBuilder("<ul class='bp-list'>");
            for (Object item : items)
                html.append("<li class='bp-list-item'>").append(esc(Builtins.str(item))).append("</li>");
            html.append("</ul>");
            interp.out.html(html.toString());
            return null;
        }));

        // ── alert(text, type?) → "info"|"success"|"warning"|"danger" ─────
        env.define("alert", callable((i2, args) -> {
            String text = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String type = args.size() > 1 ? Builtins.str(args.get(1)) : "info";
            String icon = switch (type) {
                case "success" -> "✓";
                case "warning" -> "⚠";
                case "danger"  -> "✗";
                default        -> "ℹ";
            };
            interp.out.html("<div class='bp-alert bp-alert-" + type + "'>" +
                "<span>" + icon + "</span><span>" + esc(text) + "</span></div>");
            return null;
        }));

        // ── stat(label, value, change?) ───────────────────────────────────
        env.define("stat", callable((i2, args) -> {
            String label  = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String value  = args.size() > 1 ? Builtins.str(args.get(1)) : "";
            String change = args.size() > 2 ? Builtins.str(args.get(2)) : "";
            String changeHtml = "";
            if (!change.isEmpty()) {
                boolean up = change.startsWith("+");
                changeHtml = "<div class='bp-stat-change " + (up ? "up" : "down") + "'>" +
                    esc(change) + "</div>";
            }
            interp.out.html("<div class='bp-stat'>" +
                "<div class='bp-stat-value'>" + esc(value) + "</div>" +
                "<div class='bp-stat-label'>" + esc(label) + "</div>" +
                changeHtml + "</div>");
            return null;
        }));

        // ── code_block(code) ─────────────────────────────────────────────
        env.define("code_block", callable((i2, args) -> {
            String code = args.isEmpty() ? "" : Builtins.str(args.get(0));
            interp.out.html("<pre class='bp-code'>" + esc(code) + "</pre>");
            return null;
        }));

        // ── img(src, alt?) ────────────────────────────────────────────────
        env.define("img", callable((i2, args) -> {
            String src = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String alt = args.size() > 1 ? Builtins.str(args.get(1)) : "";
            interp.out.html("<img class='bp-img' src='" + src + "' alt='" + esc(alt) + "'>");
            return null;
        }));

        // ── input(placeholder?, type?) ────────────────────────────────────
        env.define("input", callable((i2, args) -> {
            String ph   = args.isEmpty() ? "" : Builtins.str(args.get(0));
            String type = args.size() > 1 ? Builtins.str(args.get(1)) : "text";
            interp.out.html("<input class='bp-input' type='" + type +
                "' placeholder='" + esc(ph) + "'>");
            return null;
        }));

        // ── avatar(initials?, color?) ─────────────────────────────────────
        env.define("avatar", callable((i2, args) -> {
            String initials = args.isEmpty() ? "?" : Builtins.str(args.get(0));
            String color    = args.size() > 1 ? Builtins.str(args.get(1)) : p.primary;
            String bg       = ColorLib.mixColor(color, p.surface, 0.82);
            interp.out.html("<div class='bp-avatar' style='background:" + bg +
                ";color:" + color + "'>" + esc(initials) + "</div>");
            return null;
        }));

        // ── progress(percent, color?) ─────────────────────────────────────
        env.define("progress", callable((i2, args) -> {
            double pct   = args.isEmpty() ? 0 : Ops.toDouble(args.get(0));
            String color = args.size() > 1 ? Builtins.str(args.get(1)) : p.primary;
            pct = Math.max(0, Math.min(100, pct));
            interp.out.html("<div class='bp-progress-wrap'>" +
                "<div class='bp-progress-bar' style='width:" + (int)pct + "%;background:" + color + "'></div>" +
                "</div>");
            return null;
        }));

        // ── raw_html(html) — HTML خام لمن يحتاج تحكم كامل ───────────────
        env.define("raw_html", callable((i2, args) -> {
            if (!args.isEmpty()) interp.out.html(Builtins.str(args.get(0)));
            return null;
        }));

        // ── slug متاح كمتغيّر داخل render ────────────────────────────────
        env.define("slug", slug);
    }

    // ── مساعدات ─────────────────────────────────────────────────────────────

    private static VcgCallable callable(
            java.util.function.BiFunction<Interpreter, List<Object>, Object> fn) {
        return new VcgCallable() {
            @Override public Object call(Interpreter i, List<Object> a) { return fn.apply(i, a); }
        };
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private static String px(double v) {
        return (v == Math.floor(v)) ? ((long)v) + "px" : v + "px";
    }

    private static String slugify(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
            else if (sb.length() > 0 && sb.charAt(sb.length()-1) != '-') sb.append('-');
        }
        String s = sb.toString();
        return s.isEmpty() ? "pattern" : s;
    }
}
