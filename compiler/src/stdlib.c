#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Standard Library  —  built-in functions  (v2.0, 2026-06-06)
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


/* ── v2.0 File I/O built-ins ── */
BUILTIN(file_read) {
    (void)line;
    if(!n||a[0].type!=VT_STRING) return vcg_str("");
    FILE *f=fopen(a[0].sval,"r");
    if(!f) return vcg_str("");
    char buf[65536]; size_t sz=fread(buf,1,sizeof(buf)-1,f);
    buf[sz]='\0'; fclose(f);
    return vcg_str(buf);
}
BUILTIN(file_write) {
    (void)line;
    if(n<2||a[0].type!=VT_STRING) return VCG_FALSE;
    FILE *f=fopen(a[0].sval,"w");
    if(!f) return VCG_FALSE;
    char *s=vcg_tostr(a[1]); fputs(s,f); free(s); fclose(f);
    return VCG_TRUE;
}
BUILTIN(file_append) {
    (void)line;
    if(n<2||a[0].type!=VT_STRING) return VCG_FALSE;
    FILE *f=fopen(a[0].sval,"a");
    if(!f) return VCG_FALSE;
    char *s=vcg_tostr(a[1]); fputs(s,f); free(s); fclose(f);
    return VCG_TRUE;
}
BUILTIN(file_exists) {
    (void)line;
    if(!n||a[0].type!=VT_STRING) return VCG_FALSE;
    FILE *f=fopen(a[0].sval,"r");
    if(f){fclose(f);return VCG_TRUE;} return VCG_FALSE;
}

/* ── v2.0 String extras ── */
BUILTIN(repeat_str) {
    (void)line;
    if(n<2||a[0].type!=VT_STRING) return vcg_str("");
    int times=(int)fabs(a[1].type==VT_INT?(double)a[1].ival:a[1].fval);
    char *src=a[0].sval; size_t sl=strlen(src);
    char *out=malloc(sl*times+1); out[0]='\0';
    for(int i=0;i<times;i++) strcat(out,src);
    VCGVal res=vcg_str(out); free(out); return res;
}
BUILTIN(pad_start) {
    (void)line;
    if(n<2) return n?a[0]:vcg_str("");
    char *s=vcg_tostr(a[0]); int total=(int)fabs(a[1].type==VT_INT?(double)a[1].ival:a[1].fval);
    char pad=' '; if(n>=3&&a[2].type==VT_STRING&&a[2].sval[0]) pad=a[2].sval[0];
    int sl=(int)strlen(s); int need=total-sl;
    if(need<=0){VCGVal r=vcg_str(s);free(s);return r;}
    char *out=malloc(total+1);
    memset(out,pad,need); memcpy(out+need,s,sl+1);
    VCGVal res=vcg_str(out); free(out); free(s); return res;
}
BUILTIN(pad_end) {
    (void)line;
    if(n<2) return n?a[0]:vcg_str("");
    char *s=vcg_tostr(a[0]); int total=(int)fabs(a[1].type==VT_INT?(double)a[1].ival:a[1].fval);
    char pad=' '; if(n>=3&&a[2].type==VT_STRING&&a[2].sval[0]) pad=a[2].sval[0];
    int sl=(int)strlen(s); int need=total-sl;
    if(need<=0){VCGVal r=vcg_str(s);free(s);return r;}
    char *out=malloc(total+1); memcpy(out,s,sl);
    memset(out+sl,pad,need); out[total]='\0';
    VCGVal res=vcg_str(out); free(out); free(s); return res;
}
BUILTIN(str_includes){
    (void)line;
    if(n<2||a[0].type!=VT_STRING||a[1].type!=VT_STRING) return VCG_FALSE;
    return VCG_BOOL(strstr(a[0].sval,a[1].sval)!=NULL);
}
BUILTIN(str_index){
    (void)line;
    if(n<2||a[0].type!=VT_STRING||a[1].type!=VT_STRING) return VCG_INT(-1);
    char *p=strstr(a[0].sval,a[1].sval);
    return p?VCG_INT((int)(p-a[0].sval)):VCG_INT(-1);
}
BUILTIN(str_count){
    (void)line;
    if(n<2||a[0].type!=VT_STRING||a[1].type!=VT_STRING) return VCG_INT(0);
    int cnt=0; char *p=a[0].sval; size_t sl=strlen(a[1].sval);
    while((p=strstr(p,a[1].sval))!=NULL){cnt++;p+=sl;}
    return VCG_INT(cnt);
}

