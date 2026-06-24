package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.List;

public final class VcgFunction implements VcgCallable {
    public final String declName;
    public final List<String> params;
    public final String variadicParam;
    public final List<Node.Stmt> body;
    public final Node.Expr exprBody; // for arrow-lambdas with a single expression body
    public final Environment closure;
    public VcgInstance boundSelf; // for bound methods

    public VcgFunction(String declName, List<String> params, String variadicParam,
                        List<Node.Stmt> body, Node.Expr exprBody, Environment closure) {
        this.declName = declName;
        this.params = params;
        this.variadicParam = variadicParam;
        this.body = body;
        this.exprBody = exprBody;
        this.closure = closure;
    }

    public VcgFunction bind(VcgInstance self) {
        VcgFunction f = new VcgFunction(declName, params, variadicParam, body, exprBody, closure);
        f.boundSelf = self;
        return f;
    }

    @Override
    public Object call(Interpreter interp, List<Object> args) {
        Environment env = new Environment(closure);
        if (boundSelf != null) env.define("self", boundSelf);
        for (int i = 0; i < params.size(); i++) {
            env.define(params.get(i), i < args.size() ? args.get(i) : null);
        }
        if (variadicParam != null) {
            List<Object> rest = new ArrayList<>();
            for (int i = params.size(); i < args.size(); i++) rest.add(args.get(i));
            env.define(variadicParam, rest);
        }
        if (body != null) {
            try {
                interp.execBlockStmts(body, env);
            } catch (Interpreter.ReturnSignal r) {
                return r.value;
            }
            return null;
        } else {
            return interp.eval(exprBody, env);
        }
    }

    @Override
    public String name() { return declName == null ? "<lambda>" : declName; }
}
