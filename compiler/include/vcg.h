#ifndef VCG_H
#define VCG_H

/* ================================================================
   Syrian Private Programming VCG  —  v1.0
   Main header: types, tokens, AST, runtime
   Date: 2026-06-06  (Syrian VCG Language Project)
   ================================================================ */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#include <ctype.h>

/* ── Version ── */
#define VCG_VERSION_MAJOR 1
#define VCG_VERSION_MINOR 0
#define VCG_VERSION_PATCH 0
#define VCG_VERSION_STR   "1.0.0"
#define VCG_RELEASE_DATE  "2026-06-06"
#define VCG_COPYRIGHT     "Syrian VCG Project 2026"

/* ── Limits ── */
#define VCG_MAX_IDENT     256
#define VCG_MAX_STRING    4096
#define VCG_MAX_PARAMS    32
#define VCG_MAX_LOCALS    256
#define VCG_MAX_CALL_DEPTH 512
#define VCG_MAX_ARRAY     65536

/* ================================================================
   TOKEN TYPES
   ================================================================ */
typedef enum {
    /* Literals */
    TK_INT, TK_FLOAT, TK_STRING, TK_TRUE, TK_FALSE, TK_NIL,

    /* Identifiers */
    TK_IDENT,

    /* Keywords */
    TK_LET, TK_CONST, TK_FUNC, TK_RETURN, TK_IF, TK_ELSE,
    TK_WHILE, TK_FOR, TK_IN, TK_REPEAT, TK_BREAK, TK_CONTINUE,
    TK_SHOW, TK_INPUT, TK_AND, TK_OR, TK_NOT, TK_HTML,
    TK_IMPORT, TK_AS, TK_STRUCT, TK_NEW, TK_SELF, TK_NULL,
    TK_TYPEOF, TK_SIZEOF, TK_ASSERT, TK_TRY, TK_CATCH,
    TK_THROW, TK_MATCH, TK_WHEN, TK_ARROW,

    /* Operators */
    TK_PLUS, TK_MINUS, TK_STAR, TK_SLASH, TK_PERCENT,
    TK_POWER,          /* ** */
    TK_EQ,  TK_NEQ,
    TK_LT,  TK_GT, TK_LTE, TK_GTE,
    TK_ASSIGN, TK_PLUS_ASSIGN, TK_MINUS_ASSIGN,
    TK_STAR_ASSIGN, TK_SLASH_ASSIGN,
    TK_INC, TK_DEC,    /* ++ -- */
    TK_PIPE,           /* | */
    TK_AMP,            /* & */
    TK_TILDE,          /* ~ */
    TK_LSHIFT, TK_RSHIFT,

    /* Punctuation */
    TK_LPAREN, TK_RPAREN, TK_LBRACE, TK_RBRACE,
    TK_LBRACKET, TK_RBRACKET,
    TK_COMMA, TK_SEMICOLON, TK_COLON, TK_DOT, TK_DOTDOT,
    TK_QUESTION, TK_AT,

    /* Special */
    /* v3.0 new concepts */
    TK_SET,        /* $set  — reactive setter     */
    TK_GET,        /* $get  — reactive getter     */
    TK_PUBLIC,     /* public — export symbol      */
    TK_W,          /* w     — write-only var      */
    TK_X,          /* x     — execute/run expr   */
    TK_C,          /* c     — channel/pipe var   */
    TK_DOLLAR,     /* $     — sigil prefix        */

    /* ── v1.0 UI/Media/Web keywords ── */
    TK_YOUTUBE,    /* youtube  — embed YouTube video */
    TK_FACEBOOK,   /* facebook — Facebook link/share */
    TK_INSTAGRAM,  /* instagram— Instagram link      */
    TK_XSOCIAL,    /* xsocial  — X/Twitter link      */
    TK_URL,        /* url      — hyperlink           */
    TK_BTN,        /* btn      — button element      */
    TK_KEY,        /* key      — keyboard shortcut   */
    TK_VIDEO,      /* video    — video embed         */
    TK_IMG,        /* img      — image element       */
    TK_H,          /* h        — heading (h1-h6)     */
    TK_L,          /* l        — list item           */
    TK_NEWLINE, TK_EOF, TK_ERROR
} TokenType;

