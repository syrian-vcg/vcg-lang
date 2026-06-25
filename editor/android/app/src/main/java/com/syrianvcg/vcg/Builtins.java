package com.syrianvcg.vcg;

import java.util.*;
import java.util.Base64;

public final class Builtins {

    /** Sentinel returned by tryReadProp when no such property/method exists. */
    public static final Object NO_METHOD = new Object();

    private Builtins() {}

    public static void install(Interpreter interp, Environment g) {
        g.define("VCG_VERSION", "2.1.0");
        g.define("VCG_EDITION", "Java Native Edition");
        g.define("PI", Math.PI);
        // Predefined convenience global so `print(hi)` (no quotes) works exactly as written.
        g.define("hi", "hi");

        reg(g, "typeof", (i, a) -> Ops.typeOf(arg(a, 0)));
        reg(g, "type_of", (i, a) -> Ops.typeOf(arg(a, 0)));
        reg(g, "kind", (i, a) -> {
            Object v = arg(a, 0);
            if (v instanceof VcgStruct st) return st.kind;
            return Ops.typeOf(v);
        });
        reg(g, "len", (i, a) -> (double) lengthOf(arg(a, 0)));
        reg(g, "sizeof", (i, a) -> (double) lengthOf(arg(a, 0)));

        // ---- text processing ----
        reg(g, "split", (i, a) -> {
            String s = str(arg(a, 0)); String sep = str(arg(a, 1));
            List<Object> out = new ArrayList<>();
            for (String part : sep.isEmpty() ? s.split("") : s.split(java.util.regex.Pattern.quote(sep)))
                out.add(part);
            return out;
        });
        reg(g, "replace", (i, a) -> str(arg(a, 0)).replace(str(arg(a, 1)), str(arg(a, 2))));
        reg(g, "trim", (i, a) -> str(arg(a, 0)).trim());
        reg(g, "upper", (i, a) -> str(arg(a, 0)).toUpperCase());
        reg(g, "lower", (i, a) -> str(arg(a, 0)).toLowerCase());
        reg(g, "starts_with", (i, a) -> str(arg(a, 0)).startsWith(str(arg(a, 1))));
        reg(g, "ends_with", (i, a) -> str(arg(a, 0)).endsWith(str(arg(a, 1))));
        reg(g, "contains", (i, a) -> {
            Object o = arg(a, 0);
            if (o instanceof String s) return s.contains(str(arg(a, 1)));
            if (o instanceof List<?> l) { for (Object e : l) if (Ops.equals(e, arg(a, 1))) return true; return false; }
            return false;
        });
        reg(g, "join", (i, a) -> {
            List<?> list = (List<?>) arg(a, 0);
            String sep = a.size() > 1 ? str(arg(a, 1)) : "";
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < list.size(); k++) { if (k > 0) sb.append(sep); sb.append(Interpreter.stringifyStatic(list.get(k))); }
            return sb.toString();
        });

