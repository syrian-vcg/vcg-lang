#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Parser  —  full recursive-descent  (v3.0, 2026-06-06)
   ================================================================ */

static void advance(Parser *p);
static Node *parse_stmt(Parser *p);
static Node *parse_expr(Parser *p);
static Node *parse_block(Parser *p);
static Node *parse_expr_prec(Parser *p, int minp);

/* ── Helpers ── */
static Token cur(Parser *p)        { return p->cur; }
static Token peek(Parser *p)       { return p->ahead; }

static void advance(Parser *p) {
    p->cur = p->ahead;
    do { p->ahead = lex_next(&p->lex); }
    while (p->ahead.type == TK_NEWLINE || p->ahead.type == TK_SEMICOLON);
}

static void skip_nl(Parser *p) {
    while (p->cur.type == TK_NEWLINE || p->cur.type == TK_SEMICOLON) advance(p);
}

static Token expect(Parser *p, TokenType t, const char *what) {
    if (p->cur.type != t) {
        fprintf(stderr, "[VCG] %s:%d: expected %s, got '%s'\n",
                p->cur.file, p->cur.line, what, p->cur.sval[0]?p->cur.sval:"?");
        exit(1);
    }
    Token tk = p->cur; advance(p); return tk;
}

static int check(Parser *p, TokenType t) { return p->cur.type == t; }
static int match(Parser *p, TokenType t) {
    if (p->cur.type == t) { advance(p); return 1; }
    return 0;
}

void parse_init(Parser *p, const char *src, const char *file) {
    memset(p, 0, sizeof(*p));
    lex_init(&p->lex, src, file);
    /* prime the pump – skip initial newlines */
    p->cur = lex_next(&p->lex);
    while(p->cur.type==TK_NEWLINE) p->cur=lex_next(&p->lex);
    p->ahead = lex_next(&p->lex);
    while(p->ahead.type==TK_NEWLINE) p->ahead=lex_next(&p->lex);
}

