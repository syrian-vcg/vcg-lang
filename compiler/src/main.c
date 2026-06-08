#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/vcg.h"

/* ================================================================
   vcgc  —  Syrian VCG Language Compiler  v1.0
   Release Date: 2026-06-06
   New in v3.0: $set, $get, public, w, x, c (channels), pipe, watch
   Usage:
     vcgc file.vcg              → file.html  (compile to HTML/JS)
     vcgc -r file.vcg           → run in interpreter
     vcgc -o out.html file.vcg  → custom output path
     vcgc -t dark file.vcg      → theme: dark|light|ocean|desert
     vcgc --tokens file.vcg     → dump token stream
     vcgc --ast file.vcg        → dump AST
     vcgc --version             → print version
   ================================================================ */

static char *read_file(const char *path){
    FILE *f=fopen(path,"rb");
    if(!f){ fprintf(stderr,"[vcgc] Cannot open: %s\n",path); exit(1); }
    fseek(f,0,SEEK_END); long sz=ftell(f); rewind(f);
    char *buf=malloc(sz+1);
    if(!buf){ fclose(f); fprintf(stderr,"[vcgc] Out of memory\n"); exit(1); }
    fread(buf,1,sz,f); buf[sz]='\0'; fclose(f); return buf;
}

static void extract_title(const char *path, char *out, int maxlen){
    const char *base=strrchr(path,'/');
    base=base?base+1:path;
    strncpy(out,base,maxlen-1); out[maxlen-1]='\0';
    char *dot=strrchr(out,'.'); if(dot)*dot='\0';
}

static void make_out_path(const char *in, char *out, int maxlen){
    strncpy(out,in,maxlen-6); out[maxlen-6]='\0';
    char *dot=strrchr(out,'.');
    if(dot) strcpy(dot,".html");
    else    strcat(out,".html");
}

static void dump_tokens(const char *src, const char *file){
    Lexer l; lex_init(&l,src,file);
    Token t;
    do {
        t=lex_next(&l);
        printf("%s:%d:%d  %-12d  '%s'\n", file, t.line, t.col, t.type, t.sval);
    } while(t.type!=TK_EOF);
}

static void dump_ast(Node *n, int depth){
    if(!n)return;
    for(int i=0;i<depth;i++) printf("  ");
    printf("[%d]", n->type);
    if(n->name) printf(" name=%s", n->name);
    if(n->str)  printf(" str='%s'", n->str);
    if(n->op[0]) printf(" op=%s", n->op);
    if(n->type==ND_INT||n->type==ND_FLOAT) printf(" num=%g", n->num);
    printf("\n");
    dump_ast(n->left,  depth+1);
    dump_ast(n->right, depth+1);
    dump_ast(n->cond,  depth+1);
    dump_ast(n->body,  depth+1);
    dump_ast(n->alt,   depth+1);
    for(int i=0;i<n->nchildren;i++) dump_ast(n->children[i],depth+1);
}

static void print_banner(void){
    printf("\n"
    "  ╔══════════════════════════════════════════════╗\n"
    "  ║  Syrian Private Programming VCG  v%-10s║\n"
    "  ║  Released: 2026-06-06                        ║\n"
    "  ║  Compiler  (vcgc)                            ║\n"
    "  ╚══════════════════════════════════════════════╝\n\n",
    VCG_VERSION_STR);
}

int main(int argc, char *argv[]){
    if(argc<2){ print_banner();
        printf("  Usage: vcgc [options] <file.vcg>\n\n"
               "  Options:\n"
               "    -r            Run in interpreter mode\n"
               "    -o <out.html> Specify output file\n"
               "    -t <theme>    Output theme: dark|light|ocean|desert\n"
               "    --tokens      Dump token stream\n"
               "    --ast         Dump AST\n"
               "    --version     Print version info\n\n");
        return 1;
    }

    if(strcmp(argv[1],"--version")==0){
        printf("vcgc %s\n", VCG_VERSION_STR); return 0;
    }

    /* parse flags */
    int mode_run=0, mode_tokens=0, mode_ast=0;
    const char *in_path=NULL, *out_path_arg=NULL, *theme="dark";

    for(int i=1;i<argc;i++){
        if(strcmp(argv[i],"-r")==0)         { mode_run=1; }
        else if(strcmp(argv[i],"--tokens")==0) { mode_tokens=1; }
        else if(strcmp(argv[i],"--ast")==0)    { mode_ast=1; }
        else if(strcmp(argv[i],"-o")==0&&i+1<argc) { out_path_arg=argv[++i]; }
        else if(strcmp(argv[i],"-t")==0&&i+1<argc) { theme=argv[++i]; }
        else if(argv[i][0]!='-') { in_path=argv[i]; }
    }

    if(!in_path){ fprintf(stderr,"[vcgc] No input file\n"); return 1; }

    char *src=read_file(in_path);

    /* ── token dump mode ── */
    if(mode_tokens){ dump_tokens(src,in_path); free(src); return 0; }

    /* ── parse ── */
    Parser p; parse_init(&p,src,in_path);
    Node *ast=parse_program(&p);
    free(src);

    /* ── AST dump mode ── */
    if(mode_ast){ dump_ast(ast,0); nd_free(ast); return 0; }

    /* ── interpreter mode ── */
    if(mode_run){
        Interpreter I; interp_init(&I);
        interp_run(&I,ast,NULL);
        if(I.signal==SIG_THROW)
            fprintf(stderr,"[VCG Error] line %d: %s\n", I.error_line, I.error_msg);
        interp_free(&I); nd_free(ast); return 0;
    }

    /* ── compile to HTML ── */
    char out_path[1024];
    if(out_path_arg) strncpy(out_path,out_path_arg,1023);
    else make_out_path(in_path,out_path,1024);

    FILE *out=fopen(out_path,"w");
    if(!out){ fprintf(stderr,"[vcgc] Cannot write: %s\n",out_path); nd_free(ast); return 1; }

    char title[256]; extract_title(in_path,title,256);

    CodeGen g; codegen_init(&g,out,title,theme);
    codegen_emit(&g,ast);
    fclose(out); nd_free(ast);

    printf("✓ %s  →  %s\n", in_path, out_path);
    return 0;
}
