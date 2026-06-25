package com.syrianvcg.vcg;

import java.util.List;

/** All AST node types for the VCG language, expressed as a sealed-ish hierarchy. */
public abstract class Node {

    // ---------- Expressions ----------
    public static abstract class Expr extends Node {}

    public static final class Literal extends Expr {
        public final Object value;
        public Literal(Object value) { this.value = value; }
    }

    public static final class ArrayLit extends Expr {
        public final List<Expr> elements;
        public ArrayLit(List<Expr> elements) { this.elements = elements; }
    }

    public static final class ObjectLit extends Expr {
        public final List<String> keys;
        public final List<Expr> values;
        public ObjectLit(List<String> keys, List<Expr> values) { this.keys = keys; this.values = values; }
    }

    public static final class VarExpr extends Expr {
        public final String name;
        public VarExpr(String name) { this.name = name; }
    }

    public static final class Unary extends Expr {
        public final String op;
        public final Expr right;
        public Unary(String op, Expr right) { this.op = op; this.right = right; }
    }

    public static final class Binary extends Expr {
        public final String op;
        public final Expr left, right;
        public Binary(String op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
    }

    public static final class Logical extends Expr {
        public final String op; // and / or
        public final Expr left, right;
        public Logical(String op, Expr left, Expr right) { this.op = op; this.left = left; this.right = right; }
    }

    public static final class Ternary extends Expr {
        public final Expr cond, then, otherwise;
        public Ternary(Expr cond, Expr then, Expr otherwise) { this.cond = cond; this.then = then; this.otherwise = otherwise; }
    }

    public static final class RangeExpr extends Expr {
        public final Expr from, to;
        public RangeExpr(Expr from, Expr to) { this.from = from; this.to = to; }
    }

    public static final class Call extends Expr {
        public final Expr callee;
        public final List<Expr> args;
        public Call(Expr callee, List<Expr> args) { this.callee = callee; this.args = args; }
    }

    public static final class GetProp extends Expr {
        public final Expr target;
        public final String name;
        public GetProp(Expr target, String name) { this.target = target; this.name = name; }
    }

    public static final class Index extends Expr {
        public final Expr target;
        public final Expr index;
        public Index(Expr target, Expr index) { this.target = target; this.index = index; }
    }

    public static final class Lambda extends Expr {
        public final List<String> params;
        public final List<Stmt> body; // block body
        public final Expr exprBody;   // single-expr body (\x -> x*2)
        public Lambda(List<String> params, List<Stmt> body, Expr exprBody) {
            this.params = params; this.body = body; this.exprBody = exprBody;
        }
    }

    public static final class NewExpr extends Expr {
        public final String className;
        public final List<Expr> args;
        public NewExpr(String className, List<Expr> args) { this.className = className; this.args = args; }
    }

    public static final class SelfExpr extends Expr {}

    public static final class Pipeline extends Expr {
        public final Expr left;
        public final Expr right; // a call expression missing its first arg
        public Pipeline(Expr left, Expr right) { this.left = left; this.right = right; }
    }

    // ---------- Statements ----------
    public static abstract class Stmt extends Node {}

    public static final class ExprStmt extends Stmt {
        public final Expr expr;
        public ExprStmt(Expr expr) { this.expr = expr; }
    }

    public static final class VarDecl extends Stmt {
        public final String name;
        public final Expr init;
        public final boolean isConst;
        public VarDecl(String name, Expr init, boolean isConst) { this.name = name; this.init = init; this.isConst = isConst; }
    }

    public static final class Assign extends Stmt {
        public final Expr target; // VarExpr, GetProp, or Index
        public final String op;   // = += -= *= /=
        public final Expr value;
        public Assign(Expr target, String op, Expr value) { this.target = target; this.op = op; this.value = value; }
    }

    public static final class Block extends Stmt {
        public final List<Stmt> stmts;
        public Block(List<Stmt> stmts) { this.stmts = stmts; }
    }

    public static final class If extends Stmt {
        public final Expr cond;
        public final Block thenBranch;
        public final Stmt elseBranch; // Block or If or null
        public If(Expr cond, Block thenBranch, Stmt elseBranch) {
            this.cond = cond; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
    }

    public static final class While extends Stmt {
        public final Expr cond;
        public final Block body;
        public While(Expr cond, Block body) { this.cond = cond; this.body = body; }
    }

    public static final class RepeatStmt extends Stmt {
        public final Expr count;
        public final Block body;
        public RepeatStmt(Expr count, Block body) { this.count = count; this.body = body; }
    }