/* ── Primary ── */
static Node *parse_primary(Parser *p) {
    Token t = cur(p);
    Node *n;

    switch(t.type) {
    case TK_INT:
        advance(p);
        n = nd_new(ND_INT, t.line); n->num = t.nval; return n;
    case TK_FLOAT:
        advance(p);
        n = nd_new(ND_FLOAT, t.line); n->num = t.nval; return n;
    case TK_STRING:
        advance(p);
        n = nd_new(ND_STRING, t.line); n->str = strdup(t.sval); return n;
    case TK_TRUE:
        advance(p); n=nd_new(ND_BOOL,t.line); n->bval=1; return n;
    case TK_FALSE:
        advance(p); n=nd_new(ND_BOOL,t.line); n->bval=0; return n;
    case TK_NIL: case TK_NULL:
        advance(p); return nd_new(ND_NIL, t.line);
    case TK_NOT: case TK_TILDE:
        advance(p);
        n = nd_new(ND_UNOP, t.line);
        strcpy(n->op, t.type==TK_NOT ? "not" : "~");
        n->right = parse_primary(p); return n;
    case TK_MINUS:
        advance(p);
        n = nd_new(ND_UNOP, t.line); strcpy(n->op,"-");
        n->right = parse_primary(p); return n;
    case TK_INC: case TK_DEC: {
        int is_inc = t.type==TK_INC;
        advance(p);
        n = nd_new(ND_UNOP, t.line); strcpy(n->op, is_inc?"++pre":"--pre");
        n->right = parse_primary(p); return n;
    }
    case TK_LPAREN:
        advance(p);
        /* Check for lambda: (params) -> expr */
        n = parse_expr(p);
        expect(p, TK_RPAREN, ")");
        return n;
    case TK_LBRACKET: {
        /* Array literal */
        advance(p);
        n = nd_new(ND_ARRAY, t.line);
        skip_nl(p);
        while(!check(p,TK_RBRACKET) && !check(p,TK_EOF)) {
            nd_add_child(n, parse_expr(p));
            skip_nl(p);
            if(check(p,TK_COMMA)) { advance(p); skip_nl(p); }
        }
        expect(p, TK_RBRACKET, "]"); return n;
    }
    case TK_LBRACE: {
        /* Struct literal  { key: val, ... } */
        advance(p);
        n = nd_new(ND_STRUCT_LIT, t.line);
        n->finits = NULL; n->nfinits = 0;
        skip_nl(p);
        while(!check(p,TK_RBRACE)&&!check(p,TK_EOF)) {
            /* Allow any keyword as struct key: {x:1, y:2, func:3, ...} */
            Token key=cur(p);
            if(key.type!=TK_RBRACE&&key.type!=TK_EOF&&key.type!=TK_COLON)
                advance(p);
            else key=expect(p,TK_IDENT,"field name");
            expect(p, TK_COLON, ":");
            Node *val = parse_expr(p);
            n->finits = realloc(n->finits,(n->nfinits+1)*sizeof(FieldInit));
            n->finits[n->nfinits].key = strdup(key.sval);
            n->finits[n->nfinits].val = val;
            n->nfinits++;
            skip_nl(p);
            if(check(p,TK_COMMA)){advance(p);skip_nl(p);}
        }
        expect(p,TK_RBRACE,"}"); return n;
    }
    case TK_FUNC: {
        /* Anonymous function / lambda */
        advance(p);
        n = nd_new(ND_LAMBDA, t.line);
        expect(p,TK_LPAREN,"(");
        n->params=NULL; n->nparams=0;
        while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
            Token pm=cur(p);
            if(pm.type==TK_IDENT||pm.type==TK_X||pm.type==TK_W||pm.type==TK_C){advance(p);}
            else pm=expect(p,TK_IDENT,"param");
            n->params = realloc(n->params,(n->nparams+1)*sizeof(char*));
            n->params[n->nparams++] = strdup(pm.sval);
            if(check(p,TK_COMMA)) advance(p);
        }
        expect(p,TK_RPAREN,")");
        n->body = parse_block(p); return n;
    }
    case TK_INPUT: {
        advance(p);
        n = nd_new(ND_INPUT, t.line);
        if(check(p,TK_LPAREN)){
            advance(p);
            if(!check(p,TK_RPAREN)) n->left = parse_expr(p);
            expect(p,TK_RPAREN,")");
        }
        return n;
    }
    case TK_TYPEOF: {
        advance(p); n=nd_new(ND_TYPEOF,t.line);
        expect(p,TK_LPAREN,"("); n->right=parse_expr(p); expect(p,TK_RPAREN,")");
        return n;
    }
    case TK_SIZEOF: {
        advance(p); n=nd_new(ND_SIZEOF,t.line);
        expect(p,TK_LPAREN,"("); n->right=parse_expr(p); expect(p,TK_RPAREN,")");
        return n;
    }
    case TK_NEW: {
        advance(p);
        n = nd_new(ND_NEW, t.line);
        Token nm = expect(p,TK_IDENT,"struct name");
        n->name = strdup(nm.sval);
        expect(p,TK_LPAREN,"(");
        while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
            nd_add_child(n, parse_expr(p));
            if(check(p,TK_COMMA)) advance(p);
        }
        expect(p,TK_RPAREN,")"); return n;
    }
    case TK_SELF: {
        advance(p); n=nd_new(ND_IDENT,t.line); n->name=strdup("self"); return n;
    }
    case TK_GET: {
        advance(p);
        n = nd_new(ND_GET, t.line);
        expect(p,TK_LPAREN,"(");
        n->left = parse_expr(p);
        expect(p,TK_RPAREN,")");
        return n;
    }
    case TK_SET: {
        advance(p);
        n = nd_new(ND_SET, t.line);
        expect(p,TK_LPAREN,"(");
        n->left  = parse_expr(p);
        expect(p,TK_COMMA,",");
        n->right = parse_expr(p);
        expect(p,TK_RPAREN,")");
        return n;
    }
    case TK_X: {
        advance(p);
        /* Contextual: if followed by operator/)/]/,/; → it is a variable named "x" */
        {
            TokenType nxt = cur(p).type;
            if(nxt==TK_PLUS||nxt==TK_MINUS||nxt==TK_STAR||nxt==TK_SLASH||
               nxt==TK_PERCENT||nxt==TK_POWER||nxt==TK_EQ||nxt==TK_NEQ||
               nxt==TK_LT||nxt==TK_GT||nxt==TK_LTE||nxt==TK_GTE||
               nxt==TK_RPAREN||nxt==TK_RBRACKET||nxt==TK_RBRACE||
               nxt==TK_COMMA||nxt==TK_SEMICOLON||nxt==TK_NEWLINE||
               nxt==TK_EOF||nxt==TK_AND||nxt==TK_OR||nxt==TK_DOTDOT||
               nxt==TK_ASSIGN||nxt==TK_PLUS_ASSIGN||nxt==TK_MINUS_ASSIGN||
               nxt==TK_COLON||nxt==TK_LBRACKET){
                /* Treat as variable "x" */
                n = nd_new(ND_IDENT, t.line);
                n->name = strdup("x");
                return n;
            }
        }
        n = nd_new(ND_X, t.line);
        if(check(p,TK_FUNC)){
            advance(p);
            Node *lam=nd_new(ND_LAMBDA,t.line);
            lam->params=NULL; lam->nparams=0;
            expect(p,TK_LPAREN,"(");
            while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
                Token pm=cur(p);
                if(pm.type==TK_IDENT||pm.type==TK_X||pm.type==TK_W||pm.type==TK_C){advance(p);}
                else pm=expect(p,TK_IDENT,"param");
                lam->params=realloc(lam->params,(lam->nparams+1)*sizeof(char*));
                lam->params[lam->nparams++]=strdup(pm.sval);
                if(check(p,TK_COMMA)) advance(p);
            }
            expect(p,TK_RPAREN,")");
            lam->body=parse_block(p);
            n->right=lam;
        } else {
            n->right = parse_expr(p);
        }
        return n;
    }
    case TK_C: {
        /* Contextual: c as variable name when followed by operator */
        advance(p);
        {
            TokenType nxt = cur(p).type;
            if(nxt==TK_PLUS||nxt==TK_MINUS||nxt==TK_STAR||nxt==TK_SLASH||
               nxt==TK_PERCENT||nxt==TK_EQ||nxt==TK_NEQ||nxt==TK_LT||nxt==TK_GT||
               nxt==TK_LTE||nxt==TK_GTE||nxt==TK_RPAREN||nxt==TK_RBRACKET||
               nxt==TK_RBRACE||nxt==TK_COMMA||nxt==TK_SEMICOLON||nxt==TK_NEWLINE||
               nxt==TK_EOF||nxt==TK_AND||nxt==TK_OR||nxt==TK_LBRACKET||nxt==TK_COLON){
                n = nd_new(ND_IDENT, t.line);
                n->name = strdup("c");
                return n;
            }
        }
        /* Otherwise: channel declaration */
        n = nd_new(ND_C, t.line);
        if(check(p,TK_IDENT)){
            Token nm=cur(p); advance(p);
            n->name=strdup(nm.sval);
            if(check(p,TK_ASSIGN)){ advance(p); n->right=parse_expr(p); }
        }
        return n;
    }
    case TK_W: {
        /* w as variable reference when used in expression context */
        advance(p);
        n = nd_new(ND_IDENT, t.line);
        n->name = strdup("w");
        return n;
    }
    case TK_IDENT: {
        advance(p);
        n = nd_new(ND_IDENT, t.line); n->name = strdup(t.sval);
        return n;
    }
    default:
        fprintf(stderr,"[VCG] %s:%d: unexpected token '%s'\n",
                t.file, t.line, t.sval[0]?t.sval:"?");
        advance(p);
        return nd_new(ND_NIL, t.line);
    }
}

