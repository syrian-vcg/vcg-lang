package com.syrianvcg.vcgsdk;

/**
 * VCG Language Keywords &amp; Built-in Functions Registry
 * Used for syntax highlighting, autocomplete, and IDE tooling.
 */
public final class VcgKeywords {

    private VcgKeywords() {}

    /** All VCG reserved keywords */
    public static final String[] ALL = {
        // Core
        "let", "const", "func", "return", "if", "else", "while", "for", "in",
        "repeat", "break", "continue", "show", "input", "and", "or", "not",
        "html", "true", "false", "nil", "null", "import", "as", "struct",
        "new", "self", "typeof", "sizeof", "assert", "try", "catch", "throw",
        "match", "when",
        // Reactive
        "$set", "$get", "public", "w", "x", "c", "watch",
        // UI / Media
        "youtube", "facebook", "instagram", "xsocial", "url", "btn", "key",
        "video", "img", "h", "l",
        // OOP
        "class", "extends", "implements", "interface", "super", "this",
        // Modules
        "module", "export", "from",
        // Async
        "async", "await", "promise", "defer",
        // Types
        "type", "enum", "union", "generic",
        // File I/O
        "file", "read", "write", "append",
        // Network
        "http", "request", "response", "socket",
        // Memory
        "ref", "ptr", "alloc", "free",
        // Safety
        "safe", "unsafe", "guard",
        // Functional
        "map", "filter", "reduce", "find",
        // Testing / Docs
        "doc", "test", "expect", "mock",
        // Context
        "with", "case", "pipeline", "lambda"
    };

    /** VCG built-in functions (stdlib) */
    public static final String[] BUILTINS = {
        // Math
        "abs", "floor", "ceil", "round", "sqrt", "sin", "cos", "tan",
        "log", "log2", "log10", "pow", "min", "max", "clamp", "rand",
        "gcd", "lcm", "fib", "factorial", "is_prime",
        // Array
        "range", "len", "flat", "unique", "sum", "avg", "first", "last",
        "chunk", "zip", "sort",
        // String
        "str", "int", "float", "bool", "char", "ord", "repeat",
        "pad_start", "pad_end", "includes", "indexof", "count", "format",
        // Type checking
        "typeof", "sizeof", "isnil", "isnum", "isstr", "isarr", "defined",
        // Object
        "keys", "values", "entries", "has", "del", "merge", "copy", "freeze",
        // Functional
        "pipe", "send", "recv",
        // JSON
        "JSON_stringify", "JSON_parse",
        // Assert
        "assert_eq", "assert_ne", "assert_true", "assert_false",
        // Util
        "uuid", "hash", "sleep", "show", "print", "input",
        // Constants
        "PI", "E", "TAU", "PHI", "INF",
        "VCG_VERSION", "VCG_DATE", "VCG_EDITION"
    };

    /** Check if a token is a keyword */
    public static boolean isKeyword(String token) {
        for (String kw : ALL) {
            if (kw.equals(token)) return true;
        }
        return false;
    }

    /** Check if a token is a built-in function */
    public static boolean isBuiltin(String token) {
        for (String b : BUILTINS) {
            if (b.equals(token)) return true;
        }
        return false;
    }
}