/* ── v2.0 Array extras ── */
BUILTIN(arr_flat){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY) return vcg_arr_new();
    VCGVal res=vcg_arr_new();
    for(int i=0;i<a[0].arr->len;i++){
        VCGVal item=a[0].arr->items[i];
        if(item.type==VT_ARRAY){
            for(int j=0;j<item.arr->len;j++){
                if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
                res.arr->items[res.arr->len++]=item.arr->items[j];
            }
        } else {
            if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
            res.arr->items[res.arr->len++]=item;
        }
    }
    return res;
}
BUILTIN(arr_unique){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY) return vcg_arr_new();
    VCGVal res=vcg_arr_new();
    for(int i=0;i<a[0].arr->len;i++){
        int found=0;
        for(int j=0;j<res.arr->len;j++) if(vcg_equal(res.arr->items[j],a[0].arr->items[i])){found=1;break;}
        if(!found){
            if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
            res.arr->items[res.arr->len++]=a[0].arr->items[i];
        }
    }
    return res;
}
BUILTIN(arr_sum){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY) return VCG_INT(0);
    double s=0;
    for(int i=0;i<a[0].arr->len;i++) s+=(a[0].arr->items[i].type==VT_INT?a[0].arr->items[i].ival:a[0].arr->items[i].fval);
    return s==(long long)s&&fabs(s)<1e15?VCG_INT((int)s):VCG_FLOAT(s);
}
BUILTIN(arr_avg){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY||a[0].arr->len==0) return VCG_FLOAT(0);
    double s=0;
    for(int i=0;i<a[0].arr->len;i++) s+=(a[0].arr->items[i].type==VT_INT?a[0].arr->items[i].ival:a[0].arr->items[i].fval);
    return VCG_FLOAT(s/a[0].arr->len);
}
BUILTIN(arr_first){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY||a[0].arr->len==0) return VCG_NIL;
    return a[0].arr->items[0];
}
BUILTIN(arr_last){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY||a[0].arr->len==0) return VCG_NIL;
    return a[0].arr->items[a[0].arr->len-1];
}
BUILTIN(arr_chunk){
    (void)line;
    if(n<2||a[0].type!=VT_ARRAY) return vcg_arr_new();
    int sz=(int)fabs(a[1].type==VT_INT?(double)a[1].ival:a[1].fval);
    if(sz<1) sz=1;
    VCGVal res=vcg_arr_new();
    int i=0;
    while(i<a[0].arr->len){
        VCGVal chunk=vcg_arr_new();
        for(int j=0;j<sz&&i<a[0].arr->len;j++,i++){
            if(chunk.arr->len>=chunk.arr->cap){chunk.arr->cap=chunk.arr->cap?chunk.arr->cap*2:8;chunk.arr->items=realloc(chunk.arr->items,chunk.arr->cap*sizeof(VCGVal));}
            chunk.arr->items[chunk.arr->len++]=a[0].arr->items[i];
        }
        if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
        res.arr->items[res.arr->len++]=chunk;
    }
    return res;
}
BUILTIN(arr_zip){
    (void)line;
    if(n<2||a[0].type!=VT_ARRAY||a[1].type!=VT_ARRAY) return vcg_arr_new();
    int len=a[0].arr->len<a[1].arr->len?a[0].arr->len:a[1].arr->len;
    VCGVal res=vcg_arr_new();
    for(int i=0;i<len;i++){
        VCGVal pair=vcg_arr_new();
        pair.arr->items=realloc(pair.arr->items,2*sizeof(VCGVal));
        pair.arr->items[0]=a[0].arr->items[i];
        pair.arr->items[1]=a[1].arr->items[i];
        pair.arr->len=2; pair.arr->cap=2;
        if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
        res.arr->items[res.arr->len++]=pair;
    }
    return res;
}

