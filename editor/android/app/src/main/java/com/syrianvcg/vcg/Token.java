package com.syrianvcg.vcg;

public final class Token {
    public final TokenType type;
    public final String text;
    public final Object literal;
    public final int line;

    public Token(TokenType type, String text, Object literal, int line) {
        this.type = type;
        this.text = text;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}