    public static final class ForIn extends Stmt {
        public final String varName;
        public final Expr iterable;
        public final Block body;
        public ForIn(String varName, Expr iterable, Block body) {
            this.varName = varName; this.iterable = iterable; this.body = body;
        }
    }

    public static final class FuncDecl extends Stmt {
        public final String name;
        public final List<String> params;
        public final String variadicParam; // ..nums or null
        public final Block body;
        public final boolean isAsync;
        public FuncDecl(String name, List<String> params, String variadicParam, Block body, boolean isAsync) {
            this.name = name; this.params = params; this.variadicParam = variadicParam;
            this.body = body; this.isAsync = isAsync;
        }
    }

    public static final class Return extends Stmt {
        public final Expr value;
        public Return(Expr value) { this.value = value; }
    }

    public static final class Break extends Stmt {}
    public static final class Continue extends Stmt {}

    public static final class ShowStmt extends Stmt {
        public final List<Expr> args;
        public ShowStmt(List<Expr> args) { this.args = args; }
    }

    public static final class PrintStmt extends Stmt {
        public final List<Expr> args;
        public PrintStmt(List<Expr> args) { this.args = args; }
    }

    public static final class HtmlStmt extends Stmt {
        public final Expr value;
        public HtmlStmt(Expr value) { this.value = value; }
    }

    public static final class SealStmt extends Stmt {
        public final List<Expr> args;
        public SealStmt(List<Expr> args) { this.args = args; }
    }

    public static final class ClassDecl extends Stmt {
        public final String name;
        public final String superName; // nullable
        public final List<FuncDecl> methods;
        public ClassDecl(String name, String superName, List<FuncDecl> methods) {
            this.name = name; this.superName = superName; this.methods = methods;
        }
    }

    public static final class ModuleDecl extends Stmt {
        public final String name;
        public final List<Stmt> body;
        public ModuleDecl(String name, List<Stmt> body) { this.name = name; this.body = body; }
    }

    public static final class EnumDecl extends Stmt {
        public final String name;
        public final List<String> values;
        public EnumDecl(String name, List<String> values) { this.name = name; this.values = values; }
    }

    public static final class TryCatch extends Stmt {
        public final Block tryBlock;
        public final String errName; // nullable
        public final Block catchBlock;
        public TryCatch(Block tryBlock, String errName, Block catchBlock) {
            this.tryBlock = tryBlock; this.errName = errName; this.catchBlock = catchBlock;
        }
    }

    public static final class SafeBlock extends Stmt {
        public final Block body;
        public SafeBlock(Block body) { this.body = body; }
    }

    public static final class GuardStmt extends Stmt {
        public final Expr cond;
        public final Block elseBlock;
        public GuardStmt(Expr cond, Block elseBlock) { this.cond = cond; this.elseBlock = elseBlock; }
    }

    public static final class ThrowStmt extends Stmt {
        public final Expr value;
        public ThrowStmt(Expr value) { this.value = value; }
    }

    public static final class MatchStmt extends Stmt {
        public final Expr value;
        public final List<Expr> whenValues;
        public final List<Stmt> whenBodies;
        public MatchStmt(Expr value, List<Expr> whenValues, List<Stmt> whenBodies) {
            this.value = value; this.whenValues = whenValues; this.whenBodies = whenBodies;
        }
    }

    public static final class TestStmt extends Stmt {
        public final String name;
        public final Block body;
        public TestStmt(String name, Block body) { this.name = name; this.body = body; }
    }

    public static final class TypeAlias extends Stmt {
        public final String name;
        public final Expr value;
        public TypeAlias(String name, Expr value) { this.name = name; this.value = value; }
    }

    public static final class StructDecl extends Stmt {
        public final String name;
        public final List<String> fields;
        public StructDecl(String name, List<String> fields) { this.name = name; this.fields = fields; }
    }

    public static final class ChannelDecl extends Stmt {
        public final String name;
        public ChannelDecl(String name) { this.name = name; }
    }

    public static final class PatternDecl extends Stmt {
        public final String typeName; // e.g. Balanced
        public final String patternName;
        public final List<String> keys;
        public final List<Expr> values;
        public PatternDecl(String typeName, String patternName, List<String> keys, List<Expr> values) {
            this.typeName = typeName; this.patternName = patternName; this.keys = keys; this.values = values;
        }
    }

    public static final class RenderStmt extends Stmt {
        public final Expr target; // ident or string
        public final Block body;  // nullable
        public RenderStmt(Expr target, Block body) { this.target = target; this.body = body; }
    }
}
