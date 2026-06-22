package com.syrianvcg.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VcgHeadlessRunner — مُنفّذ مصغّر لكود VCG داخل الترمنال (بدون WebView)
 * يدعم: show(...)، let/const، تعابير رياضية بسيطة، نصوص، ودوال رياضية أساسية.
 * هذا ليس بديلاً عن المترجم الكامل (المستخدم في المعاينة المباشرة)، بل أداة REPL سريعة للأسطر الواحدة.
 */
public class VcgHeadlessRunner {

    /**
     * ⚠️ كانت session خريطة واحدة مشتركة بين كل التيرمينالات بلا تمييز
     * بين المشاريع. فتح تيرمينال لمشروع، تعريف متغيّرات فيه، ثم فتح
     * تيرمينال لمشروع آخر، كان يُبقي متغيّرات المشروع الأول مرئية ومتاحة
     * في الثاني (تسرّب حالة). الآن كل مشروع له خريطة متغيّرات مستقلة،
     * مفتاحها projectId (أو "global" إن لم يكن هناك مشروع محدَّد).
     */
    private static final Map<String, Map<String, Double>> sessionsByProject = new HashMap<>();

    private static Map<String, Double> sessionFor(String projectId) {
        String key = projectId == null ? "global" : projectId;
        return sessionsByProject.computeIfAbsent(key, k -> new HashMap<>());
    }