/* ── Postfix: calls, indexing, field access ── */
static Node *parse_postfix(Parser *p) {
    Node *n = parse_primary(p);
    while(1) {
        if(check(p,TK_LPAREN)) {
            /* function call */
            Token t = cur(p); advance(p);
            Node *call = nd_new(ND_CALL, t.line);
            call->left = n;
            skip_nl(p);
            while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
                nd_add_child(call, parse_expr(p));
                skip_nl(p);
                if(check(p,TK_COMMA)){advance(p);skip_nl(p);}
            }
            expect(p,TK_RPAREN,")");
            n = call;
        } else if(check(p,TK_LBRACKET)) {
            Token t=cur(p); advance(p);
            Node *idx=nd_new(ND_INDEX,t.line);
            idx->left=n; idx->right=parse_expr(p);
            expect(p,TK_RBRACKET,"]");
            n=idx;
        } else if(check(p,TK_DOT)) {
            Token t=cur(p); advance(p);
            /* Field name: allow keywords as field names contextually */
            Token field=cur(p);
            if(field.type==TK_IDENT||field.type==TK_X||field.type==TK_W||
               field.type==TK_C||field.type==TK_PUBLIC||field.type==TK_IN||
               field.type==TK_LET||field.type==TK_FUNC||field.type==TK_TRUE||
               field.type==TK_FALSE||field.type==TK_NIL||
               field.type==TK_H||field.type==TK_L||field.type==TK_BTN||
               field.type==TK_KEY||field.type==TK_IMG||field.type==TK_URL||
               field.type==TK_VIDEO){
                advance(p);
            } else {
                field=expect(p,TK_IDENT,"field");
            }
            if(check(p,TK_LPAREN)){
                advance(p);
                Node *mc=nd_new(ND_METHOD_CALL,t.line);
                mc->left=n; mc->name=strdup(field.sval);
                skip_nl(p);
                while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
                    nd_add_child(mc,parse_expr(p)); skip_nl(p);
                    if(check(p,TK_COMMA)){advance(p);skip_nl(p);}
                }
                expect(p,TK_RPAREN,")"); n=mc;
            } else {
                Node *fa=nd_new(ND_FIELD,t.line);
                fa->left=n; fa->name=strdup(field.sval); n=fa;
            }
        } else if(check(p,TK_INC)||check(p,TK_DEC)){
            Token t=cur(p); advance(p);
            Node *u=nd_new(ND_UNOP,t.line);
            strcpy(u->op, t.type==TK_INC?"++post":"--post");
            u->left=n; n=u;
        } else break;
    }
    return n;
}

