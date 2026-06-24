package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexer — turns raw VCG source text into a stream of Tokens.
 * Supports:
 *   - line comments:  #  and  //
 *   - block comments: /* ... * /
 *   - string literals with \n \t \" \\ escapes
 *   - integer / float literals
 *   - identifiers (ASCII + Unicode, so Arabic identifiers work too)
 *   - all VCG operators & punctuation
 */
public final class Lexer {

    private final String src;
    private int pos = 0;
    private int line = 1;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("let", TokenType.LET);
        KEYWORDS.put("const", TokenType.CONST);
        KEYWORDS.put("w", TokenType.W);
        KEYWORDS.put("func", TokenType.FUNC);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("repeat", TokenType.REPEAT);
        KEYWORDS.put("match", TokenType.MATCH);
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("class", TokenType.CLASS);
        KEYWORDS.put("extends", TokenType.EXTENDS);
        KEYWORDS.put("new", TokenType.NEW);
        KEYWORDS.put("self", TokenType.SELF);
        KEYWORDS.put("module", TokenType.MODULE);
        KEYWORDS.put("from", TokenType.FROM);
        KEYWORDS.put("import", TokenType.IMPORT);
        KEYWORDS.put("enum", TokenType.ENUM);
        KEYWORDS.put("try", TokenType.TRY);
        KEYWORDS.put("catch", TokenType.CATCH);
        KEYWORDS.put("throw", TokenType.THROW);
        KEYWORDS.put("safe", TokenType.SAFE);
        KEYWORDS.put("unsafe", TokenType.UNSAFE);
        KEYWORDS.put("guard", TokenType.GUARD);
        KEYWORDS.put("test", TokenType.TEST);
        KEYWORDS.put("assert", TokenType.ASSERT);
        KEYWORDS.put("break", TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
        KEYWORDS.put("async", TokenType.ASYNC);
        KEYWORDS.put("await", TokenType.AWAIT);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("struct", TokenType.STRUCT);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("nil", TokenType.NIL);
        KEYWORDS.put("Seal", TokenType.SEAL);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("show", TokenType.SHOW);
        KEYWORDS.put("html", TokenType.HTML);
        KEYWORDS.put("pattern", TokenType.PATTERN);
        KEYWORDS.put("render", TokenType.RENDER);
        KEYWORDS.put("c", TokenType.C_CHANNEL);
    }

