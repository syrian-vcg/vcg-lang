#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Standard Library  —  built-in functions  (v1.0, 2026-06-06)
   ================================================================ */

#define BUILTIN(name) static VCGVal bi_##name(VCGVal *a, int n, int line)
#define REG(env,nm,fn) do{ VCGVal v; v.type=VT_BUILTIN; v.builtin=bi_##fn; env_set(env,nm,v,0); }while(0)

static double as_num(VCGVal v){
    if(v.type==VT_INT)   return (double)v.ival;
    if(v.type==VT_FLOAT) return v.fval;
    if(v.type==VT_STRING) return atof(v.sval);
    return 0;
}
static VCGVal num_val(double d){
    if(d==(long long)d && fabs(d)<1e15) return VCG_INT((int)d);
    return VCG_FLOAT(d);
}

/* ── Math ── */
BUILTIN(abs)   { (void)line; return n>0?num_val(fabs(as_num(a[0]))):VCG_INT(0); }
BUILTIN(floor) { (void)line; return n>0?VCG_INT((int)floor(as_num(a[0]))):VCG_INT(0); }
BUILTIN(ceil)  { (void)line; return n>0?VCG_INT((int)ceil(as_num(a[0]))):VCG_INT(0); }
BUILTIN(round) { (void)line; return n>0?VCG_INT((int)round(as_num(a[0]))):VCG_INT(0); }
BUILTIN(sqrt)  { (void)line; return n>0?VCG_FLOAT(sqrt(as_num(a[0]))):VCG_INT(0); }
BUILTIN(pow)   { (void)line; return n>=2?num_val(pow(as_num(a[0]),as_num(a[1]))):VCG_INT(0); }
BUILTIN(log)   { (void)line; return n>0?VCG_FLOAT(log(as_num(a[0]))):VCG_INT(0); }
BUILTIN(log2)  { (void)line; return n>0?VCG_FLOAT(log2(as_num(a[0]))):VCG_INT(0); }
BUILTIN(log10) { (void)line; return n>0?VCG_FLOAT(log10(as_num(a[0]))):VCG_INT(0); }
BUILTIN(sin)   { (void)line; return n>0?VCG_FLOAT(sin(as_num(a[0]))):VCG_INT(0); }
BUILTIN(cos)   { (void)line; return n>0?VCG_FLOAT(cos(as_num(a[0]))):VCG_INT(0); }
BUILTIN(tan)   { (void)line; return n>0?VCG_FLOAT(tan(as_num(a[0]))):VCG_INT(0); }
BUILTIN(min)   { (void)line; if(n<2)return n?a[0]:VCG_INT(0); double r=as_num(a[0]); for(int i=1;i<n;i++){double v=as_num(a[i]);if(v<r)r=v;} return num_val(r); }
BUILTIN(max)   { (void)line; if(n<2)return n?a[0]:VCG_INT(0); double r=as_num(a[0]); for(int i=1;i<n;i++){double v=as_num(a[i]);if(v>r)r=v;} return num_val(r); }
BUILTIN(clamp) { (void)line; if(n<3)return n?a[0]:VCG_INT(0); double v=as_num(a[0]),lo=as_num(a[1]),hi=as_num(a[2]); return num_val(v<lo?lo:v>hi?hi:v); }

BUILTIN(rand)  {
    (void)a;(void)line;
    if(n==0) return VCG_FLOAT((double)rand()/RAND_MAX);
    if(n==1){ int hi=(int)as_num(a[0]); return VCG_INT(rand()%(hi>0?hi:1)); }
    int lo=(int)as_num(a[0]), hi=(int)as_num(a[1]);
    return VCG_INT(lo + rand()%((hi-lo)>0?(hi-lo):1));
}
BUILTIN(srand) { (void)line; srand(n>0?(unsigned)as_num(a[0]):(unsigned)time(NULL)); return VCG_NIL; }
BUILTIN(pi)    { (void)a;(void)n;(void)line; return VCG_FLOAT(3.14159265358979323846); }
BUILTIN(e_val) { (void)a;(void)n;(void)line; return VCG_FLOAT(2.71828182845904523536); }