    public static String run(String code, String projectId) {
        Map<String, Double> session = sessionFor(projectId);
        StringBuilder out = new StringBuilder();
        try {
            for (String rawLine : code.split("\n")) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String result = execLine(line, session);
                if (result != null) out.append(result).append("\n");
            }
        } catch (Exception e) {
            out.append("ERR:").append(e.getMessage() == null ? "خطأ غير معروف" : e.getMessage()).append("\n");
        }
        if (out.length() == 0) out.append("(تم التنفيذ بلا مخرجات)\n");
        return out.toString();
    }

    private static String execLine(String line, Map<String, Double> session) {
        // let/const x = expr
        Matcher letM = Pattern.compile("^(let|const)\\s+([a-zA-Z_]\\w*)\\s*=\\s*(.+)$").matcher(line);
        if (letM.matches()) {
            double v = evalExpr(letM.group(3), session);
            session.put(letM.group(2), v);
            return letM.group(2) + " = " + fmt(v);
        }

        // show(...)
        Matcher showM = Pattern.compile("^(show|print)\\s*\\((.*)\\)$").matcher(line);
        if (showM.matches()) {
            String argsRaw = showM.group(2);
            List<String> parts = splitArgs(argsRaw);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(evalToken(parts.get(i).trim(), session));
            }
            return sb.toString();
        }

        // bare expression
        try {
            double v = evalExpr(line, session);
            return fmt(v);
        } catch (Exception e) {
            return "ERR:" + (e.getMessage() == null ? ("تعبير غير مفهوم: " + line) : e.getMessage());
        }
    }

    private static String evalToken(String tok, Map<String, Double> session) {
        if (tok.length() >= 2 && (tok.charAt(0) == '"' || tok.charAt(0) == '\'')
                && tok.charAt(tok.length() - 1) == tok.charAt(0)) {
            return tok.substring(1, tok.length() - 1);
        }
        try {
            return fmt(evalExpr(tok, session));
        } catch (Exception e) {
            return tok;
        }
    }

    private static List<String> splitArgs(String s) {
        List<String> result = new ArrayList<>();
        int depth = 0; boolean inStr = false; char strCh = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                cur.append(c);
                if (c == strCh) inStr = false;
                continue;
            }
            if (c == '"' || c == '\'') { inStr = true; strCh = c; cur.append(c); continue; }
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (c == ',' && depth == 0) { result.add(cur.toString()); cur = new StringBuilder(); continue; }
            cur.append(c);
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result;
    }

    /** Simple recursive-descent arithmetic evaluator supporting +,-,*,/,%,**, parens, vars, sqrt/abs/etc */
    private static double evalExpr(String expr, Map<String, Double> session) {
        ExprParser p = new ExprParser(expr.trim(), session);
        double v = p.parseExpr();
        p.skipSpaces();
        if (p.pos < p.s.length()) throw new RuntimeException("رمز غير متوقع: " + p.s.substring(p.pos));
        return v;
    }

    private static String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    /** يصفّر متغيّرات تيرمينال مشروع معيّن (أو العالمي إن لم يُحدَّد مشروع). */
    public static void resetSession(String projectId) {
        sessionFor(projectId).clear();
    }

    private static class ExprParser {
        final String s; int pos = 0;
        final Map<String, Double> session;
        ExprParser(String s, Map<String, Double> session) { this.s = s; this.session = session; }

        void skipSpaces() { while (pos < s.length() && s.charAt(pos) == ' ') pos++; }
        char peek() { skipSpaces(); return pos < s.length() ? s.charAt(pos) : '\0'; }

        double parseExpr() { return parseAdd(); }

        double parseAdd() {
            double v = parseMul();
            while (true) {
                char c = peek();
                if (c == '+') { pos++; v += parseMul(); }
                else if (c == '-') { pos++; v -= parseMul(); }
                else break;
            }
            return v;
        }

        double parseMul() {
            double v = parsePow();
            while (true) {
                char c = peek();
                if (c == '*' && pos + 1 < s.length() && s.charAt(pos + 1) == '*') { pos += 2; v = Math.pow(v, parsePow()); }
                else if (c == '*') { pos++; v *= parsePow(); }
                else if (c == '/') { pos++; v /= parsePow(); }
                else if (c == '%') { pos++; v %= parsePow(); }
                else break;
            }
            return v;
        }

        double parsePow() { return parseUnary(); }

        double parseUnary() {
            char c = peek();
            if (c == '-') { pos++; return -parseUnary(); }
            if (c == '+') { pos++; return parseUnary(); }
            return parseAtom();
        }

        double parseAtom() {
            skipSpaces();
            if (pos >= s.length()) throw new RuntimeException("تعبير غير مكتمل");
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                double v = parseExpr();
                skipSpaces();
                if (pos < s.length() && s.charAt(pos) == ')') pos++;
                return v;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                return Double.parseDouble(s.substring(start, pos));
            }
            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) pos++;
                String name = s.substring(start, pos);
                skipSpaces();
                if (pos < s.length() && s.charAt(pos) == '(') {
                    pos++;
                    List<Double> args = new ArrayList<>();
                    skipSpaces();
                    if (pos < s.length() && s.charAt(pos) != ')') {
                        args.add(parseExpr());
                        skipSpaces();
                        while (pos < s.length() && s.charAt(pos) == ',') {
                            pos++; args.add(parseExpr()); skipSpaces();
                        }
                    }
                    if (pos < s.length() && s.charAt(pos) == ')') pos++;
                    return callFn(name, args);
                }
                if (name.equals("PI")) return Math.PI;
                if (name.equals("E")) return Math.E;
                if (session.containsKey(name)) return session.get(name);
                throw new RuntimeException("متغير غير معروف: " + name);
            }
            throw new RuntimeException("رمز غير متوقع: " + c);
        }

        double callFn(String name, List<Double> args) {
            switch (name) {
                case "sqrt": return Math.sqrt(args.get(0));
                case "abs": return Math.abs(args.get(0));
                case "floor": return Math.floor(args.get(0));
                case "ceil": return Math.ceil(args.get(0));
                case "round": return Math.round(args.get(0));
                case "pow": return Math.pow(args.get(0), args.get(1));
                case "min": return Math.min(args.get(0), args.get(1));
                case "max": return Math.max(args.get(0), args.get(1));
                case "sin": return Math.sin(args.get(0));
                case "cos": return Math.cos(args.get(0));
                default: throw new RuntimeException("دالة غير معروفة: " + name);
            }
        }
    }
}