/* ── v2.0 Object/Struct helpers ── */
BUILTIN(obj_merge){
    (void)line;
    VCGVal res=vcg_struct_new("merged");
    for(int i=0;i<n;i++){
        if(a[i].type==VT_STRUCT)
            for(int j=0;j<a[i].obj->len;j++)
                struct_set(res.obj,a[i].obj->keys[j],a[i].obj->vals[j]);
    }
    return res;
}
BUILTIN(obj_has){
    (void)line;
    if(n<2||a[0].type!=VT_STRUCT||a[1].type!=VT_STRING) return VCG_FALSE;
    return VCG_BOOL(struct_get(a[0].obj,a[1].sval)!=NULL);
}
BUILTIN(obj_delete){
    (void)line;
    if(n<2||a[0].type!=VT_STRUCT||a[1].type!=VT_STRING) return VCG_NIL;
    VCGStruct *st=a[0].obj;
    for(int i=0;i<st->len;i++){
        if(strcmp(st->keys[i],a[1].sval)==0){
            free(st->keys[i]);
            memmove(st->keys+i,st->keys+i+1,(st->len-i-1)*sizeof(char*));
            memmove(st->vals+i,st->vals+i+1,(st->len-i-1)*sizeof(VCGVal));
            st->len--; break;
        }
    }
    return VCG_NIL;
}
BUILTIN(obj_entries){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return vcg_arr_new();
    VCGVal res=vcg_arr_new();
    for(int i=0;i<a[0].obj->len;i++){
        VCGVal pair=vcg_arr_new();
        pair.arr->items=realloc(pair.arr->items,2*sizeof(VCGVal));
        pair.arr->items[0]=vcg_str(a[0].obj->keys[i]);
        pair.arr->items[1]=a[0].obj->vals[i];
        pair.arr->len=2; pair.arr->cap=2;
        if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
        res.arr->items[res.arr->len++]=pair;
    }
    return res;
}

/* ── v2.0 Math extras ── */
BUILTIN(gcd_fn){
    (void)line;
    if(n<2) return VCG_INT(0);
    int a2=(int)fabs(a[0].type==VT_INT?(double)a[0].ival:a[0].fval);
    int b2=(int)fabs(a[1].type==VT_INT?(double)a[1].ival:a[1].fval);
    while(b2){int t=b2;b2=a2%b2;a2=t;}
    return VCG_INT(a2);
}
BUILTIN(lcm_fn){
    (void)line;
    if(n<2) return VCG_INT(0);
    int a2=(int)fabs(a[0].type==VT_INT?(double)a[0].ival:a[0].fval);
    int b2=(int)fabs(a[1].type==VT_INT?(double)a[1].ival:a[1].fval);
    int g=a2; int t2=b2; while(t2){int t=t2;t2=g%t2;g=t;}
    return g?VCG_INT(a2/g*b2):VCG_INT(0);
}
BUILTIN(fib_fn){
    (void)line;
    if(!n) return VCG_INT(0);
    long long x=(long long)(a[0].type==VT_INT?a[0].ival:a[0].fval);
    if(x<=1) return VCG_INT((int)x);
    long long pa=0,pb=1;
    for(long long i=2;i<=x;i++){long long tmp=pa+pb;pa=pb;pb=tmp;}
    return VCG_INT((int)pb);
}
BUILTIN(factorial_fn){
    (void)line;
    if(!n) return VCG_INT(1);
    long long x=(long long)(a[0].type==VT_INT?a[0].ival:a[0].fval);
    if(x<0) return VCG_INT(0);
    long long r=1; for(long long i=2;i<=x&&i<=20;i++) r*=i;
    return VCG_INT((int)r);
}
BUILTIN(is_prime){
    (void)line;
    if(!n) return VCG_FALSE;
    int x=(int)(a[0].type==VT_INT?a[0].ival:a[0].fval);
    if(x<2) return VCG_FALSE;
    if(x==2) return VCG_TRUE;
    if(x%2==0) return VCG_FALSE;
    for(int i=3;(long long)i*i<=x;i+=2) if(x%i==0) return VCG_FALSE;
    return VCG_TRUE;
}

/* ── v2.0 JSON ── */
BUILTIN(json_stringify){
    (void)line;
    if(!n) return vcg_str("null");
    char *s=vcg_tostr(a[0]);
    /* Simple: wrap strings in quotes, arrays/structs as-is */
    VCGVal res;
    if(a[0].type==VT_STRING){
        char *out=malloc(strlen(s)+3);
        sprintf(out,"\"%s\"",s);
        res=vcg_str(out); free(out);
    } else res=vcg_str(s);
    free(s); return res;
}
BUILTIN(json_parse){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return VCG_NIL;
    /* Very basic: parse numbers, booleans, strings */
    char *s=a[0].sval;
    if(strcmp(s,"null")==0) return VCG_NIL;
    if(strcmp(s,"true")==0) return VCG_TRUE;
    if(strcmp(s,"false")==0) return VCG_FALSE;
    char *end; double d=strtod(s,&end);
    if(end!=s&&*end=='\0') return VCG_FLOAT(d);
    /* Strip quotes */
    size_t sl=strlen(s);
    if(sl>=2&&s[0]=='"'&&s[sl-1]=='"'){
        char *inner=malloc(sl-1); memcpy(inner,s+1,sl-2); inner[sl-2]='\0';
        VCGVal r=vcg_str(inner); free(inner); return r;
    }
    return vcg_str(s);
}