        // ---- arrays ----
        reg(g, "sort", (i, a) -> {
            List<Object> list = asList(arg(a, 0));
            if (a.size() > 1 && a.get(1) instanceof VcgCallable cmp) {
                list.sort((x, y) -> (int) Math.signum(Ops.toDouble(cmp.call(i, List.of(x, y)))));
            } else {
                list.sort((x, y) -> Ops.compare(x, y));
            }
            return list;
        });
        reg(g, "reverse", (i, a) -> {
            Object o = arg(a, 0);
            if (o instanceof String s) return new StringBuilder(s).reverse().toString();
            List<Object> list = asList(o);
            Collections.reverse(list);
            return list;
        });
        reg(g, "push", (i, a) -> { List<Object> l = asList(arg(a, 0)); l.add(arg(a, 1)); return l; });
        reg(g, "pop", (i, a) -> { List<Object> l = asList(arg(a, 0)); return l.isEmpty() ? null : l.remove(l.size() - 1); });
        reg(g, "shift", (i, a) -> { List<Object> l = asList(arg(a, 0)); return l.isEmpty() ? null : l.remove(0); });
        reg(g, "unshift", (i, a) -> { List<Object> l = asList(arg(a, 0)); l.add(0, arg(a, 1)); return l; });
        reg(g, "slice", (i, a) -> {
            Object o = arg(a, 0);
            int start = (int) Ops.toDouble(arg(a, 1));
            if (o instanceof String s) {
                int end = a.size() > 2 ? (int) Ops.toDouble(arg(a, 2)) : s.length();
                start = clampIdx(start, s.length()); end = clampIdx(end, s.length());
                if (end < start) end = start;
                return s.substring(start, end);
            }
            List<Object> list = asList(o);
            int end = a.size() > 2 ? (int) Ops.toDouble(arg(a, 2)) : list.size();
            start = clampIdx(start, list.size()); end = clampIdx(end, list.size());
            if (end < start) end = start;
            return new ArrayList<>(list.subList(start, end));
        });
        reg(g, "map", (i, a) -> {
            List<Object> list = asList(arg(a, 0)); VcgCallable fn = (VcgCallable) arg(a, 1);
            List<Object> out = new ArrayList<>();
            for (Object e : list) out.add(fn.call(i, List.of(e)));
            return out;
        });
        reg(g, "filter", (i, a) -> {
            List<Object> list = asList(arg(a, 0)); VcgCallable fn = (VcgCallable) arg(a, 1);
            List<Object> out = new ArrayList<>();
            for (Object e : list) if (Ops.truthy(fn.call(i, List.of(e)))) out.add(e);
            return out;
        });
        reg(g, "reduce", (i, a) -> {
            VcgCallable fn = (VcgCallable) arg(a, 0);
            Object acc = arg(a, 1);
            List<Object> list = asList(arg(a, 2));
            for (Object e : list) acc = fn.call(i, List.of(acc, e));
            return acc;
        });

        // ---- base64 ----
        reg(g, "base64_encode", (i, a) -> Base64.getEncoder().encodeToString(str(arg(a, 0)).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        reg(g, "base64_decode", (i, a) -> new String(Base64.getDecoder().decode(str(arg(a, 0))), java.nio.charset.StandardCharsets.UTF_8));

        // ---- math ----
        reg(g, "abs", (i, a) -> Math.abs(Ops.toDouble(arg(a, 0))));
        reg(g, "floor", (i, a) -> Math.floor(Ops.toDouble(arg(a, 0))));
        reg(g, "ceil", (i, a) -> Math.ceil(Ops.toDouble(arg(a, 0))));
        reg(g, "round", (i, a) -> (double) Math.round(Ops.toDouble(arg(a, 0))));
        reg(g, "sqrt", (i, a) -> Math.sqrt(Ops.toDouble(arg(a, 0))));
        reg(g, "min", (i, a) -> a.stream().mapToDouble(Ops::toDouble).min().orElse(0));
        reg(g, "max", (i, a) -> a.stream().mapToDouble(Ops::toDouble).max().orElse(0));
        reg(g, "random", (i, a) -> Math.random());

        // ---- control / assertions ----
        reg(g, "assert", (i, a) -> {
            if (!Ops.truthy(arg(a, 0))) {
                String msg = a.size() > 1 ? str(arg(a, 1)) : "Assertion failed";
                throw new Environment.VcgRuntimeError(msg);
            }
            return null;
        });
        reg(g, "assert_eq", (i, a) -> {
            if (!Ops.equals(arg(a, 0), arg(a, 1)))
                throw new Environment.VcgRuntimeError("assert_eq failed: " + Interpreter.stringifyStatic(arg(a, 0)) + " != " + Interpreter.stringifyStatic(arg(a, 1)));
            return null;
        });
        reg(g, "assert_ne", (i, a) -> {
            if (Ops.equals(arg(a, 0), arg(a, 1)))
                throw new Environment.VcgRuntimeError("assert_ne failed: values are equal");
            return null;
        });
        reg(g, "assert_true", (i, a) -> {
            if (!Ops.truthy(arg(a, 0))) throw new Environment.VcgRuntimeError("assert_true failed");
            return null;
        });
        reg(g, "assert_false", (i, a) -> {
            if (Ops.truthy(arg(a, 0))) throw new Environment.VcgRuntimeError("assert_false failed");
            return null;
        });

        // ---- output helpers usable as expressions ----
        reg(g, "show", (i, a) -> {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < a.size(); k++) { if (k > 0) sb.append(' '); sb.append(Interpreter.stringifyStatic(a.get(k))); }
            i.out.text(sb.toString());
            return null;
        });
        reg(g, "print", (i, a) -> {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < a.size(); k++) { if (k > 0) sb.append(' '); sb.append(Interpreter.stringifyStatic(a.get(k))); }
            i.out.text(sb.toString());
            return null;
        });
        reg(g, "h", (i, a) -> {
            int level = (int) Ops.toDouble(arg(a, 0));
            String text = str(arg(a, 1));
            i.out.html("<h" + level + ">" + escapeHtml(text) + "</h" + level + ">");
            return null;
        });