typedef struct {
    TokenType  type;
    char       sval[VCG_MAX_STRING];
    double     nval;
    int        line;
    int        col;
    const char *file;
} Token;

/* ================================================================
   AST NODE TYPES
   ================================================================ */
typedef enum {
    /* Literals */
    ND_INT, ND_FLOAT, ND_STRING, ND_BOOL, ND_NIL, ND_ARRAY, ND_STRUCT_LIT,

    /* Identifiers / access */
    ND_IDENT, ND_INDEX, ND_FIELD,

    /* Operations */
    ND_BINOP, ND_UNOP, ND_TERNARY,

    /* Statements */
    ND_BLOCK, ND_LET, ND_CONST, ND_ASSIGN, ND_OP_ASSIGN,
    ND_IF, ND_WHILE, ND_FOR_IN, ND_REPEAT,
    ND_BREAK, ND_CONTINUE, ND_RETURN,
    ND_SHOW, ND_INPUT, ND_HTML,
    ND_ASSERT, ND_THROW, ND_TRY_CATCH,

    /* Functions */
    ND_FUNC, ND_LAMBDA, ND_CALL, ND_METHOD_CALL,

    /* Structs */
    ND_STRUCT, ND_NEW,

    /* Match */
    ND_MATCH,

    /* Import */
    ND_IMPORT,

    /* Built-ins */
    ND_TYPEOF, ND_SIZEOF,

    /* Program root */
    /* v3.0 nodes */
    ND_SET,        /* $set(key, val)  */
    ND_GET,        /* $get(key)       */
    ND_PUBLIC,     /* public decl     */
    ND_W,          /* w name = expr   */
    ND_X,          /* x expr          */
    ND_C,          /* c name          */
    ND_CHANNEL_SEND,  /* name <- expr */
    ND_CHANNEL_RECV,  /* <-name       */

    /* ── v1.0 UI/Media nodes ── */
    ND_YOUTUBE,    ND_FACEBOOK,   ND_INSTAGRAM,
    ND_XSOCIAL,    ND_URL,        ND_BTN,
    ND_KEY,        ND_VIDEO,      ND_IMG,
    ND_H,          ND_L,
    ND_PROGRAM
} NodeType;

/* ── AST Node ── */
struct Node;
typedef struct Node Node;

typedef struct {
    char  *key;
    Node  *val;
} FieldInit;

struct Node {
    NodeType    type;
    int         line;
    const char *file;

    /* Scalars */
    double      num;
    char       *str;
    int         bval;
    char        op[16];     /* operator string */

    /* Children */
    Node       *left;
    Node       *right;
    Node       *cond;
    Node       *body;
    Node       *alt;       /* else / catch */
    Node       *init;      /* for/let init */
    Node       *update;

    /* Lists */
    Node      **children;
    int         nchildren;
    int         cap;

    /* Function */
    char       *name;
    char      **params;
    int        *param_defaults_mask;
    Node      **param_defaults;
    int         nparams;
    int         variadic;  /* last param is ... */

    /* Struct */
    char      **fields;
    int         nfields;
    FieldInit  *finits;
    int         nfinits;

    /* Match arms */
    Node      **arms_cond;
    Node      **arms_body;
    int         narms;
};

Node *nd_new(NodeType type, int line);
void  nd_add_child(Node *parent, Node *child);
void  nd_free(Node *n);

/* ================================================================
   LEXER
   ================================================================ */
typedef struct {
    const char *src;
    int         pos;
    int         line;
    int         col;
    const char *file;
} Lexer;

void  lex_init(Lexer *l, const char *src, const char *file);
Token lex_next(Lexer *l);
Token lex_peek(Lexer *l);

/* ================================================================
   PARSER
   ================================================================ */
typedef struct {
    Lexer  lex;
    Token  cur;
    Token  ahead;          /* one lookahead */
    int    in_func;
    int    in_loop;
} Parser;

void  parse_init(Parser *p, const char *src, const char *file);
Node *parse_program(Parser *p);

/* ================================================================
   RUNTIME VALUE
   ================================================================ */
typedef enum {
    VT_NIL, VT_BOOL, VT_INT, VT_FLOAT, VT_STRING,
    VT_ARRAY, VT_FUNC, VT_STRUCT, VT_BUILTIN
} ValType;