/* ── Binary operator precedence ── */
typedef struct { TokenType tok; int prec; const char *op; int right_assoc; } OpInfo;
static const OpInfo OPS[] = {
    {TK_OR,     1, "or",  0}, {TK_AND,    2, "and", 0},
    {TK_PIPE,   3, "|",   0}, {TK_AMP,    5, "&",   0},
    {TK_EQ,     6, "==",  0}, {TK_NEQ,    6, "!=",  0},
    {TK_LT,     7, "<",   0}, {TK_GT,     7, ">",   0},
    {TK_LTE,    7, "<=",  0}, {TK_GTE,    7, ">=",  0},
    {TK_LSHIFT, 8, "<<",  0}, {TK_RSHIFT, 8, ">>",  0},
    {TK_DOTDOT, 8, "..",  0},
    {TK_PLUS,   9, "+",   0}, {TK_MINUS,  9, "-",   0},
    {TK_STAR,  10, "*",   0}, {TK_SLASH, 10, "/",   0},
    {TK_PERCENT,10,"%",   0}, {TK_POWER, 11, "**",  1},
    {TK_EOF,    0, NULL,  0}
};

static const OpInfo *find_op(TokenType t) {
    for(int i=0; OPS[i].op; i++) if(OPS[i].tok==t) return &OPS[i];
    return NULL;
}

static Node *parse_expr_prec(Parser *p, int minp) {
    Node *left = parse_postfix(p);
    while(1) {
        const OpInfo *op = find_op(cur(p).type);
        if(!op || op->prec <= minp) break;
        int line = cur(p).line;
        advance(p);
        int next_prec = op->right_assoc ? op->prec - 1 : op->prec;
        Node *right = parse_expr_prec(p, next_prec);
        Node *bin = nd_new(ND_BINOP, line);
        strncpy(bin->op, op->op, 3);
        bin->left = left; bin->right = right;
        left = bin;
    }
    /* Ternary  expr ? then : else */
    if(check(p,TK_QUESTION)){
        advance(p);
        Node *tn=nd_new(ND_TERNARY, left->line);
        tn->cond=left;
        tn->body=parse_expr(p);
        expect(p,TK_COLON,":");
        tn->alt=parse_expr(p);
        return tn;
    }
    return left;
}

static Node *parse_expr(Parser *p) { return parse_expr_prec(p, 0); }

/* ── Block ── */
static Node *parse_block(Parser *p) {
    Node *b = nd_new(ND_BLOCK, cur(p).line);
    skip_nl(p);
    expect(p,TK_LBRACE,"{");
    skip_nl(p);
    while(!check(p,TK_RBRACE)&&!check(p,TK_EOF)){
        Node *s=parse_stmt(p);
        if(s) nd_add_child(b, s);
        skip_nl(p);
    }
    expect(p,TK_RBRACE,"}");
    return b;
}

