package com.syrianvcg.vcgsdk;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════
 *  VcgHighlighter  —  Syntax Token Classifier for VCG
 *
 *  Returns a list of VcgToken objects, each with:
 *    - text    (the raw text)
 *    - type    (KEYWORD | BUILTIN | STRING | NUMBER |
 *               COMMENT | OPERATOR | IDENTIFIER | OTHER)
 *
 *  Designed for use in syntax-highlighting editors.
 * ═══════════════════════════════════════════════════════
 */
public class VcgHighlighter {

    public enum TokenType {
        KEYWORD, BUILTIN, STRING, NUMBER,
        COMMENT, OPERATOR, IDENTIFIER, OTHER
    }

    public static class Token {
        public final String    text;
        public final TokenType type;
        public Token(String text, TokenType type) {
            this.text = text; this.type = type;
        }
        @Override public String toString() {
            return "[" + type + "] " + text;
        }
    }

    /**
     * Tokenize a line of VCG code into typed tokens.
     */
    public static List<Token> tokenizeLine(String line) {
        List<Token> tokens = new ArrayList<>();
        int i = 0, n = line.length();

        while (i < n) {
            char c = line.charAt(i);

            // Single-line comment  # or //
            if (c == '#' || (c == '/' && i + 1 < n && line.charAt(i+1) == '/')) {
                tokens.add(new Token(line.substring(i), TokenType.COMMENT));
                break;
            }

            // String literal " or '
            if (c == '"' || c == '\'') {
                char q = c;
                int start = i++;
                while (i < n) {
                    if (line.charAt(i) == '\\') { i += 2; continue; }
                    if (line.charAt(i) == q)    { i++; break; }
                    i++;
                }
                tokens.add(new Token(line.substring(start, i), TokenType.STRING));
                continue;
            }

            // Number
            if (Character.isDigit(c) || (c == '-' && i+1 < n && Character.isDigit(line.charAt(i+1)))) {
                int start = i++;
                while (i < n && (Character.isDigit(line.charAt(i)) || line.charAt(i) == '.')) i++;
                tokens.add(new Token(line.substring(start, i), TokenType.NUMBER));
                continue;
            }

            // Identifier or keyword
            if (Character.isLetter(c) || c == '_' || c == '$') {
                int start = i++;
                while (i < n && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) i++;
                String word = line.substring(start, i);
                TokenType type;
                if      (VcgKeywords.isKeyword(word)) type = TokenType.KEYWORD;
                else if (VcgKeywords.isBuiltin(word)) type = TokenType.BUILTIN;
                else                                   type = TokenType.IDENTIFIER;
                tokens.add(new Token(word, type));
                continue;
            }

            // Operator / punctuation
            if ("+-*/%=<>!&|^~?.,:;()[]{}".indexOf(c) >= 0) {
                tokens.add(new Token(String.valueOf(c), TokenType.OPERATOR));
                i++;
                continue;
            }

            // Whitespace / other
            tokens.add(new Token(String.valueOf(c), TokenType.OTHER));
            i++;
        }
        return tokens;
    }

    /**
     * Returns an HTML-highlighted version of a VCG code string.
     * Uses inline <span class="vcg-*"> tags.
     */
    public static String toHtml(String vcgCode) {
        StringBuilder sb = new StringBuilder();
        for (String line : vcgCode.split("\n", -1)) {
            for (Token tok : tokenizeLine(line)) {
                String esc = tok.text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
                sb.append("<span class=\"vcg-")
                  .append(tok.type.name().toLowerCase())
                  .append("\">")
                  .append(esc)
                  .append("</span>");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns CSS rules for the vcg-* span classes.
     */
    public static String getHighlightCss(String theme) {
        boolean dark = !"white".equals(theme);
        return
            ".vcg-keyword  { color: " + (dark ? "#4dc95a" : "#1f7a3d") + "; font-weight:700; }\n" +
            ".vcg-builtin  { color: " + (dark ? "#e0a84d" : "#c06010") + "; }\n" +
            ".vcg-string   { color: " + (dark ? "#f4965a" : "#a03030") + "; }\n" +
            ".vcg-number   { color: " + (dark ? "#5b8cff" : "#2040b0") + "; }\n" +
            ".vcg-comment  { color: " + (dark ? "#4a6a4a" : "#6b7568") + "; font-style:italic; }\n" +
            ".vcg-operator { color: " + (dark ? "#e6ecff" : "#333333") + "; }\n" +
            ".vcg-identifier { color: " + (dark ? "#e8f5e0" : "#1b221c") + "; }\n" +
            ".vcg-other    { color: " + (dark ? "#e8f5e0" : "#1b221c") + "; }\n";
    }
}
