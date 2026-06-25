package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A genuine, native Java tree-walking interpreter for the VCG language.
 * No HTML, no JavaScript: parses VCG source into an AST and executes it
 * directly on the JVM.
 */
public final class Interpreter {

    public final OutputSink out;
    public final Environment globals = new Environment();
    private boolean sealed = false;
    private String sealNotice = null;

    public Interpreter(OutputSink out) {
        this.out = out;
        Builtins.install(this, globals);
    }

    // ---------- control-flow signals ----------
    public static final class ReturnSignal extends RuntimeException {
        public final Object value;
        public ReturnSignal(Object value) { super(null, null, false, false); this.value = value; }
    }
    public static final class BreakSignal extends RuntimeException {
        public BreakSignal() { super(null, null, false, false); }
    }
    public static final class ContinueSignal extends RuntimeException {
        public ContinueSignal() { super(null, null, false, false); }
    }
    public static final class VcgThrown extends RuntimeException {
        public final Object value;
        public VcgThrown(Object value) { super(Interpreter.stringifyStatic(value)); this.value = value; }
    }

    public void run(List<Node.Stmt> program) {
        for (Node.Stmt stmt : program) {
            exec(stmt, globals);
        }
    }

    public void checkNotSealed() {
        if (sealed) {
            throw new Environment.VcgRuntimeError(
                (sealNotice != null ? sealNotice : "© All rights reserved.") +
                " — البرنامج مختوم (Sealed) ولا يمكن تنفيذ المزيد من العبارات.");
        }
    }

    public void seal(String notice) {
        this.sealed = true;
        this.sealNotice = notice;
    }

    // =========================================================
    // Statement execution
    // =========================================================
    public void exec(Node.Stmt stmt, Environment env) {
        checkNotSealed();
        if (stmt instanceof Node.ExprStmt s) { eval(s.expr, env); return; }
        if (stmt instanceof Node.VarDecl s) {
            Object v = s.init != null ? eval(s.init, env) : null;
            env.define(s.name, v, s.isConst);
            return;
        }
        if (stmt instanceof Node.Assign s) { execAssign(s, env); return; }
        if (stmt instanceof Node.Block s) { execBlock(s, new Environment(env)); return; }
        if (stmt instanceof Node.If s) { execIf(s, env); return; }
        if (stmt instanceof Node.While s) { execWhile(s, env); return; }
        if (stmt instanceof Node.RepeatStmt s) { execRepeat(s, env); return; }
        if (stmt instanceof Node.ForIn s) { execForIn(s, env); return; }
        if (stmt instanceof Node.FuncDecl s) {
            VcgFunction f = new VcgFunction(s.name, s.params, s.variadicParam, s.body.stmts, null, env);
            env.define(s.name, f);
            return;
        }
        if (stmt instanceof Node.Return s) {
            throw new ReturnSignal(s.value != null ? eval(s.value, env) : null);
        }
        if (stmt instanceof Node.Break s) { throw new BreakSignal(); }
        if (stmt instanceof Node.Continue s) { throw new ContinueSignal(); }
        if (stmt instanceof Node.ShowStmt s) { execShow(s, env); return; }
        if (stmt instanceof Node.PrintStmt s) { execPrint(s, env); return; }
        if (stmt instanceof Node.HtmlStmt s) {
            Object v = eval(s.value, env);
            out.html(stringify(v));
            return;
        }
        if (stmt instanceof Node.SealStmt s) { execSeal(s, env); return; }
        if (stmt instanceof Node.ClassDecl s) { execClassDecl(s, env); return; }
        if (stmt instanceof Node.ModuleDecl s) { execModuleDecl(s, env); return; }
        if (stmt instanceof Node.EnumDecl s) { execEnumDecl(s, env); return; }
        if (stmt instanceof Node.TryCatch s) { execTryCatch(s, env); return; }
        if (stmt instanceof Node.SafeBlock s) { execSafe(s, env); return; }
        if (stmt instanceof Node.GuardStmt s) { execGuard(s, env); return; }
        if (stmt instanceof Node.ThrowStmt s) { throw new VcgThrown(eval(s.value, env)); }
        if (stmt instanceof Node.MatchStmt s) { execMatch(s, env); return; }
        if (stmt instanceof Node.TestStmt s) { execTest(s, env); return; }
        if (stmt instanceof Node.TypeAlias s) {
            env.define(s.name, s.value != null ? eval(s.value, env) : null);
            return;
        }
        if (stmt instanceof Node.StructDecl s) { execStructDecl(s, env); return; }
        if (stmt instanceof Node.ChannelDecl s) { env.define(s.name, new VcgChannel()); return; }
        if (stmt instanceof Node.CallWithBlock s) { execCallWithBlock(s, env); return; }
        if (stmt instanceof Node.PatternDecl s) { BalancedPattern.declare(this, s, env); return; }
        if (stmt instanceof Node.RenderStmt s) { BalancedPattern.render(this, s, env); return; }

        throw new IllegalStateException("Unhandled statement: " + stmt.getClass());
    }