/* ── Statements ── */
static Node *parse_stmt(Parser *p) {
    skip_nl(p);
    Token t = cur(p);
    Node *n;

    switch(t.type) {

    /* let x = expr  /  let x: type = expr */
    case TK_LET: case TK_CONST: {
        int is_const = (t.type==TK_CONST);
        advance(p);
        n = nd_new(is_const?ND_CONST:ND_LET, t.line);
        Token nm=cur(p);
        if(nm.type==TK_IDENT||nm.type==TK_X||nm.type==TK_W||nm.type==TK_C)
            advance(p);
        else nm=expect(p,TK_IDENT,"variable name");
        n->name = strdup(nm.sval);
        /* optional type annotation */
        if(check(p,TK_COLON)){ advance(p); advance(p); /* skip type for now */ }
        if(check(p,TK_ASSIGN)){
            advance(p); n->right = parse_expr(p);
        }
        return n;
    }

    /* func name(params) { } */
    case TK_FUNC: {
        advance(p);
        n = nd_new(ND_FUNC, t.line);
        /* func name: allow contextual keyword names like 'x', 'w', 'c' */
        Token nm = cur(p);
        if(nm.type==TK_IDENT||nm.type==TK_X||nm.type==TK_W||nm.type==TK_C) advance(p);
        else nm = expect(p,TK_IDENT,"function name");
        n->name = strdup(nm.sval);
        expect(p,TK_LPAREN,"(");
        n->params=NULL; n->nparams=0;
        n->param_defaults=NULL; n->param_defaults_mask=NULL;
        while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
            if(check(p,TK_DOTDOT)){advance(p);n->variadic=1;}
            /* allow contextual keywords as param names */
            Token pm=cur(p);
            if(pm.type==TK_IDENT||pm.type==TK_X||pm.type==TK_W||pm.type==TK_C){advance(p);}
            else pm=expect(p,TK_IDENT,"param");
            n->params=realloc(n->params,(n->nparams+1)*sizeof(char*));
            n->params[n->nparams++]=strdup(pm.sval);
            if(check(p,TK_ASSIGN)){
                advance(p); parse_expr(p); /* default value – skip for now */
            }
            if(check(p,TK_COMMA)) advance(p);
        }
        expect(p,TK_RPAREN,")");
        n->body = parse_block(p);
        return n;
    }

    /* struct Name { field1, field2, ... } */
    case TK_STRUCT: {
        advance(p);
        n = nd_new(ND_STRUCT, t.line);
        Token nm=expect(p,TK_IDENT,"struct name");
        n->name=strdup(nm.sval);
        expect(p,TK_LBRACE,"{");
        skip_nl(p);
        n->fields=NULL; n->nfields=0;
        while(!check(p,TK_RBRACE)&&!check(p,TK_EOF)){
            Token f=cur(p);
            if(f.type==TK_IDENT||f.type==TK_X||f.type==TK_W||f.type==TK_C||
               f.type==TK_H||f.type==TK_L||f.type==TK_KEY||f.type==TK_BTN||
               f.type==TK_IMG||f.type==TK_URL||f.type==TK_VIDEO||
               f.type==TK_PUBLIC||f.type==TK_IN||f.type==TK_LET||
               f.type==TK_YOUTUBE||f.type==TK_FACEBOOK||f.type==TK_INSTAGRAM||
               f.type==TK_XSOCIAL)
                advance(p);
            else f=expect(p,TK_IDENT,"field");
            n->fields=realloc(n->fields,(n->nfields+1)*sizeof(char*));
            n->fields[n->nfields++]=strdup(f.sval);
            skip_nl(p);
            if(check(p,TK_COMMA)){advance(p);skip_nl(p);}
        }
        expect(p,TK_RBRACE,"}"); return n;
    }

    /* if cond { } else if cond { } else { } */
    case TK_IF: {
        advance(p);
        n = nd_new(ND_IF, t.line);
        n->cond = parse_expr(p);
        n->body = parse_block(p);
        skip_nl(p);
        if(check(p,TK_ELSE)){
            advance(p); skip_nl(p);
            if(check(p,TK_IF)) n->alt=parse_stmt(p);
            else n->alt=parse_block(p);
        }
        return n;
    }

    /* while cond { } */
    case TK_WHILE: {
        advance(p);
        n = nd_new(ND_WHILE, t.line);
        n->cond = parse_expr(p);
        int ol=p->in_loop; p->in_loop=1;
        n->body = parse_block(p);
        p->in_loop=ol; return n;
    }

    /* for x in expr { } */
    case TK_FOR: {
        advance(p);
        n = nd_new(ND_FOR_IN, t.line);
        /* loop var: allow contextual keywords x, c, w, etc. */
        Token var=cur(p);
        if(var.type==TK_IDENT||var.type==TK_X||var.type==TK_W||var.type==TK_C)
            advance(p);
        else var=expect(p,TK_IDENT,"loop var");
        n->name=strdup(var.sval);
        expect(p,TK_IN,"in");
        n->init=parse_expr(p);
        int ol=p->in_loop; p->in_loop=1;
        n->body=parse_block(p);
        p->in_loop=ol; return n;
    }

    /* repeat N { } */
    case TK_REPEAT: {
        advance(p);
        n = nd_new(ND_REPEAT, t.line);
        n->cond = parse_expr(p);
        int ol=p->in_loop; p->in_loop=1;
        n->body = parse_block(p);
        p->in_loop=ol; return n;
    }

    /* return [expr] */
    case TK_RETURN: {
        advance(p);
        n = nd_new(ND_RETURN, t.line);
        if(!check(p,TK_RBRACE)&&!check(p,TK_NEWLINE)&&!check(p,TK_EOF)&&!check(p,TK_SEMICOLON))
            n->right = parse_expr(p);
        return n;
    }

    case TK_BREAK:
        advance(p); return nd_new(ND_BREAK, t.line);
    case TK_CONTINUE:
        advance(p); return nd_new(ND_CONTINUE, t.line);

    /* show(args...) */
    case TK_SHOW: {
        advance(p);
        n = nd_new(ND_SHOW, t.line);
        expect(p,TK_LPAREN,"(");
        skip_nl(p);
        while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
            nd_add_child(n,parse_expr(p));
            skip_nl(p);
            if(check(p,TK_COMMA)){advance(p);skip_nl(p);}
        }
        expect(p,TK_RPAREN,")"); return n;
    }

    /* html "..." */
    case TK_HTML: {
        advance(p);
        n = nd_new(ND_HTML, t.line);
        n->right = parse_expr(p); return n;
    }

    /* assert(expr [, msg]) */
    case TK_ASSERT: {
        advance(p); n=nd_new(ND_ASSERT,t.line);
        expect(p,TK_LPAREN,"(");
        n->cond=parse_expr(p);
        if(check(p,TK_COMMA)){advance(p);n->right=parse_expr(p);}
        expect(p,TK_RPAREN,")"); return n;
    }

    /* throw expr */
    case TK_THROW: {
        advance(p); n=nd_new(ND_THROW,t.line);
        n->right=parse_expr(p); return n;
    }

    /* try { } catch e { } */
    case TK_TRY: {
        advance(p); n=nd_new(ND_TRY_CATCH,t.line);
        n->body=parse_block(p);
        skip_nl(p);
        expect(p,TK_CATCH,"catch");
        if(check(p,TK_IDENT)){Token e=cur(p);advance(p);n->name=strdup(e.sval);}
        n->alt=parse_block(p); return n;
    }

    /* match expr { when val -> stmt } */
    case TK_MATCH: {
        advance(p); n=nd_new(ND_MATCH,t.line);
        n->cond=parse_expr(p);
        expect(p,TK_LBRACE,"{"); skip_nl(p);
        n->arms_cond=NULL; n->arms_body=NULL; n->narms=0;
        while(check(p,TK_WHEN)){
            advance(p);
            n->arms_cond=realloc(n->arms_cond,(n->narms+1)*sizeof(Node*));
            n->arms_body=realloc(n->arms_body,(n->narms+1)*sizeof(Node*));
            n->arms_cond[n->narms]=parse_expr(p);
            if(check(p,TK_ARROW)) advance(p);
            else expect(p,TK_COLON,":");
            n->arms_body[n->narms]=parse_stmt(p);
            n->narms++; skip_nl(p);
        }
        expect(p,TK_RBRACE,"}"); return n;
    }

    /* ═══════════════════════════════════════════════════════
       v1.0  UI / MEDIA / WEB  KEYWORDS
       ═══════════════════════════════════════════════════════ */

    /* youtube(url [, width, height])  — embed YouTube */
    case TK_YOUTUBE: {
        advance(p); n=nd_new(ND_YOUTUBE,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);   /* URL or video ID */
        if(check(p,TK_COMMA)){advance(p);
            n->right=parse_expr(p);  /* optional: width */
            if(check(p,TK_COMMA)){advance(p);
                Node *h3=nd_new(ND_INT,t.line); h3->num=0;
                /* store height as 3rd child */
                nd_add_child(n,h3);
                nd_add_child(n,parse_expr(p));
            }
        }
        expect(p,TK_RPAREN,")"); return n;
    }

    /* facebook(url [, text]) */
    case TK_FACEBOOK: {
        advance(p); n=nd_new(ND_FACEBOOK,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p);}
        expect(p,TK_RPAREN,")"); return n;
    }

    /* instagram(handle [, text]) */
    case TK_INSTAGRAM: {
        advance(p); n=nd_new(ND_INSTAGRAM,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p);}
        expect(p,TK_RPAREN,")"); return n;
    }

    /* xsocial(handle [, text]) — X/Twitter */
    case TK_XSOCIAL: {
        advance(p); n=nd_new(ND_XSOCIAL,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p);}
        expect(p,TK_RPAREN,")"); return n;
    }

    /* url(href, text [, target]) */
    case TK_URL: {
        advance(p); n=nd_new(ND_URL,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);   /* href */
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p); /* text */
            if(check(p,TK_COMMA)){advance(p); nd_add_child(n,parse_expr(p));} /* target */
        }
        expect(p,TK_RPAREN,")"); return n;
    }

    /* btn(text [, onclick_expr]) */
    case TK_BTN: {
        advance(p); n=nd_new(ND_BTN,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);   /* label */
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p);} /* action */
        expect(p,TK_RPAREN,")"); return n;
    }

    /* key(key_combo)  — render keyboard shortcut badge */
    case TK_KEY: {
        advance(p); n=nd_new(ND_KEY,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);
        expect(p,TK_RPAREN,")"); return n;
    }

    /* video(src [, width, height]) — HTML5 video */
    case TK_VIDEO: {
        advance(p); n=nd_new(ND_VIDEO,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);   /* src */
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p); /* width */
            if(check(p,TK_COMMA)){advance(p); nd_add_child(n,parse_expr(p));} /* height */
        }
        expect(p,TK_RPAREN,")"); return n;
    }

    /* img(src [, alt, width]) */
    case TK_IMG: {
        advance(p); n=nd_new(ND_IMG,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);   /* src */
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p); /* alt */
            if(check(p,TK_COMMA)){advance(p); nd_add_child(n,parse_expr(p));} /* width */
        }
        expect(p,TK_RPAREN,")"); return n;
    }

    /* h(level, text)  — heading: h(1, "Title") → <h1>Title</h1> */
    case TK_H: {
        advance(p); n=nd_new(ND_H,t.line);
        expect(p,TK_LPAREN,"(");
        n->left=parse_expr(p);   /* level 1-6 */
        if(check(p,TK_COMMA)){advance(p); n->right=parse_expr(p);} /* text */
        expect(p,TK_RPAREN,")"); return n;
    }

    /* l(text [, text2, ...])  — list item(s) */
    case TK_L: {
        advance(p); n=nd_new(ND_L,t.line);
        expect(p,TK_LPAREN,"(");
        while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
            nd_add_child(n,parse_expr(p));
            if(check(p,TK_COMMA)) advance(p);
        }
        expect(p,TK_RPAREN,")"); return n;
    }

    /* import "module" [as alias] */
    case TK_IMPORT: {
        advance(p); n=nd_new(ND_IMPORT,t.line);
        n->str=strdup(cur(p).sval); advance(p);
        if(check(p,TK_AS)){advance(p);n->name=strdup(cur(p).sval);advance(p);}
        return n;
    }

    /* ── v3.0 NEW CONCEPTS ────────────────────────────────── */

    /* public decl:  public let x = ...  /  public func f() {} */
    case TK_PUBLIC: {
        advance(p);
        n = nd_new(ND_PUBLIC, t.line);
        n->right = parse_stmt(p);   /* wrap whatever follows */
        return n;
    }

    /* $set(key, val)  —  reactive property setter */
    case TK_SET: {
        advance(p);
        n = nd_new(ND_SET, t.line);
        expect(p, TK_LPAREN, "(");
        n->left  = parse_expr(p);   /* key expression */
        expect(p, TK_COMMA, ",");
        n->right = parse_expr(p);   /* value expression */
        expect(p, TK_RPAREN, ")");
        return n;
    }

    /* $get(key)  —  reactive property getter (as statement) */
    case TK_GET: {
        advance(p);
        n = nd_new(ND_GET, t.line);
        expect(p, TK_LPAREN, "(");
        n->left = parse_expr(p);
        expect(p, TK_RPAREN, ")");
        return n;
    }

    /* w name = expr   — write-only binding (cannot be read) */
    case TK_W: {
        advance(p);
        n = nd_new(ND_W, t.line);
        Token nm = expect(p, TK_IDENT, "variable name");
        n->name = strdup(nm.sval);
        if(check(p,TK_ASSIGN)) { advance(p); n->right = parse_expr(p); }
        return n;
    }

    /* x expr   — execute / immediately invoke expression
       Special: x func() {}  treats as IIFE */
    case TK_X: {
        advance(p);
        n = nd_new(ND_X, t.line);
        /* if next is func, parse as anonymous lambda and wrap in call */
        if(check(p,TK_FUNC)){
            advance(p);  /* eat 'func' */
            Node *lam = nd_new(ND_LAMBDA, t.line);
            lam->params=NULL; lam->nparams=0;
            expect(p,TK_LPAREN,"(");
            while(!check(p,TK_RPAREN)&&!check(p,TK_EOF)){
                Token pm=expect(p,TK_IDENT,"param");
                lam->params=realloc(lam->params,(lam->nparams+1)*sizeof(char*));
                lam->params[lam->nparams++]=strdup(pm.sval);
                if(check(p,TK_COMMA)) advance(p);
            }
            expect(p,TK_RPAREN,")");
            lam->body = parse_block(p);
            /* wrap in ND_X so interpreter auto-invokes it */
            n->right = lam;
        } else {
            n->right = parse_expr(p);
        }
        return n;
    }

    /* c name   —  channel variable declaration */
    case TK_C: {
        advance(p);
        /* c name  or  c name <- expr  (send)  or  <- c name (recv in expr) */
        n = nd_new(ND_C, t.line);
        if(check(p,TK_IDENT)){
            Token nm = cur(p); advance(p);
            n->name = strdup(nm.sval);
            if(check(p,TK_ASSIGN)){
                /* c chan = expr — declare & send initial value */
                advance(p);
                n->right = parse_expr(p);
            }
        }
        return n;
    }

    /* expression statement or assignment */
    default: {
        Node *expr = parse_expr(p);
        /* compound assignment */
        TokenType ct = cur(p).type;
        if(ct==TK_ASSIGN||ct==TK_PLUS_ASSIGN||ct==TK_MINUS_ASSIGN||
           ct==TK_STAR_ASSIGN||ct==TK_SLASH_ASSIGN){
            int line=cur(p).line;
            char op[4]="=";
            if(ct==TK_PLUS_ASSIGN)  strcpy(op,"+=");
            else if(ct==TK_MINUS_ASSIGN) strcpy(op,"-=");
            else if(ct==TK_STAR_ASSIGN)  strcpy(op,"*=");
            else if(ct==TK_SLASH_ASSIGN) strcpy(op,"/=");
            advance(p);
            Node *a = (ct==TK_ASSIGN)
                ? nd_new(ND_ASSIGN,line)
                : nd_new(ND_OP_ASSIGN,line);
            strcpy(a->op, op);
            a->left=expr; a->right=parse_expr(p);
            return a;
        }
        return expr;
    }
    }
}

Node *parse_program(Parser *p) {
    Node *prog = nd_new(ND_PROGRAM, 1);
    skip_nl(p);
    while(!check(p,TK_EOF)){
        Node *s = parse_stmt(p);
        if(s) nd_add_child(prog, s);
        skip_nl(p);
    }
    return prog;
}
