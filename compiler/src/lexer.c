#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Lexer  —  full tokeniser  (v1.0, 2026-06-06)
   ================================================================ */

static const struct { const char *word; TokenType tok; } KEYWORDS[] = {
    {"let",      TK_LET},   {"const",    TK_CONST},  {"func",     TK_FUNC},
    {"return",   TK_RETURN},{"if",       TK_IF},      {"else",     TK_ELSE},
    {"while",    TK_WHILE}, {"for",      TK_FOR},     {"in",       TK_IN},
    {"repeat",   TK_REPEAT},{"break",    TK_BREAK},   {"continue", TK_CONTINUE},
    {"show",     TK_SHOW},  {"input",    TK_INPUT},   {"and",      TK_AND},
    {"or",       TK_OR},    {"not",      TK_NOT},     {"html",     TK_HTML},
    {"true",     TK_TRUE},  {"false",    TK_FALSE},   {"nil",      TK_NIL},
    {"import",   TK_IMPORT},{"as",       TK_AS},      {"struct",   TK_STRUCT},
    {"new",      TK_NEW},   {"self",     TK_SELF},    {"null",     TK_NULL},
    {"typeof",   TK_TYPEOF},{"sizeof",   TK_SIZEOF},  {"assert",   TK_ASSERT},
    {"try",      TK_TRY},   {"catch",    TK_CATCH},   {"throw",    TK_THROW},
    {"match",    TK_MATCH}, {"when",     TK_WHEN},
    {"public",   TK_PUBLIC},{"w",         TK_W},
    {"x",         TK_X},   {"c",         TK_C},
    /* v1.0 UI/Media keywords */
    {"youtube",   TK_YOUTUBE},  {"facebook",  TK_FACEBOOK},
    {"instagram", TK_INSTAGRAM},{"xsocial",   TK_XSOCIAL},
    {"url",       TK_URL},      {"btn",       TK_BTN},
    {"key",       TK_KEY},      {"video",     TK_VIDEO},
    {"img",       TK_IMG},      {"h",         TK_H},
    {"l",         TK_L},
    {NULL, TK_EOF}
};

void lex_init(Lexer *l, const char *src, const char *file) {
    l->src  = src;
    l->pos  = 0;
    l->line = 1;
    l->col  = 1;
    l->file = file ? file : "<stdin>";
}

static char lc(Lexer *l)       { return l->src[l->pos]; }
static char la(Lexer *l, int n){ return l->src[l->pos + n]; }
static char adv(Lexer *l) {
    char c = l->src[l->pos];
    if(c){ l->pos++; if(c=='\n'){l->line++;l->col=1;}else l->col++; }
    return c;
}
static void skip_ws(Lexer *l){
    while(lc(l)==' '||lc(l)=='\t'||lc(l)=='\r') adv(l);
}

static Token mktok(Lexer *l, TokenType t, const char *v, double n){
    Token tk;
    tk.type = t;
    tk.nval = n;
    tk.line = l->line;
    tk.col  = l->col;
    tk.file = l->file;
    if(v) strncpy(tk.sval, v, VCG_MAX_STRING-1);
    else  tk.sval[0]='\0';
    return tk;
}

static Token lex_string(Lexer *l, char delim) {
    adv(l); /* opening quote */
    char buf[VCG_MAX_STRING]; int i=0;
    while(lc(l) && lc(l)!=delim) {
        if(lc(l)=='\\') {
            adv(l);
            switch(adv(l)){
                case 'n':  buf[i++]='\n'; break;
                case 't':  buf[i++]='\t'; break;
                case 'r':  buf[i++]='\r'; break;
                case '"':  buf[i++]='"';  break;
                case '\'': buf[i++]='\''; break;
                case '\\': buf[i++]='\\'; break;
                case '0':  buf[i++]='\0'; break;
                default:   buf[i++]='?';  break;
            }
        } else buf[i++]=adv(l);
        if(i>=VCG_MAX_STRING-2) break;
    }
    buf[i]='\0';
    if(lc(l)==delim) adv(l);
    return mktok(l, TK_STRING, buf, 0);
}

static Token lex_number(Lexer *l) {
    char buf[128]; int i=0; int is_float=0;
    if(lc(l)=='0' && (la(l,1)=='x'||la(l,1)=='X')) {
        buf[i++]=adv(l); buf[i++]=adv(l);
        while(isxdigit(lc(l))) buf[i++]=adv(l);
    } else {
        while(isdigit(lc(l))) buf[i++]=adv(l);
        if(lc(l)=='.' && la(l,1)!='.'){ is_float=1; buf[i++]=adv(l); while(isdigit(lc(l))) buf[i++]=adv(l); }
        if(lc(l)=='e'||lc(l)=='E'){ is_float=1; buf[i++]=adv(l); if(lc(l)=='+'||lc(l)=='-') buf[i++]=adv(l); while(isdigit(lc(l))) buf[i++]=adv(l); }
    }
    buf[i]='\0';
    double n = strtod(buf, NULL);
    return mktok(l, is_float ? TK_FLOAT : TK_INT, buf, n);
}

static Token lex_ident(Lexer *l) {
    char buf[VCG_MAX_IDENT]; int i=0;
    while(isalnum(lc(l))||lc(l)=='_') buf[i++]=adv(l);
    buf[i]='\0';
    TokenType t = TK_IDENT;
    for(int k=0; KEYWORDS[k].word; k++)
        if(strcmp(buf, KEYWORDS[k].word)==0){ t=KEYWORDS[k].tok; break; }
    return mktok(l, t, buf, 0);
}