    public void execBlockStmts(List<Node.Stmt> stmts, Environment env) {
        for (Node.Stmt s : stmts) exec(s, env);
    }

    /** call() { body } — يستدعي الدالة ثم يُشغِّل body ثم يستدعي end() إن وُجدت */
    private void execCallWithBlock(Node.CallWithBlock s, Environment env) {
        // 1) استدعِ الدالة (تفتح div أو مكوِّن)
        eval(s.call, env);
        // 2) شغِّل الجسم في نفس البيئة
        execBlockStmts(s.body.stmts, env);
        // 3) إذا كانت دالة end() معرَّفة في البيئة، استدعِها لإغلاق div
        try {
            Object endFn = env.get("end");
            if (endFn instanceof VcgCallable fn) fn.call(this, java.util.List.of());
        } catch (Environment.VcgRuntimeError ignored) {
            // end() غير موجودة في هذه البيئة — لا بأس
        }
    }

    private void execBlock(Node.Block block, Environment env) {
        execBlockStmts(block.stmts, env);
    }

    private void execAssign(Node.Assign s, Environment env) {
        Object rhs = eval(s.value, env);
        if (s.target instanceof Node.VarExpr v) {
            Object newVal = applyAssignOp(s.op, env.has(v.name) ? env.get(v.name) : null, rhs);
            env.set(v.name, newVal);
        } else if (s.target instanceof Node.GetProp g) {
            Object target = eval(g.target, env);
            Object cur = readProp(target, g.name);
            Object newVal = applyAssignOp(s.op, cur, rhs);
            writeProp(target, g.name, newVal);
        } else if (s.target instanceof Node.Index idx) {
            Object target = eval(idx.target, env);
            Object key = eval(idx.index, env);
            Object cur = readIndex(target, key);
            Object newVal = applyAssignOp(s.op, cur, rhs);
            writeIndex(target, key, newVal);
        } else {
            throw new Environment.VcgRuntimeError("Invalid assignment target");
        }
    }

    private Object applyAssignOp(String op, Object cur, Object rhs) {
        switch (op) {
            case "=": return rhs;
            case "+=": return Ops.add(cur, rhs);
            case "-=": return Ops.sub(cur, rhs);
            case "*=": return Ops.mul(cur, rhs);
            case "/=": return Ops.div(cur, rhs);
            default: throw new Environment.VcgRuntimeError("Unknown assignment operator: " + op);
        }
    }

    private void execIf(Node.If s, Environment env) {
        if (Ops.truthy(eval(s.cond, env))) {
            execBlock(s.thenBranch, new Environment(env));
        } else if (s.elseBranch != null) {
            if (s.elseBranch instanceof Node.Block b) execBlock(b, new Environment(env));
            else exec(s.elseBranch, env);
        }
    }

    private void execWhile(Node.While s, Environment env) {
        while (Ops.truthy(eval(s.cond, env))) {
            try {
                execBlock(s.body, new Environment(env));
            } catch (BreakSignal b) { break; }
            catch (ContinueSignal c) { /* continue loop */ }
        }
    }