    public Lexer(String src) {
        this.src = src;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() { return pos >= src.length(); }
    private char peek() { return isAtEnd() ? '\0' : src.charAt(pos); }
    private char peekNext() { return pos + 1 >= src.length() ? '\0' : src.charAt(pos + 1); }
    private char advance() { return src.charAt(pos++); }

    private boolean match(char expected) {
        if (isAtEnd() || src.charAt(pos) != expected) return false;
        pos++;
        return true;
    }

    private void add(TokenType type, String text) {
        tokens.add(new Token(type, text, null, line));
    }

    private void add(TokenType type, String text, Object literal) {
        tokens.add(new Token(type, text, literal, line));
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ': case '\t': case '\r': return;
            case '\n':
                add(TokenType.NEWLINE, "\\n");
                line++;
                return;
            case '#':
                while (peek() != '\n' && !isAtEnd()) advance();
                return;
            case '"':
                scanString();
                return;
            case '(': add(TokenType.LPAREN, "("); return;
            case ')': add(TokenType.RPAREN, ")"); return;
            case '{': add(TokenType.LBRACE, "{"); return;
            case '}': add(TokenType.RBRACE, "}"); return;
            case '[': add(TokenType.LBRACKET, "["); return;
            case ']': add(TokenType.RBRACKET, "]"); return;
            case ',': add(TokenType.COMMA, ","); return;
            case ':': add(TokenType.COLON, ":"); return;
            case ';': add(TokenType.SEMI, ";"); return;
            case '\\': add(TokenType.BACKSLASH, "\\"); return;
            case '?': add(TokenType.QUESTION, "?"); return;
            case '.':
                if (match('.')) { add(TokenType.DOTDOT, ".."); }
                else { add(TokenType.DOT, "."); }
                return;
            case '+':
                if (match('=')) add(TokenType.PLUS_EQ, "+=");
                else add(TokenType.PLUS, "+");
                return;
            case '-':
                if (match('=')) add(TokenType.MINUS_EQ, "-=");
                else if (match('>')) add(TokenType.ARROW, "->");
                else add(TokenType.MINUS, "-");
                return;
            case '*':
                if (match('*')) add(TokenType.POW, "**");
                else if (match('=')) add(TokenType.STAR_EQ, "*=");
                else add(TokenType.STAR, "*");
                return;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
                        if (peek() == '\n') line++;
                        advance();
                    }
                    if (!isAtEnd()) { advance(); advance(); } // consume */
                } else if (match('=')) {
                    add(TokenType.SLASH_EQ, "/=");
                } else {
                    add(TokenType.SLASH, "/");
                }
                return;
            case '%': add(TokenType.PERCENT, "%"); return;
            case '=':
                if (match('=')) add(TokenType.EQ, "==");
                else if (match('>')) add(TokenType.FATARROW, "=>");
                else add(TokenType.ASSIGN, "=");
                return;
            case '!':
                if (match('=')) add(TokenType.NEQ, "!=");
                else throw new LexError("Unexpected '!' at line " + line);
                return;
            case '<':
                if (match('=')) add(TokenType.LE, "<=");
                else if (match('<')) add(TokenType.SHL, "<<");
                else add(TokenType.LT, "<");
                return;
            case '>':
                if (match('=')) add(TokenType.GE, ">=");
                else if (match('>')) add(TokenType.SHR, ">>");
                else add(TokenType.GT, ">");
                return;
            case '|':
                if (match('>')) add(TokenType.PIPE, "|>");
                else add(TokenType.PIPE, "|");
                return;
            case '&': add(TokenType.AMP, "&"); return;
            default:
                if (Character.isDigit(c)) {
                    scanNumber();
                } else if (isIdentStart(c)) {
                    scanIdent();
                } else {
                    throw new LexError("Unexpected character '" + c + "' at line " + line);
                }
        }
    }

    private boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private void scanIdent() {
        int start = pos - 1;
        while (isIdentPart(peek())) advance();
        String text = src.substring(start, pos);
        TokenType kw = KEYWORDS.get(text);
        if (kw != null) {
            add(kw, text);
        } else {
            add(TokenType.IDENT, text);
        }
    }

    private void scanNumber() {
        int start = pos - 1;
        while (Character.isDigit(peek())) advance();
        boolean isFloat = false;
        if (peek() == '.' && Character.isDigit(peekNext())) {
            isFloat = true;
            advance();
            while (Character.isDigit(peek())) advance();
        }
        String text = src.substring(start, pos);
        if (isFloat) {
            add(TokenType.NUMBER, text, Double.parseDouble(text));
        } else {
            add(TokenType.NUMBER, text, (double) Long.parseLong(text));
        }
    }

    private void scanString() {
        StringBuilder sb = new StringBuilder();
        int startLine = line;
        while (peek() != '"' && !isAtEnd()) {
            char ch = advance();
            if (ch == '\\') {
                char esc = advance();
                switch (esc) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '$': sb.append('$'); break;
                    default: sb.append(esc);
                }
            } else {
                if (ch == '\n') line++;
                sb.append(ch);
            }
        }
        if (isAtEnd()) throw new LexError("Unterminated string starting at line " + startLine);
        advance(); // closing quote
        add(TokenType.STRING, sb.toString(), sb.toString());
    }

    public static final class LexError extends RuntimeException {
        public LexError(String msg) { super(msg); }
    }
}
