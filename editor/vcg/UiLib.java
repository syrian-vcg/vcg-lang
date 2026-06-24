package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.List;

public final class UiLib {
    private UiLib() {}

    public static void install(Environment g) {
        g.define("text", builtin((i, a) -> {
            VcgStruct st = new VcgStruct("Text");
            st.set("content", Builtins.str(Builtins.arg(a, 0)));
            st.set("style", a.size() > 1 ? a.get(1) : new VcgStruct("Style"));
            return st;
        }));
        g.define("text_s", builtin((i, a) -> {
            VcgStruct st = new VcgStruct("Text");
            st.set("content", Builtins.str(Builtins.arg(a, 0)));
            st.set("style", a.size() > 1 ? a.get(1) : new VcgStruct("Style"));
            return st;
        }));
        g.define("btn", builtin((i, a) -> {
            VcgStruct st = new VcgStruct("Button");
            st.set("label", Builtins.str(Builtins.arg(a, 0)));
            st.set("onclick", a.size() > 1 ? Builtins.str(a.get(1)) : null);
            st.set("style", a.size() > 2 ? a.get(2) : new VcgStruct("Style"));
            return st;
        }));
        g.define("style", builtin((i, a) -> {
            VcgStruct st;
            if (!a.isEmpty() && a.get(0) instanceof VcgStruct existing) {
                st = existing.copy();
                st.kind = "Style";
            } else {
                st = new VcgStruct("Style");
            }
            return st;
        }));
        g.define("design", builtin((i, a) -> {
            VcgStruct st;
            if (!a.isEmpty() && a.get(0) instanceof VcgStruct existing) {
                st = existing.copy();
                st.kind = "Design";
            } else {
                st = new VcgStruct("Design");
            }
            return st;
        }));
        g.define("ui", builtin((i, a) -> {
            VcgStruct st = new VcgStruct("UI");
            List<Object> children = new ArrayList<>(a);
            st.set("children", children);
            return st;
        }));
        g.define("settings_new", builtin((i, a) -> SettingsLib.newSettings()));
    }

    private static VcgCallable builtin(java.util.function.BiFunction<Interpreter, List<Object>, Object> f) {
        return new VcgCallable() {
            @Override public Object call(Interpreter interp, List<Object> args) { return f.apply(interp, args); }
        };
    }

    /** Implements the chained `.color(...)` method available on text()/text_s()/btn() structs.
     *  Updates style.bg (Button) or style.color (Text), returns the struct itself for chaining. */
    static Object applyColorMethod(VcgStruct target, List<Object> args) {
        VcgStruct colorResult;
        if (args.size() >= 2) {
            int r = (int) Ops.toDouble(args.get(0));
            int gg = (int) Ops.toDouble(args.get(1));
            int b = args.size() > 2 ? (int) Ops.toDouble(args.get(2)) : 0;
            double al = args.size() > 3 ? Ops.toDouble(args.get(3)) : 1.0;
            colorResult = ColorLib.makeColorStruct(r, gg, b, al);
        } else {
            String spec = Builtins.str(Builtins.arg(args, 0));
            int[] rgb = spec.startsWith("#") ? ColorLib.hexToRgb(spec)
                    : ColorLib.NAMED.getOrDefault(spec, new int[]{0, 0, 0});
            colorResult = ColorLib.makeColorStruct(rgb[0], rgb[1], rgb[2], 1.0);
        }
        Object styleObj = target.get("style");
        VcgStruct style = (styleObj instanceof VcgStruct s) ? s : new VcgStruct("Style");
        if ("Button".equals(target.kind)) {
            style.set("bg", colorResult.get("hex"));
        } else {
            style.set("color", colorResult.get("hex"));
        }
        target.set("style", style);
        return colorResult;
    }
}