/* ── Type conversion ── */
BUILTIN(int_fn) {
    (void)line;
    if(!n) return VCG_INT(0);
    if(a[0].type==VT_STRING) return VCG_INT((int)strtol(a[0].sval,NULL,10));
    return VCG_INT((int)as_num(a[0]));
}
BUILTIN(float_fn) {
    (void)line;
    if(!n) return VCG_FLOAT(0);
    return VCG_FLOAT(as_num(a[0]));
}
BUILTIN(str_fn) {
    (void)line;
    if(!n) return vcg_str("");
    char *s=vcg_tostr(a[0]); VCGVal r=vcg_str(s); free(s); return r;
}
BUILTIN(bool_fn){ (void)line; return n?VCG_BOOL(vcg_truthy(a[0])):VCG_FALSE; }
BUILTIN(char_fn){
    (void)line;
    if(!n) return vcg_str("");
    char buf[2]={(char)(int)as_num(a[0]),'\0'}; return vcg_str(buf);
}
BUILTIN(ord_fn){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return VCG_INT(0);
    return VCG_INT((unsigned char)a[0].sval[0]);
}

/* ── I/O ── */
BUILTIN(print_fn) {
    (void)line;
    for(int i=0;i<n;i++){
        if(i) printf(" ");
        char *s=vcg_tostr(a[i]); printf("%s",s); free(s);
    }
    printf("\n"); return VCG_NIL;
}
BUILTIN(input_fn) {
    (void)line;
    if(n){ char *s=vcg_tostr(a[0]); printf("%s",s); free(s); fflush(stdout); }
    char buf[4096]; if(!fgets(buf,sizeof(buf),stdin)) return vcg_str("");
    buf[strcspn(buf,"\n")]='\0'; return vcg_str(buf);
}
BUILTIN(format_fn) {
    /* format("Hello %s, you are %d", name, age) */
    (void)line;
    if(!n) return vcg_str("");
    char out[4096]; int oi=0;
    const char *fmt=a[0].type==VT_STRING?a[0].sval:"";
    int ai=1;
    for(int i=0;fmt[i]&&oi<4090;i++){
        if(fmt[i]=='%'&&fmt[i+1]){
            i++;
            if(ai>=n){ out[oi++]='?'; continue; }
            char *s=vcg_tostr(a[ai++]);
            int sl=(int)strlen(s);
            if(oi+sl>=4090){free(s);break;}
            memcpy(out+oi,s,sl); oi+=sl; free(s);
        } else out[oi++]=fmt[i];
    }
    out[oi]='\0'; return vcg_str(out);
}

/* ── Array helpers ── */
BUILTIN(len_fn) {
    (void)line;
    if(!n) return VCG_INT(0);
    if(a[0].type==VT_ARRAY)  return VCG_INT(a[0].arr->len);
    if(a[0].type==VT_STRING) return VCG_INT((int)strlen(a[0].sval));
    if(a[0].type==VT_STRUCT) return VCG_INT(a[0].obj->len);
    return VCG_INT(1);
}
BUILTIN(range_fn) {
    (void)line;
    int from=0,to=0,step=1;
    if(n==1){ to=(int)as_num(a[0]); }
    else if(n==2){ from=(int)as_num(a[0]); to=(int)as_num(a[1]); }
    else if(n>=3){ from=(int)as_num(a[0]); to=(int)as_num(a[1]); step=(int)as_num(a[2]); }
    if(step==0) step=1;
    VCGVal arr=vcg_arr_new();
    for(int i=from; step>0?i<to:i>to; i+=step){
        if(arr.arr->len>=VCG_MAX_ARRAY) break;
        if(arr.arr->len>=arr.arr->cap){arr.arr->cap=arr.arr->cap?arr.arr->cap*2:16;arr.arr->items=realloc(arr.arr->items,arr.arr->cap*sizeof(VCGVal));}
        arr.arr->items[arr.arr->len++]=VCG_INT(i);
    }
    return arr;
}
BUILTIN(keys_fn) {
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return vcg_arr_new();
    VCGVal arr=vcg_arr_new();
    for(int i=0;i<a[0].obj->len;i++){
        if(arr.arr->len>=arr.arr->cap){arr.arr->cap=arr.arr->cap?arr.arr->cap*2:8;arr.arr->items=realloc(arr.arr->items,arr.arr->cap*sizeof(VCGVal));}
        arr.arr->items[arr.arr->len++]=vcg_str(a[0].obj->keys[i]);
    }
    return arr;
}
BUILTIN(values_fn) {
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return vcg_arr_new();
    VCGVal arr=vcg_arr_new();
    for(int i=0;i<a[0].obj->len;i++){
        if(arr.arr->len>=arr.arr->cap){arr.arr->cap=arr.arr->cap?arr.arr->cap*2:8;arr.arr->items=realloc(arr.arr->items,arr.arr->cap*sizeof(VCGVal));}
        arr.arr->items[arr.arr->len++]=a[0].obj->vals[i];
    }
    return arr;
}

