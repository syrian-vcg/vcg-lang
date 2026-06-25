package com.syrianvcg.vcg;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ColorLib {
    private ColorLib() {}

    static final Map<String, int[]> NAMED = new LinkedHashMap<>();
    static {
        NAMED.put("vcg_olive", new int[]{61, 74, 47});
        NAMED.put("vcg_dark", new int[]{13, 19, 14});
        NAMED.put("vcg_accent", new int[]{77, 166, 90});
        NAMED.put("red", new int[]{220, 53, 69});
        NAMED.put("blue", new int[]{47, 111, 237});
        NAMED.put("green", new int[]{40, 167, 69});
        NAMED.put("white", new int[]{255, 255, 255});
        NAMED.put("black", new int[]{0, 0, 0});
        NAMED.put("gray", new int[]{128, 128, 128});
        NAMED.put("yellow", new int[]{255, 193, 7});
        NAMED.put("orange", new int[]{253, 126, 20});
        NAMED.put("purple", new int[]{111, 66, 193});
    }

    public static void install(Environment g) {
        g.define("color", builtin((i, a) -> colorFn(a)));
        g.define("tone", builtin((i, a) -> tone(Builtins.str(Builtins.arg(a, 0)), Ops.toDouble(Builtins.arg(a, 1)))));
        g.define("mix_color", builtin((i, a) -> mixColor(Builtins.str(Builtins.arg(a, 0)), Builtins.str(Builtins.arg(a, 1)), Ops.toDouble(Builtins.arg(a, 2)))));
        g.define("lighten", builtin((i, a) -> tone(Builtins.str(Builtins.arg(a, 0)), Math.abs(Ops.toDouble(Builtins.arg(a, 1))))));
        g.define("darken", builtin((i, a) -> tone(Builtins.str(Builtins.arg(a, 0)), -Math.abs(Ops.toDouble(Builtins.arg(a, 1))))));
    }

    private static VcgCallable builtin(java.util.function.BiFunction<Interpreter, List<Object>, Object> f) {
        return new VcgCallable() {
            @Override public Object call(Interpreter interp, List<Object> args) { return f.apply(interp, args); }
        };
    }

    static VcgStruct colorFn(List<Object> args) {
        int r, gg, b; double alpha = 1.0;
        if (args.size() >= 3) {
            r = (int) Ops.toDouble(args.get(0));
            gg = (int) Ops.toDouble(args.get(1));
            b = (int) Ops.toDouble(args.get(2));
            if (args.size() >= 4) alpha = Ops.toDouble(args.get(3));
        } else {
            String s = Builtins.str(Builtins.arg(args, 0));
            int[] rgb;
            if (s.startsWith("#")) rgb = hexToRgb(s);
            else if (NAMED.containsKey(s)) rgb = NAMED.get(s);
            else rgb = new int[]{0, 0, 0};
            r = rgb[0]; gg = rgb[1]; b = rgb[2];
        }
        return makeColorStruct(r, gg, b, alpha);
    }

    static VcgStruct makeColorStruct(int r, int g, int b, double a) {
        VcgStruct st = new VcgStruct("Color");
        st.set("r", (double) r);
        st.set("g", (double) g);
        st.set("b", (double) b);
        st.set("a", a);
        st.set("hex", rgbToHex(r, g, b));
        st.set("rgb", "rgb(" + r + "," + g + "," + b + ")");
        st.set("rgba", String.format("rgba(%d,%d,%d,%.2f)", r, g, b, a));
        return st;
    }

    static int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 3) {
            h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        }
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", clamp(r), clamp(g), clamp(b));
    }

    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** tone(hex, percent): lighten if percent > 0, darken if percent < 0. */
    static String tone(String hex, double percent) {
        int[] rgb = resolveColorInput(hex);
        double t = percent / 100.0;
        int r, g, b;
        if (t >= 0) {
            r = (int) (rgb[0] + (255 - rgb[0]) * t);
            g = (int) (rgb[1] + (255 - rgb[1]) * t);
            b = (int) (rgb[2] + (255 - rgb[2]) * t);
        } else {
            double f = 1 + t; // t is negative
            r = (int) (rgb[0] * f);
            g = (int) (rgb[1] * f);
            b = (int) (rgb[2] * f);
        }
        return rgbToHex(clamp(r), clamp(g), clamp(b));
    }

    static String mixColor(String hex1, String hex2, double t) {
        int[] a = resolveColorInput(hex1);
        int[] b = resolveColorInput(hex2);
        int r = (int) (a[0] + (b[0] - a[0]) * t);
        int g = (int) (a[1] + (b[1] - a[1]) * t);
        int bl = (int) (a[2] + (b[2] - a[2]) * t);
        return rgbToHex(clamp(r), clamp(g), clamp(bl));
    }

    static int[] resolveColorInput(String s) {
        if (s.startsWith("#")) return hexToRgb(s);
        if (NAMED.containsKey(s)) return NAMED.get(s);
        return new int[]{0, 0, 0};
    }
}