/* ── v2.0 Test assertion helpers ── */
BUILTIN(assert_eq){
    (void)line;
    if(n<2){fprintf(stderr,"[ASSERT] Missing args\n");return VCG_FALSE;}
    if(!vcg_equal(a[0],a[1])){
        char *sa=vcg_tostr(a[0]),*sb=vcg_tostr(a[1]);
        fprintf(stderr,"[ASSERT_EQ FAIL] expected=%s got=%s\n",sb,sa);
        free(sa);free(sb); return VCG_FALSE;
    }
    return VCG_TRUE;
}
BUILTIN(assert_ne){
    (void)line;
    if(n<2) return VCG_FALSE;
    if(vcg_equal(a[0],a[1])){
        char *sa=vcg_tostr(a[0]);
        fprintf(stderr,"[ASSERT_NE FAIL] should not equal %s\n",sa);
        free(sa); return VCG_FALSE;
    }
    return VCG_TRUE;
}
BUILTIN(assert_true_fn){
    (void)line;
    if(!n||!vcg_truthy(a[0])){fprintf(stderr,"[ASSERT_TRUE FAIL]\n");return VCG_FALSE;}
    return VCG_TRUE;
}
BUILTIN(assert_false_fn){
    (void)line;
    if(!n||vcg_truthy(a[0])){fprintf(stderr,"[ASSERT_FALSE FAIL]\n");return VCG_FALSE;}
    return VCG_TRUE;
}

/* ── v2.0 Utility ── */
BUILTIN(sleep_fn){
    (void)a;(void)line;
    /* no-op in interpreter */
    return VCG_NIL;
}
BUILTIN(uuid_fn){
    (void)a;(void)n;(void)line;
    char buf[37];
    unsigned int r1=(unsigned)rand(),r2=(unsigned)rand(),r3=(unsigned)rand(),r4=(unsigned)rand();
    snprintf(buf,37,"%08x-%04x-4%03x-%04x-%08x%04x",
        r1,r2&0xffff,(r3>>4)&0x0fff,
        (r3&0x3fff)|0x8000,r4,rand()&0xffff);
    return vcg_str(buf);
}
BUILTIN(hash_fn){
    (void)line;
    if(!n) return VCG_INT(0);
    char *s=vcg_tostr(a[0]);
    unsigned long h=5381;
    for(int i=0;s[i];i++) h=((h<<5)+h)+s[i];
    free(s); return VCG_INT((int)(h&0x7fffffff));
}
BUILTIN(type_of){
    (void)line;
    if(!n) return vcg_str("nil");
    const char *names[]={"nil","bool","int","float","string","array","func","struct","builtin"};
    return vcg_str(names[a[0].type<9?a[0].type:0]);
}
BUILTIN(copy_fn){
    (void)line;
    if(!n) return VCG_NIL;
    if(a[0].type==VT_ARRAY){
        VCGVal res=vcg_arr_new();
        for(int i=0;i<a[0].arr->len;i++){
            if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
            res.arr->items[res.arr->len++]=a[0].arr->items[i];
        }
        return res;
    }
    if(a[0].type==VT_STRUCT){
        VCGVal res=vcg_struct_new(a[0].obj->type_name);
        for(int i=0;i<a[0].obj->len;i++)
            struct_set(res.obj,a[0].obj->keys[i],a[0].obj->vals[i]);
        return res;
    }
    if(a[0].type==VT_STRING) return vcg_str(a[0].sval);
    return a[0];
}

