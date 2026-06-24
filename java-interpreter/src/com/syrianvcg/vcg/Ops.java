package com.syrianvcg.vcg;

import java.util.List;
import java.util.Objects;

public final class Ops {
    private Ops() {}

    public static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Double d) return d != 0.0;
        if (v instanceof String s) return !s.isEmpty();
        if (v instanceof List<?> l) return !l.isEmpty();
        return true;
    }

    public static double toDouble(Object v) {
        if (v instanceof Double d) return d;
        if (v instanceof Boolean b) return b ? 1.0 : 0.0;
        if (v instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return 0.0; }
        }
        if (v == null) return 0.0;
        throw new Environment.VcgRuntimeError("Expected a number but got " + typeOf(v));
    }

    public static String typeOf(Object v) {
        if (v == null) return "nil";
        if (v instanceof Boolean) return "bool";
        if (v instanceof Double) return "number";
        if (v instanceof String) return "string";
        if (v instanceof List) return "array";
        if (v instanceof VcgStruct st) return st.kind;
        if (v instanceof VcgInstance inst) return inst.klass.name;
        if (v instanceof VcgClass) return "class";
        if (v instanceof VcgCallable) return "function";
        if (v instanceof VcgRange) return "range";
        return "object";
    }

    @SuppressWarnings("unchecked")
    public static Object add(Object l, Object r) {
        if (l instanceof String || r instanceof String) {
            return Interpreter.stringifyStatic(l) + Interpreter.stringifyStatic(r);
        }
        if (l instanceof List && r instanceof List) {
            java.util.List<Object> out = new java.util.ArrayList<>((List<Object>) l);
            out.addAll((List<Object>) r);
            return out;
        }
        return toDouble(l) + toDouble(r);
    }

    public static Object sub(Object l, Object r) { return toDouble(l) - toDouble(r); }
    public static Object mul(Object l, Object r) {
        if (l instanceof String s && r instanceof Double n) return s.repeat(Math.max(0, n.intValue()));
        return toDouble(l) * toDouble(r);
    }
    public static Object div(Object l, Object r) {
        double rd = toDouble(r);
        if (rd == 0.0) throw new Environment.VcgRuntimeError("القسمة على صفر / Division by zero");
        return toDouble(l) / rd;
    }
    public static Object mod(Object l, Object r) {
        double rd = toDouble(r);
        if (rd == 0.0) throw new Environment.VcgRuntimeError("القسمة على صفر / Division by zero (mod)");
        return toDouble(l) % rd;
    }

    public static boolean equals(Object l, Object r) {
        if (l == null || r == null) return l == r;
        if (l instanceof Double && r instanceof Double) return ((Double) l).doubleValue() == ((Double) r).doubleValue();
        return Objects.equals(l, r);
    }

    public static int compare(Object l, Object r) {
        if (l instanceof String ls && r instanceof String rs) return ls.compareTo(rs);
        return Double.compare(toDouble(l), toDouble(r));
    }
}