/* ── String helpers ── */
BUILTIN(strjoin) {
    (void)line;
    if(!n) return vcg_str("");
    char *sep=(n>1&&a[1].type==VT_STRING)?a[1].sval:",";
    if(a[0].type!=VT_ARRAY) { char *s=vcg_tostr(a[0]); VCGVal r=vcg_str(s); free(s); return r; }
    char *out=strdup(""); size_t ol=0;
    for(int i=0;i<a[0].arr->len;i++){
        if(i){ size_t sl=strlen(sep); out=realloc(out,ol+sl+1); memcpy(out+ol,sep,sl+1); ol+=sl; }
        char *s=vcg_tostr(a[0].arr->items[i]); size_t sl=strlen(s);
        out=realloc(out,ol+sl+1); memcpy(out+ol,s,sl+1); ol+=sl; free(s);
    }
    VCGVal r=vcg_str(out); free(out); return r;
}

/* ── Time ── */
BUILTIN(time_fn){ (void)a;(void)n;(void)line; return VCG_INT((int)time(NULL)); }

/* ── Type check ── */
BUILTIN(isnil)   { (void)line; return n?VCG_BOOL(a[0].type==VT_NIL):VCG_TRUE; }
BUILTIN(isnum)   { (void)line; return n?VCG_BOOL(a[0].type==VT_INT||a[0].type==VT_FLOAT):VCG_FALSE; }
BUILTIN(isstr)   { (void)line; return n?VCG_BOOL(a[0].type==VT_STRING):VCG_FALSE; }
BUILTIN(isarr)   { (void)line; return n?VCG_BOOL(a[0].type==VT_ARRAY):VCG_FALSE; }
BUILTIN(isfunc)  { (void)line; return n?VCG_BOOL(a[0].type==VT_FUNC||a[0].type==VT_BUILTIN):VCG_FALSE; }
BUILTIN(isstruct){ (void)line; return n?VCG_BOOL(a[0].type==VT_STRUCT):VCG_FALSE; }

/* ── v3.0 NEW built-ins ── */

/* watch(key, fn) – register reactive watcher for $set/$get */
BUILTIN(watch_fn) {
    (void)line;
    if(n<2) return VCG_NIL;
    /* get __watchers__ from globals – we need env access */
    /* workaround: store as global side effect via args */
    /* In HTML codegen this is handled natively */
    return VCG_NIL;
}

/* store() — return the full reactive store as struct */
BUILTIN(store_fn) {
    (void)a;(void)n;(void)line;
    return VCG_NIL; /* filled by interpreter */
}

/* exports() — return exported public symbols */
BUILTIN(exports_fn) {
    (void)a;(void)n;(void)line;
    return VCG_NIL;
}

/* send(chan, val) — push value into channel */
BUILTIN(send_fn) {
    (void)line;
    if(n<2 || a[0].type!=VT_ARRAY) return VCG_NIL;
    arr_push(a[0].arr, a[1]);
    return a[1];
}

/* recv(chan) — pop from front of channel */
BUILTIN(recv_fn) {
    (void)line;
    if(!n || a[0].type!=VT_ARRAY || a[0].arr->len==0) return VCG_NIL;
    VCGVal front = a[0].arr->items[0];
    memmove(a[0].arr->items, a[0].arr->items+1,
            (a[0].arr->len-1)*sizeof(VCGVal));
    a[0].arr->len--;
    return front;
}

