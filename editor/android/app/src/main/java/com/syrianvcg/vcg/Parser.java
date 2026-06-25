package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.List;

public final class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    public static final class ParseError extends RuntimeException {
        public ParseError(String msg) { super(msg); }
    }

    // ---------- token helpers ----------
    private Token peek() { return tokens.get(pos); }
    private Token peekAt(int off) { int i = pos + off; return i < tokens.size() ? tokens.get(i) : tokens.get(tokens.size() - 1); }
    private Token previous() { return tokens.get(pos - 1); }
    private boolean isAtEnd() { return peek().type == TokenType.EOF; }

    private Token advance() { if (!isAtEnd()) pos++; return previous(); }

    private boolean check(TokenType t) { return !isAtEnd() && peek().type == t; }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token expect(TokenType t, String msg) {
        if (check(t)) return advance();
        throw new ParseError(msg + " — got " + peek() + " at line " + peek().line);
    }

    private void skipNewlines() {
        while (check(TokenType.NEWLINE) || check(TokenType.SEMI)) advance();
    }

    // ---------- entry point ----------
    public List<Node.Stmt> parseProgram() {
        List<Node.Stmt> stmts = new ArrayList<>();
        skipNewlines();
        while (!isAtEnd()) {
            stmts.add(parseStmt());
            skipNewlines();
        }
        return stmts;
    }

    private Node.Block parseBlock() {
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<Node.Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            stmts.add(parseStmt());
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.Block(stmts);
    }

    // ---------- statements ----------
    private Node.Stmt parseStmt() {
        if (check(TokenType.LET) || check(TokenType.CONST)) return parseVarDecl();
        if (check(TokenType.W)) return parseWriteOnlyDecl();
        if (check(TokenType.FUNC)) return parseFuncDecl(false);
        if (check(TokenType.ASYNC)) { advance(); expect(TokenType.FUNC, "Expected 'func' after 'async'"); return parseFuncDeclBody(true); }
        if (check(TokenType.IF)) return parseIf();
        if (check(TokenType.WHILE)) return parseWhile();
        if (check(TokenType.REPEAT)) return parseRepeat();
        if (check(TokenType.FOR)) return parseForIn();
        if (check(TokenType.RETURN)) return parseReturn();
        if (check(TokenType.BREAK)) { advance(); return new Node.Break(); }
        if (check(TokenType.CONTINUE)) { advance(); return new Node.Continue(); }
        if (check(TokenType.SHOW)) return parseShow();
        if (check(TokenType.PRINT)) return parsePrint();
        if (check(TokenType.HTML)) return parseHtml();
        if (check(TokenType.SEAL)) return parseSeal();
        if (check(TokenType.CLASS)) return parseClassDecl();
        if (check(TokenType.MODULE)) return parseModuleDecl();
        if (check(TokenType.ENUM)) return parseEnumDecl();
        if (check(TokenType.TRY)) return parseTryCatch();
        if (check(TokenType.SAFE)) { advance(); return new Node.SafeBlock(parseBlock()); }
        if (check(TokenType.UNSAFE)) { advance(); return parseBlock(); }
        if (check(TokenType.GUARD)) return parseGuard();
        if (check(TokenType.THROW)) { advance(); Node.Expr v = parseExpr(); return new Node.ThrowStmt(v); }
        if (check(TokenType.MATCH)) return parseMatch();
        if (check(TokenType.TEST)) return parseTest();
        if (check(TokenType.ASSERT)) return parseAssert();
        if (check(TokenType.TYPE)) return parseTypeAlias();
        if (check(TokenType.STRUCT)) return parseStructDecl();
        if (check(TokenType.C_CHANNEL) && peekAt(1).type == TokenType.IDENT) { advance(); String nm = expect(TokenType.IDENT, "channel name").text; return new Node.ChannelDecl(nm); }
        if (check(TokenType.PATTERN)) return parsePatternDecl();
        if (check(TokenType.RENDER)) return parseRenderStmt();
        if (check(TokenType.FROM) || check(TokenType.IMPORT)) return parseImportLike();
        if (check(TokenType.LBRACE)) return parseBlock();

        return parseExprOrAssignStmt();
    }

    private Node.Stmt parseImportLike() {
        // from X import y, z   OR   import X
        while (!check(TokenType.NEWLINE) && !check(TokenType.SEMI) && !isAtEnd() && !check(TokenType.LBRACE)) advance();
        return new Node.Block(new ArrayList<>());
    }

    private Node.Stmt parseVarDecl() {
        boolean isConst = check(TokenType.CONST);
        advance();
        String name = expect(TokenType.IDENT, "Expected variable name").text;
        Node.Expr init = null;
        if (match(TokenType.ASSIGN)) init = parseExpr();
        return new Node.VarDecl(name, init, isConst);
    }

    private Node.Stmt parseWriteOnlyDecl() {
        advance(); // w
        String name = expect(TokenType.IDENT, "Expected variable name").text;
        Node.Expr init = null;
        if (match(TokenType.ASSIGN)) init = parseExpr();
        return new Node.VarDecl(name, init, false);
    }

    private Node.Stmt parseFuncDecl(boolean isAsync) {
        advance(); // func
        return parseFuncDeclBody(isAsync);
    }

    private Node.Stmt parseFuncDeclBody(boolean isAsync) {
        String name = expect(TokenType.IDENT, "Expected function name").text;
        expect(TokenType.LPAREN, "Expected '('");
        List<String> params = new ArrayList<>();
        String variadic = null;
        while (!check(TokenType.RPAREN) && !isAtEnd()) {
            if (match(TokenType.DOTDOT)) {
                variadic = expect(TokenType.IDENT, "Expected variadic param name").text;
            } else {
                params.add(expect(TokenType.IDENT, "Expected parameter name").text);
            }
            match(TokenType.COMMA);
        }
        expect(TokenType.RPAREN, "Expected ')'");
        Node.Block body = parseBlock();
        return new Node.FuncDecl(name, params, variadic, body, isAsync);
    }

    private Node.Stmt parseIf() {
        advance(); // if
        Node.Expr cond = parseExpr();
        Node.Block thenB = parseBlock();
        Node.Stmt elseB = null;
        skipOptionalNewlineBeforeElse();
        if (check(TokenType.ELSE)) {
            advance();
            if (check(TokenType.IF)) {
                elseB = parseIf();
            } else {
                elseB = parseBlock();
            }
        }
        return new Node.If(cond, thenB, elseB);
    }

    private void skipOptionalNewlineBeforeElse() {
        int save = pos;
        while (check(TokenType.NEWLINE)) advance();
        if (!check(TokenType.ELSE)) pos = save;
    }

    private Node.Stmt parseWhile() {
        advance();
        Node.Expr cond = parseExpr();
        Node.Block body = parseBlock();
        return new Node.While(cond, body);
    }

    private Node.Stmt parseRepeat() {
        advance();
        Node.Expr count = parseExpr();
        Node.Block body = parseBlock();
        return new Node.RepeatStmt(count, body);
    }

    private Node.Stmt parseForIn() {
        advance();
        String varName = expect(TokenType.IDENT, "Expected loop variable").text;
        expect(TokenType.IN, "Expected 'in'");
        Node.Expr iterable = parseExpr();
        Node.Block body = parseBlock();
        return new Node.ForIn(varName, iterable, body);
    }

    private Node.Stmt parseReturn() {
        advance();
        if (check(TokenType.NEWLINE) || check(TokenType.RBRACE) || check(TokenType.SEMI) || isAtEnd()) {
            return new Node.Return(null);
        }
        Node.Expr v = parseExpr();
        return new Node.Return(v);
    }

    private Node.Stmt parseShow() {
        advance();
        expect(TokenType.LPAREN, "Expected '(' after show");
        List<Node.Expr> args = parseArgList();
        expect(TokenType.RPAREN, "Expected ')'");
        return new Node.ShowStmt(args);
    }

    private Node.Stmt parsePrint() {
        advance();
        expect(TokenType.LPAREN, "Expected '(' after print");
        List<Node.Expr> args = parseArgList();
        expect(TokenType.RPAREN, "Expected ')'");
        return new Node.PrintStmt(args);
    }

    private Node.Stmt parseSeal() {
        advance();
        List<Node.Expr> args = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            args = parseArgList();
            expect(TokenType.RPAREN, "Expected ')'");
        }
        return new Node.SealStmt(args);
    }

    private Node.Stmt parseHtml() {
        advance();
        Node.Expr v = parseExpr();
        return new Node.HtmlStmt(v);
    }

    private List<Node.Expr> parseArgList() {
        List<Node.Expr> args = new ArrayList<>();
        while (!check(TokenType.RPAREN) && !isAtEnd()) {
            args.add(parseExpr());
            if (!match(TokenType.COMMA)) break;
        }
        return args;
    }

    private Node.Stmt parseClassDecl() {
        advance();
        String name = expect(TokenType.IDENT, "Expected class name").text;
        String superName = null;
        if (match(TokenType.EXTENDS)) superName = expect(TokenType.IDENT, "Expected superclass name").text;
        if (check(TokenType.IMPORT) || peek().text.equals("implements")) {
            // tolerate "implements X, Y" — skip till '{'
        }
        while (!check(TokenType.LBRACE) && !isAtEnd()) advance();
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<Node.FuncDecl> methods = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (check(TokenType.FUNC)) {
                advance();
                methods.add((Node.FuncDecl) parseFuncDeclBody(false));
            } else {
                advance(); // tolerate stray tokens
            }
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.ClassDecl(name, superName, methods);
    }

    private Node.Stmt parseModuleDecl() {
        advance();
        String name = expect(TokenType.IDENT, "Expected module name").text;
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<Node.Stmt> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            body.add(parseStmt());
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.ModuleDecl(name, body);
    }

    private Node.Stmt parseEnumDecl() {
        advance();
        String name = expect(TokenType.IDENT, "Expected enum name").text;
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<String> values = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            values.add(expect(TokenType.IDENT, "Expected enum member").text);
            match(TokenType.COMMA);
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.EnumDecl(name, values);
    }

    private Node.Stmt parseTryCatch() {
        advance();
        Node.Block tryB = parseBlock();
        skipNewlines();
        expect(TokenType.CATCH, "Expected 'catch'");
        String errName = null;
        if (check(TokenType.IDENT)) errName = advance().text;
        Node.Block catchB = parseBlock();
        return new Node.TryCatch(tryB, errName, catchB);
    }

    private Node.Stmt parseGuard() {
        advance();
        Node.Expr cond = parseExpr();
        expect(TokenType.ELSE, "Expected 'else' after guard condition");
        Node.Block elseB = parseBlock();
        return new Node.GuardStmt(cond, elseB);
    }

    private Node.Stmt parseMatch() {
        advance();
        Node.Expr value = parseExpr();
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<Node.Expr> whenValues = new ArrayList<>();
        List<Node.Stmt> whenBodies = new ArrayList<>();
        while (check(TokenType.WHEN)) {
            advance();
            whenValues.add(parseExpr());
            if (check(TokenType.ARROW) || check(TokenType.COLON)) advance();
            whenBodies.add(parseStmt());
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.MatchStmt(value, whenValues, whenBodies);
    }

    private Node.Stmt parseTest() {
        advance();
        String name = "test";
        if (check(TokenType.STRING)) name = (String) advance().literal;
        Node.Block body = parseBlock();
        return new Node.TestStmt(name, body);
    }

    private Node.Stmt parseAssert() {
        advance();
        expect(TokenType.LPAREN, "Expected '('");
        Node.Expr cond = parseExpr();
        Node.Expr msg = null;
        if (match(TokenType.COMMA)) msg = parseExpr();
        expect(TokenType.RPAREN, "Expected ')'");
        List<Node.Expr> args = new ArrayList<>();
        args.add(cond);
        if (msg != null) args.add(msg);
        return new Node.ExprStmt(new Node.Call(new Node.VarExpr("assert"), args));
    }

    private Node.Stmt parseTypeAlias() {
        advance();
        String name = expect(TokenType.IDENT, "Expected type name").text;
        Node.Expr value = null;
        if (match(TokenType.ASSIGN)) value = parseExpr();
        return new Node.TypeAlias(name, value);
    }

    private Node.Stmt parseStructDecl() {
        advance();
        String name = expect(TokenType.IDENT, "Expected struct name").text;
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<String> fields = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            fields.add(expect(TokenType.IDENT, "Expected field name").text);
            match(TokenType.COMMA);
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.StructDecl(name, fields);
    }

    private Node.Stmt parsePatternDecl() {
        advance(); // pattern
        String typeName = expect(TokenType.IDENT, "Expected pattern type (e.g. Balanced)").text;
        String patternName = (String) expect(TokenType.STRING, "Expected pattern name string").literal;
        expect(TokenType.LBRACE, "Expected '{'");
        skipNewlines();
        List<String> keys = new ArrayList<>();
        List<Node.Expr> values = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            String key = expect(TokenType.IDENT, "Expected pattern field name").text;
            expect(TokenType.COLON, "Expected ':'");
            Node.Expr val = parseExpr();
            keys.add(key);
            values.add(val);
            match(TokenType.COMMA);
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.PatternDecl(typeName, patternName, keys, values);
    }

    private Node.Stmt parseRenderStmt() {
        advance(); // render
        Node.Expr target = parsePostfix(parsePrimary());
        Node.Block body = null;
        if (check(TokenType.LBRACE)) body = parseBlock();
        return new Node.RenderStmt(target, body);
    }

    // ---------- expression-or-assignment ----------
    private Node.Stmt parseExprOrAssignStmt() {
        Node.Expr expr = parseExpr();
        if (check(TokenType.ASSIGN) || check(TokenType.PLUS_EQ) || check(TokenType.MINUS_EQ)
                || check(TokenType.STAR_EQ) || check(TokenType.SLASH_EQ)) {
            String op = advance().text;
            Node.Expr value = parseExpr();
            return new Node.Assign(expr, op, value);
        }
        return new Node.ExprStmt(expr);
    }

    // ---------- expressions (precedence climbing) ----------
    public Node.Expr parseExpr() { return parsePipeline(); }

    private Node.Expr parsePipeline() {
        Node.Expr left = parseTernary();
        while (check(TokenType.PIPE) && peek().text.equals("|>")) {
            advance();
            Node.Expr right = parseTernary();
            left = new Node.Pipeline(left, right);
        }
        return left;
    }

    private Node.Expr parseTernary() {
        Node.Expr cond = parseOr();
        if (match(TokenType.QUESTION)) {
            Node.Expr then = parseExpr();
            expect(TokenType.COLON, "Expected ':' in ternary");
            Node.Expr otherwise = parseExpr();
            return new Node.Ternary(cond, then, otherwise);
        }
        return cond;
    }

    private Node.Expr parseOr() {
        Node.Expr left = parseAnd();
        while (check(TokenType.OR)) { advance(); left = new Node.Logical("or", left, parseAnd()); }
        return left;
    }

    private Node.Expr parseAnd() {
        Node.Expr left = parseNot();
        while (check(TokenType.AND)) { advance(); left = new Node.Logical("and", left, parseNot()); }
        return left;
    }

    private Node.Expr parseNot() {
        if (check(TokenType.NOT)) { advance(); return new Node.Unary("not", parseNot()); }
        return parseEquality();
    }

    private Node.Expr parseEquality() {
        Node.Expr left = parseComparison();
        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            String op = advance().text;
            left = new Node.Binary(op, left, parseComparison());
        }
        return left;
    }

    private Node.Expr parseComparison() {
        Node.Expr left = parseRange();
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) {
            String op = advance().text;
            left = new Node.Binary(op, left, parseRange());
        }
        return left;
    }

    private Node.Expr parseRange() {
        Node.Expr left = parseBitwise();
        if (check(TokenType.DOTDOT)) {
            advance();
            Node.Expr right = parseBitwise();
            return new Node.RangeExpr(left, right);
        }
        return left;
    }

    private Node.Expr parseBitwise() {
        Node.Expr left = parseAdditive();
        while (check(TokenType.AMP) || check(TokenType.SHL) || check(TokenType.SHR)
                || (check(TokenType.PIPE) && peek().text.equals("|"))) {
            String op = advance().text;
            left = new Node.Binary(op, left, parseAdditive());
        }
        return left;
    }

    private Node.Expr parseAdditive() {
        Node.Expr left = parseMultiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = advance().text;
            left = new Node.Binary(op, left, parseMultiplicative());
        }
        return left;
    }

    private Node.Expr parseMultiplicative() {
        Node.Expr left = parsePower();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            String op = advance().text;
            left = new Node.Binary(op, left, parsePower());
        }
        return left;
    }

    private Node.Expr parsePower() {
        Node.Expr left = parseUnary();
        if (check(TokenType.POW)) {
            advance();
            Node.Expr right = parsePower(); // right-assoc
            return new Node.Binary("**", left, right);
        }
        return left;
    }

    private Node.Expr parseUnary() {
        if (check(TokenType.MINUS)) { advance(); return new Node.Unary("-", parseUnary()); }
        if (check(TokenType.PLUS)) { advance(); return parseUnary(); }
        return parsePostfix(parsePrimary());
    }

    private Node.Expr parsePostfix(Node.Expr expr) {
        while (true) {
            if (check(TokenType.LPAREN)) {
                advance();
                List<Node.Expr> args = parseArgList();
                expect(TokenType.RPAREN, "Expected ')'");
                expr = new Node.Call(expr, args);
            } else if (check(TokenType.DOT)) {
                advance();
                String name = advance().text; // allow keywords as method names too
                expr = new Node.GetProp(expr, name);
            } else if (check(TokenType.LBRACKET)) {
                advance();
                Node.Expr idx = parseExpr();
                expect(TokenType.RBRACKET, "Expected ']'");
                expr = new Node.Index(expr, idx);
            } else {
                break;
            }
        }
        return expr;
    }

    private Node.Expr parsePrimary() {
        Token t = peek();
        switch (t.type) {
            case NUMBER: advance(); return new Node.Literal(t.literal);
            case STRING: advance(); return new Node.Literal(t.literal);
            case TRUE: advance(); return new Node.Literal(Boolean.TRUE);
            case FALSE: advance(); return new Node.Literal(Boolean.FALSE);
            case NIL: advance(); return new Node.Literal(null);
            case SELF: advance(); return new Node.SelfExpr();
            case NEW: return parseNewExpr();
            case BACKSLASH: return parseLambda();
            case LPAREN: {
                advance();
                Node.Expr e = parseExpr();
                expect(TokenType.RPAREN, "Expected ')'");
                return e;
            }
            case LBRACKET: return parseArrayLit();
            case LBRACE: return parseObjectLit();
            case IDENT:
            case SHOW:    // identifiers that happen to be keywords used as values (e.g. Math2.PI)
            case PRINT:
            case HTML:
                advance();
                return new Node.VarExpr(t.text);
            default:
                throw new ParseError("Unexpected token " + t + " at line " + t.line);
        }
    }

    private Node.Expr parseNewExpr() {
        advance(); // new
        String name = expect(TokenType.IDENT, "Expected class name after 'new'").text;
        List<Node.Expr> args = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            args = parseArgList();
            expect(TokenType.RPAREN, "Expected ')'");
        }
        return new Node.NewExpr(name, args);
    }

    private Node.Expr parseLambda() {
        advance(); // backslash
        List<String> params = new ArrayList<>();
        if (check(TokenType.IDENT)) {
            params.add(advance().text);
            while (match(TokenType.COMMA)) params.add(expect(TokenType.IDENT, "param").text);
        }
        expect(TokenType.ARROW, "Expected '->' in lambda");
        if (check(TokenType.LBRACE)) {
            Node.Block body = parseBlock();
            return new Node.Lambda(params, body.stmts, null);
        } else {
            Node.Expr body = parseExpr();
            return new Node.Lambda(params, null, body);
        }
    }

    private Node.Expr parseArrayLit() {
        advance(); // [
        List<Node.Expr> elems = new ArrayList<>();
        while (!check(TokenType.RBRACKET) && !isAtEnd()) {
            elems.add(parseExpr());
            if (!match(TokenType.COMMA)) break;
        }
        expect(TokenType.RBRACKET, "Expected ']'");
        return new Node.ArrayLit(elems);
    }

    private Node.Expr parseObjectLit() {
        advance(); // {
        skipNewlines();
        List<String> keys = new ArrayList<>();
        List<Node.Expr> values = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            String key;
            if (check(TokenType.STRING)) key = (String) advance().literal;
            else key = advance().text;
            expect(TokenType.COLON, "Expected ':' in object literal");
            Node.Expr val = parseExpr();
            keys.add(key);
            values.add(val);
            match(TokenType.COMMA);
            skipNewlines();
        }
        expect(TokenType.RBRACE, "Expected '}'");
        return new Node.ObjectLit(keys, values);
    }
}