struct VCGVal;
typedef struct VCGVal VCGVal;
typedef struct VCGEnv VCGEnv;

typedef struct VCGArray {
    VCGVal *items;
    int     len;
    int     cap;
    int     refs;
} VCGArray;

typedef struct VCGStruct {
    char   **keys;
    VCGVal  *vals;
    int      len;
    int      cap;
    int      refs;
    char    *type_name;
} VCGStruct;

typedef struct {
    Node    *body;
    char   **params;
    int      nparams;
    int      variadic;
    VCGEnv  *closure;
    char    *name;
} VCGFunc;

typedef VCGVal (*VCGBuiltinFn)(VCGVal *args, int nargs, int line);

struct VCGVal {
    ValType    type;
    union {
        int         ival;
        double      fval;
        char       *sval;
        int         bval;
        VCGArray   *arr;
        VCGStruct  *obj;
        VCGFunc    *fn;
        VCGBuiltinFn builtin;
    };
};

#define VCG_NIL   ((VCGVal){.type=VT_NIL})
#define VCG_TRUE  ((VCGVal){.type=VT_BOOL,.bval=1})
#define VCG_FALSE ((VCGVal){.type=VT_BOOL,.bval=0})
#define VCG_INT(n)   ((VCGVal){.type=VT_INT,.ival=(int)(n)})
#define VCG_FLOAT(n) ((VCGVal){.type=VT_FLOAT,.fval=(double)(n)})
#define VCG_BOOL(b)  ((VCGVal){.type=VT_BOOL,.bval=!!(b)})

VCGVal vcg_str(const char *s);
VCGVal vcg_arr_new(void);
VCGVal vcg_struct_new(const char *type_name);
int    vcg_truthy(VCGVal v);
int    vcg_equal(VCGVal a, VCGVal b);
char  *vcg_tostr(VCGVal v);
ValType vcg_typeof(VCGVal v);
void   vcg_val_free(VCGVal *v);

/* ================================================================
   ENVIRONMENT (scope)
   ================================================================ */
typedef struct VCGBinding {
    char   name[VCG_MAX_IDENT];
    VCGVal val;
    int    is_const;
} VCGBinding;

struct VCGEnv {
    VCGBinding  *bindings;
    int          len;
    int          cap;
    VCGEnv      *parent;
    int          refs;
};

VCGEnv *env_new(VCGEnv *parent);
void    env_set(VCGEnv *e, const char *name, VCGVal val, int is_const);
int     env_assign(VCGEnv *e, const char *name, VCGVal val);
VCGVal *env_get(VCGEnv *e, const char *name);
void    env_free(VCGEnv *e);

/* ================================================================
   INTERPRETER
   ================================================================ */
typedef enum {
    SIG_NONE, SIG_BREAK, SIG_CONTINUE, SIG_RETURN, SIG_THROW
} Signal;

typedef struct {
    VCGEnv *globals;
    int     call_depth;
    int     error_line;
    char    error_msg[512];
    Signal  signal;
    VCGVal  signal_val;
    int     html_mode;    /* collecting HTML output */
    FILE   *html_out;
} Interpreter;

void   interp_init(Interpreter *I);
VCGVal interp_run(Interpreter *I, Node *program, FILE *html_out);
void   interp_free(Interpreter *I);

/* ================================================================
   CODE GENERATOR  →  HTML + JS
   ================================================================ */
typedef struct {
    FILE  *out;
    int    indent;
    int    tmp;
    char   title[256];
    char   theme[64];
} CodeGen;

void codegen_init(CodeGen *g, FILE *out, const char *title, const char *theme);
void codegen_emit(CodeGen *g, Node *program);

/* ================================================================
   STDLIB
   ================================================================ */
void stdlib_register(VCGEnv *env);

/* ================================================================
   ERROR REPORTING
   ================================================================ */
void vcg_error(const char *file, int line, const char *fmt, ...);
void vcg_warn (const char *file, int line, const char *fmt, ...);

#endif /* VCG_H */

/* value.c internal helpers used by interpreter */
void    struct_set(VCGStruct *s, const char *key, VCGVal val);
VCGVal *struct_get(VCGStruct *s, const char *key);
void    arr_push(VCGArray *a, VCGVal item);