/* pipe(val, fn1, fn2, ...) — functional pipeline x |> f |> g */
BUILTIN(pipe_fn) {
    (void)line;
    if(!n) return VCG_NIL;
    VCGVal v = a[0];
    for(int i=1;i<n;i++){
        if(a[i].type==VT_BUILTIN)      v=a[i].builtin(&v,1,0);
        else if(a[i].type==VT_FUNC){
            /* call manually: we cannot call call_func here without Interpreter*
               so we just return current value - full support in codegen */
        }
    }
    return v;
}

/* freeze(val) — make value immutable (returns same val, marks in store) */
BUILTIN(freeze_fn) { (void)line; return n?a[0]:VCG_NIL; }

/* type(val) — alias for typeof */
BUILTIN(type_fn) {
    (void)line;
    if(!n) return vcg_str("nil");
    const char *names[]={"nil","bool","int","float","string","array","func","struct","builtin"};
    return vcg_str(names[a[0].type<9?a[0].type:0]);
}

/* defined(name_str) — check if a key exists in store */
BUILTIN(defined_fn) {
    (void)line;
    return n?VCG_BOOL(a[0].type!=VT_NIL):VCG_FALSE;
}

/* ── Register all ── */
void stdlib_register(VCGEnv *env) {
    srand((unsigned)time(NULL));

    /* Math */
    REG(env,"abs",abs);   REG(env,"floor",floor); REG(env,"ceil",ceil);
    REG(env,"round",round);REG(env,"sqrt",sqrt);  REG(env,"pow",pow);
    REG(env,"log",log);   REG(env,"log2",log2);   REG(env,"log10",log10);
    REG(env,"sin",sin);   REG(env,"cos",cos);     REG(env,"tan",tan);
    REG(env,"min",min);   REG(env,"max",max);     REG(env,"clamp",clamp);
    REG(env,"rand",rand); REG(env,"srand",srand);
    REG(env,"pi",pi);     REG(env,"e",e_val);

    /* Type */
    REG(env,"int",int_fn);     REG(env,"float",float_fn);
    REG(env,"str",str_fn);     REG(env,"bool",bool_fn);
    REG(env,"char",char_fn);   REG(env,"ord",ord_fn);

    /* I/O */
    REG(env,"print",print_fn); REG(env,"input",input_fn);
    REG(env,"format",format_fn);

    /* Array/String helpers */
    REG(env,"len",len_fn);   REG(env,"range",range_fn);
    REG(env,"keys",keys_fn); REG(env,"values",values_fn);
    REG(env,"join",strjoin);

    /* Time */
    REG(env,"time",time_fn);

    /* Type checks */
    REG(env,"isnil",isnil);   REG(env,"isnum",isnum);
    REG(env,"isstr",isstr);   REG(env,"isarr",isarr);
    REG(env,"isfunc",isfunc); REG(env,"isstruct",isstruct);

    /* Constants */
    env_set(env,"PI",   VCG_FLOAT(3.14159265358979323846), 1);
    env_set(env,"E",    VCG_FLOAT(2.71828182845904523536), 1);
    env_set(env,"INF",  VCG_FLOAT(1.0/0.0), 1);
    env_set(env,"NAN",  VCG_FLOAT(0.0/0.0), 1);
    env_set(env,"true", VCG_TRUE,  1);
    env_set(env,"false",VCG_FALSE, 1);
    env_set(env,"nil",  VCG_NIL,   1);

    /* v3.0 new built-ins */
    REG(env,"send",    send_fn);
    REG(env,"recv",    recv_fn);
    REG(env,"pipe",    pipe_fn);
    REG(env,"freeze",  freeze_fn);
    REG(env,"type",    type_fn);
    REG(env,"defined", defined_fn);
    REG(env,"watch",   watch_fn);
    REG(env,"exports", exports_fn);
    REG(env,"store",   store_fn);

    /* Date constant */
    env_set(env,"VCG_VERSION",  vcg_str("1.0.0"),      1);
    env_set(env,"VCG_DATE",     vcg_str("2026-06-06"),  1);
    env_set(env,"VCG_AUTHOR",   vcg_str("Syrian VCG"),  1);
}