    private void execRepeat(Node.RepeatStmt s, Environment env) {
        Object n = eval(s.count, env);
        long count = (long) Ops.toDouble(n);
        for (long i = 0; i < count; i++) {
            try {
                execBlock(s.body, new Environment(env));
            } catch (BreakSignal b) { break; }
            catch (ContinueSignal c) { /* continue */ }
        }
    }

    @SuppressWarnings("unchecked")
    private void execForIn(Node.ForIn s, Environment env) {
        Object iterable = eval(s.iterable, env);
        List<Object> items;
        if (iterable instanceof List) {
            items = (List<Object>) iterable;
        } else if (iterable instanceof String str) {
            items = new ArrayList<>();
            for (int i = 0; i < str.length(); i++) items.add(String.valueOf(str.charAt(i)));
        } else if (iterable instanceof VcgRange r) {
            items = r.toList();
        } else if (iterable instanceof VcgStruct st) {
            items = new ArrayList<>(st.fields.values());
        } else if (iterable == null) {
            items = new ArrayList<>();
        } else {
            throw new Environment.VcgRuntimeError("Cannot iterate over value of type " + Ops.typeOf(iterable));
        }
        for (Object item : items) {
            Environment loopEnv = new Environment(env);
            loopEnv.define(s.varName, item);
            try {
                execBlock(s.body, loopEnv);
            } catch (BreakSignal b) { break; }
            catch (ContinueSignal c) { /* continue */ }
        }
    }