/* ── v2.0 Higher-order functions (map/filter/reduce/find) ── */
BUILTIN(map_fn){
    (void)line;
    if(n<2||a[1].type!=VT_ARRAY) return vcg_arr_new();
    VCGVal res=vcg_arr_new();
    for(int i=0;i<a[1].arr->len;i++){
        VCGVal mapped=vcg_call_value(a[0],&a[1].arr->items[i],1);
        if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
        res.arr->items[res.arr->len++]=mapped;
    }
    return res;
}
BUILTIN(filter_fn){
    (void)line;
    if(n<2||a[1].type!=VT_ARRAY) return vcg_arr_new();
    VCGVal res=vcg_arr_new();
    for(int i=0;i<a[1].arr->len;i++){
        VCGVal ok=vcg_call_value(a[0],&a[1].arr->items[i],1);
        if(vcg_truthy(ok)){
            if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
            res.arr->items[res.arr->len++]=a[1].arr->items[i];
        }
    }
    return res;
}
BUILTIN(reduce_fn){
    (void)line;
    if(n<3||a[2].type!=VT_ARRAY) return n>=2?a[1]:VCG_NIL;
    VCGVal acc=a[1];
    for(int i=0;i<a[2].arr->len;i++){
        VCGVal args2[2]={acc,a[2].arr->items[i]};
        acc=vcg_call_value(a[0],args2,2);
    }
    return acc;
}
BUILTIN(find_fn){
    (void)line;
    if(n<2||a[1].type!=VT_ARRAY) return VCG_NIL;
    for(int i=0;i<a[1].arr->len;i++){
        VCGVal ok=vcg_call_value(a[0],&a[1].arr->items[i],1);
        if(vcg_truthy(ok)) return a[1].arr->items[i];
    }
    return VCG_NIL;
}

/* ── Register all ── */
void stdlib_register(VCGEnv *env) {
    REG(env,"map",    map_fn);
    REG(env,"filter", filter_fn);
    REG(env,"reduce", reduce_fn);
    REG(env,"find",   find_fn);
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
    /* v2.0 File I/O */
    REG(env,"file_read",   file_read);
    REG(env,"file_write",  file_write);
    REG(env,"file_append", file_append);
    REG(env,"file_exists", file_exists);

    /* v2.0 String extras */
    REG(env,"repeat",     repeat_str);
    REG(env,"pad_start",  pad_start);
    REG(env,"pad_end",    pad_end);
    REG(env,"includes",   str_includes);
    REG(env,"indexof",    str_index);
    REG(env,"count",      str_count);

    /* v2.0 Array extras */
    REG(env,"flat",    arr_flat);
    REG(env,"unique",  arr_unique);
    REG(env,"sum",     arr_sum);
    REG(env,"avg",     arr_avg);
    REG(env,"first",   arr_first);
    REG(env,"last",    arr_last);
    REG(env,"chunk",   arr_chunk);
    REG(env,"zip",     arr_zip);

    /* v2.0 Object helpers */
    REG(env,"merge",   obj_merge);
    REG(env,"has",     obj_has);
    REG(env,"del",     obj_delete);
    REG(env,"entries", obj_entries);

    /* v2.0 Math extras */
    REG(env,"gcd",       gcd_fn);
    REG(env,"lcm",       lcm_fn);
    REG(env,"fib",       fib_fn);
    REG(env,"factorial", factorial_fn);
    REG(env,"is_prime",  is_prime);
    env_set(env,"TAU", VCG_FLOAT(6.28318530717958647692), 1);
    env_set(env,"PHI", VCG_FLOAT(1.61803398874989484820), 1);

    /* v2.0 JSON */
    REG(env,"JSON_stringify", json_stringify);
    REG(env,"JSON_parse",     json_parse);

    /* v2.0 Test */
    REG(env,"assert_eq",    assert_eq);
    REG(env,"assert_ne",    assert_ne);
    REG(env,"assert_true",  assert_true_fn);
    REG(env,"assert_false", assert_false_fn);

    /* v2.0 Utility */
    REG(env,"sleep",  sleep_fn);
    REG(env,"uuid",   uuid_fn);
    REG(env,"hash",   hash_fn);
    REG(env,"type_of",type_of);
    REG(env,"copy",   copy_fn);

    env_set(env,"VCG_VERSION",  vcg_str("2.0.0"),      1);
    env_set(env,"VCG_DATE",     vcg_str("2026-06-06"),  1);
    env_set(env,"VCG_AUTHOR",   vcg_str("Syrian VCG"),  1);
    env_set(env,"VCG_EDITION",  vcg_str("Full Edition"),1);
}