Token lex_next(Lexer *l) {
    restart:
    skip_ws(l);
    int ln = l->line;

    char c = lc(l);

    if(c=='\0') return mktok(l, TK_EOF, "EOF", 0);

    /* Newline */
    if(c=='\n'){ adv(l); Token t=mktok(l,TK_NEWLINE,"\\n",0); t.line=ln; return t; }

    /* Comments */
    if(c=='#'){ while(lc(l)&&lc(l)!='\n') adv(l); goto restart; }
    if(c=='/'&&la(l,1)=='/'){ while(lc(l)&&lc(l)!='\n') adv(l); goto restart; }
    if(c=='/'&&la(l,1)=='*'){
        adv(l);adv(l);
        while(lc(l)&&!(lc(l)=='*'&&la(l,1)=='/')) adv(l);
        if(lc(l)) { adv(l); adv(l); }
        goto restart;
    }

    /* String */
    if(c=='"'||c=='\'') return lex_string(l,c);

    /* Multi-line string (backtick) */
    if(c=='`'){
        adv(l);
        char buf[VCG_MAX_STRING]; int i=0;
        while(lc(l)&&lc(l)!='`') buf[i++]=adv(l);
        buf[i]='\0'; if(lc(l)) adv(l);
        Token t = mktok(l,TK_STRING,buf,0); t.line=ln; return t;
    }

    /* $ sigil: $set $get $c $w $x */
    if(c=='$'){
        adv(l);
        char buf2[64]; int bi=0;
        while(isalpha(lc(l))||lc(l)=='_') buf2[bi++]=adv(l);
        buf2[bi]='\0';
        if(strcmp(buf2,"set")==0) return mktok(l,TK_SET,"$set",0);
        if(strcmp(buf2,"get")==0) return mktok(l,TK_GET,"$get",0);
        if(strcmp(buf2,"c")==0)   return mktok(l,TK_C,"$c",0);
        if(strcmp(buf2,"w")==0)   return mktok(l,TK_W,"$w",0);
        if(strcmp(buf2,"x")==0)   return mktok(l,TK_X,"$x",0);
        /* generic $ident → TK_DOLLAR + ident */
        Token dt=mktok(l,TK_DOLLAR,buf2,0);
        snprintf(dt.sval,VCG_MAX_STRING,"$%s",buf2);
        return dt;
    }

    /* Number */
    if(isdigit(c)||(c=='.'&&isdigit(la(l,1)))) return lex_number(l);

    /* Identifier/keyword */
    if(isalpha(c)||c=='_') return lex_ident(l);

    /* Multi-char operators */
    adv(l);
    char n = lc(l);

#define OP2(a,b,T) if(c==(a)&&n==(b)){adv(l);Token t=mktok(l,T,"",0);t.line=ln;return t;}
    OP2('=','=',TK_EQ)   OP2('!','=',TK_NEQ)
    OP2('<','=',TK_LTE)  OP2('>','=',TK_GTE)
    OP2('+','=',TK_PLUS_ASSIGN)  OP2('-','=',TK_MINUS_ASSIGN)
    OP2('*','=',TK_STAR_ASSIGN)  OP2('/','=',TK_SLASH_ASSIGN)
    OP2('+','+',TK_INC)  OP2('-','-',TK_DEC)
    OP2('*','*',TK_POWER)
    OP2('-','>',TK_ARROW)
    OP2('<','<',TK_LSHIFT) OP2('>','>',TK_RSHIFT)
    OP2('<','-',TK_C)
    OP2('.','.',TK_DOTDOT)
#undef OP2

    /* Single-char */
    char s[2]={c,'\0'};
    switch(c){
        case '+': return mktok(l,TK_PLUS,s,0);
        case '-': return mktok(l,TK_MINUS,s,0);
        case '*': return mktok(l,TK_STAR,s,0);
        case '/': return mktok(l,TK_SLASH,s,0);
        case '%': return mktok(l,TK_PERCENT,s,0);
        case '<': return mktok(l,TK_LT,s,0);
        case '>': return mktok(l,TK_GT,s,0);
        case '=': return mktok(l,TK_ASSIGN,s,0);
        case '!': return mktok(l,TK_NOT,s,0);
        case '(': return mktok(l,TK_LPAREN,s,0);
        case ')': return mktok(l,TK_RPAREN,s,0);
        case '{': return mktok(l,TK_LBRACE,s,0);
        case '}': return mktok(l,TK_RBRACE,s,0);
        case '[': return mktok(l,TK_LBRACKET,s,0);
        case ']': return mktok(l,TK_RBRACKET,s,0);
        case ',': return mktok(l,TK_COMMA,s,0);
        case ';': return mktok(l,TK_SEMICOLON,s,0);
        case ':': return mktok(l,TK_COLON,s,0);
        case '.': return mktok(l,TK_DOT,s,0);
        case '|': return mktok(l,TK_PIPE,s,0);
        case '&': return mktok(l,TK_AMP,s,0);
        case '~': return mktok(l,TK_TILDE,s,0);
        case '?': return mktok(l,TK_QUESTION,s,0);
        case '@': return mktok(l,TK_AT,s,0);
        default:  return mktok(l,TK_ERROR,s,0);
    }
}