    private void execShow(Node.ShowStmt s, Environment env) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.args.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(stringify(eval(s.args.get(i), env)));
        }
        out.text(sb.toString());
    }

    private void execPrint(Node.PrintStmt s, Environment env) {
        // print(...) behaves like show(...): prints each argument, space-joined.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.args.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(stringify(eval(s.args.get(i), env)));
        }
        out.text(sb.toString());
    }

    private void execSeal(Node.SealStmt s, Environment env) {
        // Seal() or Seal("custom notice") — marks the program as sealed/finalized.
        // After Seal(), no further statements may execute: a tamper-evident "stop" marker,
        // and it prints a rights-reserved notice (default or custom).
        String notice;
        if (!s.args.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.args.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(stringify(eval(s.args.get(i), env)));
            }
            notice = sb.toString();
        } else {
            notice = "\u00A9 All rights reserved.";
        }
        out.text(notice);
        seal(notice);
    }

    private void execClassDecl(Node.ClassDecl s, Environment env) {
        VcgClass superclass = null;
        if (s.superName != null) {
            Object sc = env.get(s.superName);
            if (sc instanceof VcgClass vc) superclass = vc;
            else throw new Environment.VcgRuntimeError("Superclass '" + s.superName + "' is not a class");
        }
        VcgClass klass = new VcgClass(s.name, superclass);
        env.define(s.name, klass);
        for (Node.FuncDecl m : s.methods) {
            VcgFunction fn = new VcgFunction(m.name, m.params, m.variadicParam, m.body.stmts, null, env);
            klass.methods.put(m.name, fn);
        }
    }

    private void execModuleDecl(Node.ModuleDecl s, Environment env) {
        Environment moduleEnv = new Environment(env);
        for (Node.Stmt st : s.body) exec(st, moduleEnv);
        VcgStruct mod = new VcgStruct("Module");
        // expose module env bindings as struct fields for `Module.member` access
        // (we walk the body decls rather than the whole env to avoid leaking builtins)
        for (Node.Stmt st : s.body) {
            if (st instanceof Node.VarDecl vd) mod.set(vd.name, moduleEnv.get(vd.name));
            else if (st instanceof Node.FuncDecl fd) mod.set(fd.name, moduleEnv.get(fd.name));
        }
        env.define(s.name, mod);
    }

    private void execEnumDecl(Node.EnumDecl s, Environment env) {
        VcgStruct e = new VcgStruct("Enum");
        for (int i = 0; i < s.values.size(); i++) e.set(s.values.get(i), (double) i);
        env.define(s.name, e);
    }

    private void execTryCatch(Node.TryCatch s, Environment env) {
        try {
            execBlock(s.tryBlock, new Environment(env));
        } catch (VcgThrown t) {
            Environment catchEnv = new Environment(env);
            if (s.errName != null) catchEnv.define(s.errName, t.value);
            execBlock(s.catchBlock, catchEnv);
        } catch (Environment.VcgRuntimeError e) {
            Environment catchEnv = new Environment(env);
            if (s.errName != null) catchEnv.define(s.errName, e.getMessage());
            execBlock(s.catchBlock, catchEnv);
        }
    }

    private void execSafe(Node.SafeBlock s, Environment env) {
        try {
            execBlock(s.body, new Environment(env));
        } catch (VcgThrown | Environment.VcgRuntimeError e) {
            // swallow silently, per VCG "safe" semantics
        }
    }

    private void execGuard(Node.GuardStmt s, Environment env) {
        if (!Ops.truthy(eval(s.cond, env))) {
            execBlock(s.elseBlock, new Environment(env));
        }
    }

    private void execMatch(Node.MatchStmt s, Environment env) {
        Object val = eval(s.value, env);
        for (int i = 0; i < s.whenValues.size(); i++) {
            Object arm = eval(s.whenValues.get(i), env);
            if (Ops.equals(val, arm)) {
                exec(s.whenBodies.get(i), new Environment(env));
                return;
            }
        }
    }

    private void execTest(Node.TestStmt s, Environment env) {
        try {
            execBlock(s.body, new Environment(env));
            out.text("[PASS] " + s.name);
        } catch (VcgThrown | Environment.VcgRuntimeError e) {
            out.text("[FAIL] " + s.name + ": " + e.getMessage());
        }
    }

    private void execStructDecl(Node.StructDecl s, Environment env) {
        VcgStruct proto = new VcgStruct(s.name);
        for (String f : s.fields) proto.set(f, null);
        env.define(s.name, proto);
    }

    // =========================================================
    // Expression evaluation
    // =========================================================
    @SuppressWarnings("unchecked")
    public Object eval(Node.Expr expr, Environment env) {
        if (expr instanceof Node.Literal e) return e.value;
        if (expr instanceof Node.VarExpr e) return env.get(e.name);
        if (expr instanceof Node.SelfExpr e) return env.get("self");
        if (expr instanceof Node.ArrayLit e) {
            List<Object> list = new ArrayList<>();
            for (Node.Expr el : e.elements) list.add(eval(el, env));
            return list;
        }
        if (expr instanceof Node.ObjectLit e) {
            VcgStruct st = new VcgStruct();
            for (int i = 0; i < e.keys.size(); i++) st.set(e.keys.get(i), eval(e.values.get(i), env));
            return st;
        }
        if (expr instanceof Node.Unary e) {
            Object v = eval(e.right, env);
            if (e.op.equals("-")) return -Ops.toDouble(v);
            if (e.op.equals("not")) return !Ops.truthy(v);
            throw new Environment.VcgRuntimeError("Unknown unary operator " + e.op);
        }
        if (expr instanceof Node.Binary e) return evalBinary(e, env);
        if (expr instanceof Node.Logical e) {
            Object l = eval(e.left, env);
            if (e.op.equals("and")) return Ops.truthy(l) ? eval(e.right, env) : l;
            else return Ops.truthy(l) ? l : eval(e.right, env);
        }
        if (expr instanceof Node.Ternary e) {
            return Ops.truthy(eval(e.cond, env)) ? eval(e.then, env) : eval(e.otherwise, env);
        }
        if (expr instanceof Node.RangeExpr e) {
            int from = (int) Ops.toDouble(eval(e.from, env));
            int to = (int) Ops.toDouble(eval(e.to, env));
            return new VcgRange(from, to);
        }
        if (expr instanceof Node.Pipeline e) {
            Object leftVal = eval(e.left, env);
            if (e.right instanceof Node.Call call) {
                Object callee = eval(call.callee, env);
                List<Object> args = new ArrayList<>();
                args.add(leftVal);
                for (Node.Expr a : call.args) args.add(eval(a, env));
                return invoke(callee, args);
            } else {
                Object fn = eval(e.right, env);
                return invoke(fn, List.of(leftVal));
            }
        }
        if (expr instanceof Node.Call e) return evalCall(e, env);
        if (expr instanceof Node.GetProp e) {
            Object target = eval(e.target, env);
            return readProp(target, e.name);
        }
        if (expr instanceof Node.Index e) {
            Object target = eval(e.target, env);
            Object key = eval(e.index, env);
            return readIndex(target, key);
        }
        if (expr instanceof Node.Lambda e) {
            return new VcgFunction(null, e.params, null, e.body, e.exprBody, env);
        }
        if (expr instanceof Node.NewExpr e) {
            Object klass = env.get(e.className);
            if (!(klass instanceof VcgClass)) throw new Environment.VcgRuntimeError("'" + e.className + "' is not a class");
            List<Object> args = new ArrayList<>();
            for (Node.Expr a : e.args) args.add(eval(a, env));
            return ((VcgClass) klass).call(this, args);
        }
        throw new IllegalStateException("Unhandled expression: " + expr.getClass());
    }

    private Object evalBinary(Node.Binary e, Environment env) {
        Object l = eval(e.left, env);
        Object r = eval(e.right, env);
        switch (e.op) {
            case "+": return Ops.add(l, r);
            case "-": return Ops.sub(l, r);
            case "*": return Ops.mul(l, r);
            case "/": return Ops.div(l, r);
            case "%": return Ops.mod(l, r);
            case "**": return Math.pow(Ops.toDouble(l), Ops.toDouble(r));
            case "==": return Ops.equals(l, r);
            case "!=": return !Ops.equals(l, r);
            case "<": return Ops.compare(l, r) < 0;
            case ">": return Ops.compare(l, r) > 0;
            case "<=": return Ops.compare(l, r) <= 0;
            case ">=": return Ops.compare(l, r) >= 0;
            case "&": return (double) (((long) Ops.toDouble(l)) & ((long) Ops.toDouble(r)));
            case "|": return (double) (((long) Ops.toDouble(l)) | ((long) Ops.toDouble(r)));
            case "<<": return (double) (((long) Ops.toDouble(l)) << ((long) Ops.toDouble(r)));
            case ">>": return (double) (((long) Ops.toDouble(l)) >> ((long) Ops.toDouble(r)));
            default: throw new Environment.VcgRuntimeError("Unknown binary operator " + e.op);
        }
    }

    private Object evalCall(Node.Call e, Environment env) {
        // method-call sugar: target.method(args) where target is GetProp
        if (e.callee instanceof Node.GetProp gp) {
            Object target = eval(gp.target, env);
            Object maybeMethod = tryReadProp(target, gp.name);
            if (maybeMethod instanceof VcgCallable callable) {
                List<Object> args = new ArrayList<>();
                for (Node.Expr a : e.args) args.add(eval(a, env));
                return callable.call(this, args);
            }
            if (target instanceof VcgStruct || target instanceof VcgInstance) {
                List<Object> args = new ArrayList<>();
                for (Node.Expr a : e.args) args.add(eval(a, env));
                Object result = Builtins.tryStructMethod(this, target, gp.name, args);
                if (result != Builtins.NO_METHOD) return result;
            }
        }
        Object callee = eval(e.callee, env);
        List<Object> args = new ArrayList<>();
        for (Node.Expr a : e.args) args.add(eval(a, env));
        return invoke(callee, args);
    }

    public Object invoke(Object callee, List<Object> args) {
        if (callee instanceof VcgCallable c) return c.call(this, args);
        throw new Environment.VcgRuntimeError("القيمة غير قابلة للاستدعاء / Value is not callable: " + stringify(callee));
    }

    // ---------- property / index access ----------
    public Object readProp(Object target, String name) {
        Object v = tryReadProp(target, name);
        if (v == Builtins.NO_METHOD) {
            throw new Environment.VcgRuntimeError("لا توجد خاصية / No property '" + name + "' on " + Ops.typeOf(target));
        }
        return v;
    }

    public Object tryReadProp(Object target, String name) {
        if (target instanceof VcgStruct st) {
            if (st.has(name)) return st.get(name);
            Object bm = Builtins.boundStructMethod(this, st, name);
            if (bm != null) return bm;
            return Builtins.NO_METHOD;
        }
        if (target instanceof VcgInstance inst) {
            return inst.get(name);
        }
        if (target instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) target;
            return m.getOrDefault(name, Builtins.NO_METHOD);
        }
        if (target instanceof List<?> list) {
            return Builtins.listProp(list, name);
        }
        if (target instanceof String str) {
            return Builtins.stringProp(str, name);
        }
        return Builtins.NO_METHOD;
    }

    public void writeProp(Object target, String name, Object value) {
        if (target instanceof VcgStruct st) { st.set(name, value); return; }
        if (target instanceof VcgInstance inst) { inst.set(name, value); return; }
        throw new Environment.VcgRuntimeError("لا يمكن الكتابة على هذه القيمة / Cannot set property on " + Ops.typeOf(target));
    }

    @SuppressWarnings("unchecked")
    public Object readIndex(Object target, Object key) {
        if (target instanceof List) {
            List<Object> list = (List<Object>) target;
            int i = (int) Ops.toDouble(key);
            if (i < 0) i += list.size();
            if (i < 0 || i >= list.size()) return null;
            return list.get(i);
        }
        if (target instanceof String s) {
            int i = (int) Ops.toDouble(key);
            if (i < 0) i += s.length();
            if (i < 0 || i >= s.length()) return null;
            return String.valueOf(s.charAt(i));
        }
        if (target instanceof VcgStruct st) {
            return st.get(String.valueOf(key));
        }
        throw new Environment.VcgRuntimeError("لا يمكن الفهرسة / Cannot index into " + Ops.typeOf(target));
    }

    @SuppressWarnings("unchecked")
    public void writeIndex(Object target, Object key, Object value) {
        if (target instanceof List) {
            List<Object> list = (List<Object>) target;
            int i = (int) Ops.toDouble(key);
            if (i < 0) i += list.size();
            while (list.size() <= i) list.add(null);
            list.set(i, value);
            return;
        }
        if (target instanceof VcgStruct st) {
            st.set(String.valueOf(key), value);
            return;
        }
        throw new Environment.VcgRuntimeError("لا يمكن الكتابة بالفهرسة / Cannot index-assign into " + Ops.typeOf(target));
    }

    // ---------- stringify ----------
    public String stringify(Object v) { return stringifyStatic(v); }

    public static String stringifyStatic(Object v) {
        if (v == null) return "nil";
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof Double d) {
            if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1e15) {
                return String.valueOf((long) (double) d);
            }
            return String.valueOf(d);
        }
        if (v instanceof String s) return s;
        if (v instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                Object el = list.get(i);
                sb.append(el instanceof String ? "\"" + el + "\"" : stringifyStatic(el));
            }
            return sb.append("]").toString();
        }
        if (v instanceof VcgStruct st) {
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<String, Object> en : st.fields.entrySet()) {
                if (i++ > 0) sb.append(", ");
                Object val = en.getValue();
                sb.append(en.getKey()).append(": ")
                  .append(val instanceof String ? "\"" + val + "\"" : stringifyStatic(val));
            }
            return sb.append("}").toString();
        }
        if (v instanceof VcgInstance inst) return "<" + inst.klass.name + " instance>";
        if (v instanceof VcgClass kl) return "<class " + kl.name + ">";
        if (v instanceof VcgFunction fn) return "<func " + fn.name() + ">";
        if (v instanceof VcgRange r) return r.from + ".." + r.to;
        return v.toString();
    }
}