        // ---- Seal() as a callable expression too: Seal() / Seal("text") ----
        reg(g, "Seal", (i, a) -> {
            String notice = a.isEmpty() ? "\u00A9 All rights reserved." : str(arg(a, 0));
            i.out.text(notice);
            i.seal(notice);
            return notice;
        });

        // ---- reactive store ----
        Map<String, Object> store = new LinkedHashMap<>();
        Map<String, List<VcgCallable>> watchers = new LinkedHashMap<>();
        reg(g, "$set", (i, a) -> {
            String key = str(arg(a, 0)); Object val = arg(a, 1);
            store.put(key, val);
            for (VcgCallable w : watchers.getOrDefault(key, List.of())) w.call(i, List.of(val));
            return val;
        });
        reg(g, "$get", (i, a) -> store.get(str(arg(a, 0))));
        reg(g, "watch", (i, a) -> {
            String key = str(arg(a, 0)); VcgCallable cb = (VcgCallable) arg(a, 1);
            watchers.computeIfAbsent(key, k -> new ArrayList<>()).add(cb);
            return null;
        });
        reg(g, "store_zip", (i, a) -> {
            List<Object> out = new ArrayList<>();
            for (Map.Entry<String, Object> e : store.entrySet()) {
                out.add(new ArrayList<>(List.of(e.getKey(), e.getValue())));
            }
            return out;
        });

        // ---- channels ----
        reg(g, "send", (i, a) -> { ((VcgChannel) arg(a, 0)).send(arg(a, 1)); return null; });
        reg(g, "recv", (i, a) -> ((VcgChannel) arg(a, 0)).recv());

        // ---- color / style / design / UI ----
        ColorLib.install(g);
        UiLib.install(g);
    }

    @FunctionalInterface
    private interface Fn { Object apply(Interpreter i, List<Object> args); }

    private static void reg(Environment g, String name, Fn fn) {
        g.define(name, new VcgCallable() {
            @Override public Object call(Interpreter interp, List<Object> args) { return fn.apply(interp, args); }
            @Override public String name() { return name; }
        });
    }

    static Object arg(List<Object> a, int i) { return i < a.size() ? a.get(i) : null; }
    static String str(Object o) { return Interpreter.stringifyStatic(o); }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object o) {
        if (o instanceof List) return (List<Object>) o;
        throw new Environment.VcgRuntimeError("Expected an array but got " + Ops.typeOf(o));
    }

    static int lengthOf(Object o) {
        if (o instanceof String s) return s.length();
        if (o instanceof List<?> l) return l.size();
        if (o instanceof VcgStruct st) return st.fields.size();
        return 0;
    }

    static int clampIdx(int i, int len) {
        if (i < 0) i += len;
        return Math.max(0, Math.min(i, len));
    }

    static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ---------- property access for builtin "objects" (List, String) ----------
    static Object listProp(List<?> list, String name) {
        switch (name) {
            case "length": return (double) list.size();
            default: return NO_METHOD;
        }
    }

    static Object stringProp(String s, String name) {
        switch (name) {
            case "length": return (double) s.length();
            default: return NO_METHOD;
        }
    }

    /** Returns a bound VcgCallable for struct "methods" like .color(...), or null if none apply. */
    static Object boundStructMethod(Interpreter interp, VcgStruct st, String name) {
        if (name.equals("color")) {
            return (VcgCallable) (i, args) -> UiLib.applyColorMethod(st, args);
        }
        return null;
    }

    /** Dispatch struct methods invoked via call syntax target.method(args), used when GetProp lookup
     *  alone wouldn't otherwise resolve to a callable (kept for forward compatibility). */
    static Object tryStructMethod(Interpreter interp, Object target, String name, List<Object> args) {
        if (target instanceof VcgStruct st && name.equals("color")) {
            return UiLib.applyColorMethod(st, args);
        }
        return NO_METHOD;
    }
}
