#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <ctype.h>
#include <strings.h>
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

/* ── v0.2.1 NEW concepts: transmission, time(extended), admob, firebase,
       container, number, pdf, link, link_to, day, name, age,
       middle, right, left, above, below, topbar, head ── */

/* number(x) – عام: يحوّل أي قيمة إلى نوع رقمي (int إن كانت صحيحة وإلا float) */
BUILTIN(number_fn){
    (void)line;
    if(!n) return VCG_INT(0);
    return num_val(as_num(a[0]));
}

/* day() – اسم اليوم الحالي، أو day(timestamp) لاسم يوم زمن معيّن */
BUILTIN(day_fn){
    (void)line;
    time_t t = n>0 ? (time_t)as_num(a[0]) : time(NULL);
    struct tm *lt = localtime(&t);
    static const char *days[]={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
    if(!lt) return vcg_str("");
    return vcg_str(days[lt->tm_wday]);
}

/* age(birthYear) – العمر الحالي بدلالة سنة الميلاد */
BUILTIN(age_fn){
    (void)line;
    if(!n) return VCG_INT(0);
    time_t t=time(NULL); struct tm *lt=localtime(&t);
    int curYear = lt ? (lt->tm_year+1900) : 2026;
    int by = (int)as_num(a[0]);
    int age = curYear - by;
    return VCG_INT(age<0?0:age);
}

/* name(obj) – يقرأ حقل name من أي object/struct، أو يرجّع نفس القيمة كنص إن لم توجد */
BUILTIN(name_fn){
    (void)line;
    if(!n) return vcg_str("");
    if(a[0].type==VT_STRUCT){
        VCGVal *p = struct_get(a[0].obj,"name");
        if(p) return *p;
        return vcg_str(a[0].obj->type_name?a[0].obj->type_name:"");
    }
    char *s=vcg_tostr(a[0]); VCGVal r=vcg_str(s); free(s); return r;
}

/* link(url) – يبني كائن رابط بسيط { url, type:"link" } */
BUILTIN(link_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Link");
    struct_set(s.obj,"url", n>0?a[0]:vcg_str(""));
    struct_set(s.obj,"type", vcg_str("link"));
    return s;
}

/* link_to(url) – كائن انتقال/تنقّل إلى رابط أو صفحة { url, action:"navigate" } */
BUILTIN(link_to_fn){
    (void)line;
    VCGVal s = vcg_struct_new("LinkTo");
    struct_set(s.obj,"url", n>0?a[0]:vcg_str(""));
    struct_set(s.obj,"action", vcg_str("navigate"));
    return s;
}

/* transmission(data) – يمثّل عملية بث/إرسال بيانات (شبكة، إشعار، بث مباشر...) */
BUILTIN(transmission_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Transmission");
    struct_set(s.obj,"payload", n>0?a[0]:VCG_NIL);
    struct_set(s.obj,"status",  vcg_str("sent"));
    struct_set(s.obj,"time",    VCG_INT((int)time(NULL)));
    return s;
}

/* admob(unitId) – كائن إعلانات AdMob: { unitId, status } مع طرق init()/show() */
BUILTIN(admob_show_m){ (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("AdMob"); struct_set(s.obj,"status",vcg_str("shown")); return s; }
BUILTIN(admob_init_m){ (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("AdMob"); struct_set(s.obj,"status",vcg_str("initialized")); return s; }
BUILTIN(admob_fn){
    (void)line;
    VCGVal s = vcg_struct_new("AdMob");
    struct_set(s.obj,"unitId", n>0?a[0]:vcg_str(""));
    struct_set(s.obj,"status", vcg_str("ready"));
    VCGVal m;
    m.type=VT_BUILTIN; m.builtin=bi_admob_init_m; struct_set(s.obj,"init", m);
    m.type=VT_BUILTIN; m.builtin=bi_admob_show_m; struct_set(s.obj,"show", m);
    return s;
}

/* firebase(config) – كائن مشروع Firebase مع get(key)/set(key,value) بسيطة (تخزين ذاكرة) */
BUILTIN(firebase_get_m){
    (void)line;
    if(n<2||a[0].type!=VT_STRUCT) return VCG_NIL;
    VCGVal *store = struct_get(a[0].obj,"_store");
    if(!store||store->type!=VT_STRUCT) return VCG_NIL;
    char *k=vcg_tostr(a[1]); VCGVal *v=struct_get(store->obj,k); free(k);
    return v?*v:VCG_NIL;
}
BUILTIN(firebase_set_m){
    (void)line;
    if(n<3||a[0].type!=VT_STRUCT) return a[0];
    VCGVal *store = struct_get(a[0].obj,"_store");
    if(!store||store->type!=VT_STRUCT) return a[0];
    char *k=vcg_tostr(a[1]); struct_set(store->obj,k,a[2]); free(k);
    return a[0];
}
BUILTIN(firebase_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Firebase");
    struct_set(s.obj,"config", n>0?a[0]:VCG_NIL);
    struct_set(s.obj,"status", vcg_str("connected"));
    struct_set(s.obj,"_store", vcg_struct_new("object"));
    VCGVal m;
    m.type=VT_BUILTIN; m.builtin=bi_firebase_get_m; struct_set(s.obj,"get", m);
    m.type=VT_BUILTIN; m.builtin=bi_firebase_set_m; struct_set(s.obj,"set", m);
    return s;
}

/* pdf(path) – ينشئ ملف PDF فعليّ صالح (صفحة واحدة فارغة) في المسار المحدد */
BUILTIN(pdf_fn){
    (void)line;
    const char *path = (n>0&&a[0].type==VT_STRING)?a[0].sval:"output.pdf";
    const char *title = (n>1&&a[1].type==VT_STRING)?a[1].sval:"VCG PDF";
    FILE *f=fopen(path,"wb");
    VCGVal s = vcg_struct_new("Pdf");
    struct_set(s.obj,"path", vcg_str(path));
    if(!f){ struct_set(s.obj,"status",vcg_str("error")); return s; }
    fprintf(f,
        "%%PDF-1.4\n"
        "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
        "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
        "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>endobj\n"
        "4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n"
        "5 0 obj<</Length 64>>stream\n"
        "BT /F1 18 Tf 50 720 Td (%s) Tj ET\n"
        "endstream\nendobj\n"
        "trailer<</Root 1 0 R>>\n", title);
    fclose(f);
    struct_set(s.obj,"status", vcg_str("created"));
    return s;
}

/* container(...) – صانع وعاء/حاوية عامة لعناصر الواجهة، يقبل أبناء متعددين */
BUILTIN(container_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Container");
    VCGVal children = vcg_arr_new();
    for(int i=0;i<n;i++){
        if(children.arr->len>=children.arr->cap){children.arr->cap=children.arr->cap?children.arr->cap*2:8;children.arr->items=realloc(children.arr->items,children.arr->cap*sizeof(VCGVal));}
        children.arr->items[children.arr->len++]=a[i];
    }
    struct_set(s.obj,"children", children);
    return s;
}

/* music(src) – عنصر صوت/موسيقى { src, status } مع play()/pause()/stop() */
BUILTIN(music_play_m) { (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("Music"); struct_set(s.obj,"status",vcg_str("playing")); return s; }
BUILTIN(music_pause_m){ (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("Music"); struct_set(s.obj,"status",vcg_str("paused"));  return s; }
BUILTIN(music_stop_m) { (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("Music"); struct_set(s.obj,"status",vcg_str("stopped")); return s; }
BUILTIN(music_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Music");
    struct_set(s.obj,"src", n>0?a[0]:vcg_str(""));
    struct_set(s.obj,"status", vcg_str("ready"));
    VCGVal m;
    m.type=VT_BUILTIN; m.builtin=bi_music_play_m;  struct_set(s.obj,"play",  m);
    m.type=VT_BUILTIN; m.builtin=bi_music_pause_m; struct_set(s.obj,"pause", m);
    m.type=VT_BUILTIN; m.builtin=bi_music_stop_m;  struct_set(s.obj,"stop",  m);
    return s;
}

/* loading(...) – مؤشّر تحميل { progress, status } مع start()/stop()/set(percent) */
BUILTIN(loading_start_m){ (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("Loading"); struct_set(s.obj,"status",vcg_str("loading")); return s; }
BUILTIN(loading_stop_m) { (void)a;(void)n;(void)line; VCGVal s=vcg_struct_new("Loading"); struct_set(s.obj,"status",vcg_str("done"));    return s; }
BUILTIN(loading_set_m)  {
    (void)line;
    VCGVal s=vcg_struct_new("Loading");
    if(n>=2 && a[0].type==VT_STRUCT){
        double pct = n>=2?as_num(a[1]):0;
        struct_set(a[0].obj,"progress", num_val(pct));
        struct_set(a[0].obj,"status", vcg_str(pct>=100?"done":"loading"));
        return a[0];
    }
    struct_set(s.obj,"status",vcg_str("loading")); return s;
}
BUILTIN(loading_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Loading");
    struct_set(s.obj,"progress", n>0?num_val(as_num(a[0])):VCG_INT(0));
    struct_set(s.obj,"status", vcg_str("idle"));
    VCGVal m;
    m.type=VT_BUILTIN; m.builtin=bi_loading_start_m; struct_set(s.obj,"start", m);
    m.type=VT_BUILTIN; m.builtin=bi_loading_stop_m;  struct_set(s.obj,"stop",  m);
    m.type=VT_BUILTIN; m.builtin=bi_loading_set_m;   struct_set(s.obj,"set",   m);
    return s;
}

/* bar(value, max) – شريط تقدّم/قياس عام (progress/volume/health...) { value, max, percent } */
BUILTIN(bar_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Bar");
    double val = n>0?as_num(a[0]):0;
    double mx  = n>1?as_num(a[1]):100;
    if(mx==0) mx=100;
    struct_set(s.obj,"value", num_val(val));
    struct_set(s.obj,"max",   num_val(mx));
    struct_set(s.obj,"percent", num_val((val/mx)*100.0));
    return s;
}

/* edges(top,right,bottom,left) – قيم حواف/تباعد (margin/padding/border) للواجهة */
BUILTIN(edges_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Edges");
    double top=n>0?as_num(a[0]):0;
    double right=n>1?as_num(a[1]):top;
    double bottom=n>2?as_num(a[2]):top;
    double left=n>3?as_num(a[3]):right;
    struct_set(s.obj,"top",    num_val(top));
    struct_set(s.obj,"right",  num_val(right));
    struct_set(s.obj,"bottom", num_val(bottom));
    struct_set(s.obj,"left",   num_val(left));
    return s;
}

/* impact(level) – نبضة/تأثير لمسي أو حركي (haptic/feedback) { level, status } */
BUILTIN(impact_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Impact");
    const char *lvl = (n>0&&a[0].type==VT_STRING)?a[0].sval:"medium";
    struct_set(s.obj,"level",  vcg_str(lvl));
    struct_set(s.obj,"status", vcg_str("triggered"));
    struct_set(s.obj,"time",   VCG_INT((int)time(NULL)));
    return s;
}

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
/* kind(x) — مثل type_of لكن للـ struct يرجع اسمه الموسوم (Text/Button/UI/Style/Design/Color/...) */
BUILTIN(kind_fn){
    (void)line;
    if(!n) return vcg_str("nil");
    if(a[0].type==VT_STRUCT) return vcg_str(a[0].obj->type_name ? a[0].obj->type_name : "object");
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

/* ── v2.1 NEW: String manipulation ── */
BUILTIN(str_split){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return vcg_arr_new();
    const char *sep = (n>1 && a[1].type==VT_STRING && a[1].sval[0]) ? a[1].sval : NULL;
    VCGVal res = vcg_arr_new();
    const char *s = a[0].sval;
    if(!sep){
        /* split into individual chars when no separator given */
        for(size_t i=0; s[i]; i++){
            char buf[2]={s[i],'\0'};
            arr_push(res.arr, vcg_str(buf));
        }
        return res;
    }
    size_t seplen = strlen(sep);
    const char *start = s;
    const char *p;
    while((p = strstr(start, sep)) != NULL){
        size_t partlen = (size_t)(p - start);
        char *part = malloc(partlen + 1);
        memcpy(part, start, partlen); part[partlen]='\0';
        VCGVal v = vcg_str(part); free(part);
        arr_push(res.arr, v);
        start = p + seplen;
    }
    arr_push(res.arr, vcg_str(start));
    return res;
}
BUILTIN(str_replace){
    (void)line;
    if(n<3||a[0].type!=VT_STRING||a[1].type!=VT_STRING||a[2].type!=VT_STRING) return n?a[0]:vcg_str("");
    const char *src=a[0].sval, *from=a[1].sval, *to=a[2].sval;
    size_t fromlen=strlen(from), tolen=strlen(to);
    if(fromlen==0) return vcg_str(src);
    char *out=strdup(""); size_t ol=0; const char *p=src;
    const char *hit;
    while((hit=strstr(p,from))!=NULL){
        size_t prelen=(size_t)(hit-p);
        out=realloc(out, ol+prelen+tolen+1);
        memcpy(out+ol,p,prelen); ol+=prelen;
        memcpy(out+ol,to,tolen); ol+=tolen;
        out[ol]='\0';
        p = hit + fromlen;
    }
    size_t restlen=strlen(p);
    out=realloc(out, ol+restlen+1);
    memcpy(out+ol,p,restlen+1);
    VCGVal r=vcg_str(out); free(out); return r;
}
BUILTIN(str_trim){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return n?a[0]:vcg_str("");
    const char *s=a[0].sval; size_t len=strlen(s);
    size_t start=0, end=len;
    while(start<end && isspace((unsigned char)s[start])) start++;
    while(end>start && isspace((unsigned char)s[end-1])) end--;
    char *out=malloc(end-start+1);
    memcpy(out,s+start,end-start); out[end-start]='\0';
    VCGVal r=vcg_str(out); free(out); return r;
}
BUILTIN(str_upper){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return n?a[0]:vcg_str("");
    char *out=strdup(a[0].sval);
    for(char *p=out; *p; p++) *p=(char)toupper((unsigned char)*p);
    VCGVal r=vcg_str(out); free(out); return r;
}
BUILTIN(str_lower){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return n?a[0]:vcg_str("");
    char *out=strdup(a[0].sval);
    for(char *p=out; *p; p++) *p=(char)tolower((unsigned char)*p);
    VCGVal r=vcg_str(out); free(out); return r;
}
BUILTIN(str_starts_with){
    (void)line;
    if(n<2||a[0].type!=VT_STRING||a[1].type!=VT_STRING) return VCG_FALSE;
    size_t pl=strlen(a[1].sval);
    return VCG_BOOL(strncmp(a[0].sval, a[1].sval, pl)==0);
}
BUILTIN(str_ends_with){
    (void)line;
    if(n<2||a[0].type!=VT_STRING||a[1].type!=VT_STRING) return VCG_FALSE;
    size_t sl=strlen(a[0].sval), pl=strlen(a[1].sval);
    if(pl>sl) return VCG_FALSE;
    return VCG_BOOL(strcmp(a[0].sval+sl-pl, a[1].sval)==0);
}

/* ── v2.1 NEW: Array mutation/slicing ── */
BUILTIN(arr_push_fn){
    (void)line;
    if(n<2||a[0].type!=VT_ARRAY) return n?a[0]:VCG_NIL;
    arr_push(a[0].arr, a[1]);
    return a[0];
}
BUILTIN(arr_pop_fn){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY||a[0].arr->len==0) return VCG_NIL;
    VCGVal v=a[0].arr->items[a[0].arr->len-1];
    a[0].arr->len--;
    return v;
}
BUILTIN(arr_shift_fn){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY||a[0].arr->len==0) return VCG_NIL;
    VCGVal v=a[0].arr->items[0];
    memmove(a[0].arr->items, a[0].arr->items+1, (a[0].arr->len-1)*sizeof(VCGVal));
    a[0].arr->len--;
    return v;
}
BUILTIN(arr_unshift_fn){
    (void)line;
    if(n<2||a[0].type!=VT_ARRAY) return n?a[0]:VCG_NIL;
    VCGArray *ar=a[0].arr;
    if(ar->len>=ar->cap){ar->cap=ar->cap?ar->cap*2:8; ar->items=realloc(ar->items, ar->cap*sizeof(VCGVal));}
    memmove(ar->items+1, ar->items, ar->len*sizeof(VCGVal));
    ar->items[0]=a[1];
    ar->len++;
    return a[0];
}
BUILTIN(arr_reverse_fn){
    (void)line;
    if(!n) return VCG_NIL;
    if(a[0].type==VT_STRING){
        char *s=strdup(a[0].sval); size_t l=strlen(s);
        for(size_t i=0;i<l/2;i++){ char t=s[i]; s[i]=s[l-1-i]; s[l-1-i]=t; }
        VCGVal r=vcg_str(s); free(s); return r;
    }
    if(a[0].type!=VT_ARRAY) return a[0];
    VCGVal res=vcg_arr_new();
    for(int i=a[0].arr->len-1;i>=0;i--) arr_push(res.arr, a[0].arr->items[i]);
    return res;
}
static int vcg_compare_default(const void *pa, const void *pb){
    VCGVal a=*(const VCGVal*)pa, b=*(const VCGVal*)pb;
    if((a.type==VT_INT||a.type==VT_FLOAT)&&(b.type==VT_INT||b.type==VT_FLOAT)){
        double da=a.type==VT_INT?a.ival:a.fval, db=b.type==VT_INT?b.ival:b.fval;
        return da<db?-1:(da>db?1:0);
    }
    if(a.type==VT_STRING&&b.type==VT_STRING) return strcmp(a.sval,b.sval);
    return 0;
}
BUILTIN(arr_sort_fn){
    (void)line;
    if(!n||a[0].type!=VT_ARRAY) return n?a[0]:vcg_arr_new();
    VCGVal res=vcg_arr_new();
    for(int i=0;i<a[0].arr->len;i++) arr_push(res.arr, a[0].arr->items[i]);
    if(n>=2 && (a[1].type==VT_FUNC || a[1].type==VT_BUILTIN)){
        /* simple insertion sort using a comparator function(a,b) -> negative/0/positive or bool */
        for(int i=1;i<res.arr->len;i++){
            VCGVal key=res.arr->items[i];
            int j=i-1;
            while(j>=0){
                VCGVal args2[2]={res.arr->items[j], key};
                VCGVal cmp=vcg_call_value(a[1], args2, 2);
                double c = (cmp.type==VT_INT)?cmp.ival : (cmp.type==VT_FLOAT?cmp.fval : (vcg_truthy(cmp)?1:-1));
                if(c<=0) break;
                res.arr->items[j+1]=res.arr->items[j];
                j--;
            }
            res.arr->items[j+1]=key;
        }
    } else {
        qsort(res.arr->items, res.arr->len, sizeof(VCGVal), vcg_compare_default);
    }
    return res;
}
BUILTIN(arr_str_slice){
    (void)line;
    if(!n) return VCG_NIL;
    int len = a[0].type==VT_ARRAY ? a[0].arr->len : (a[0].type==VT_STRING ? (int)strlen(a[0].sval) : 0);
    int start = n>1 ? (int)as_num(a[1]) : 0;
    int end   = n>2 ? (int)as_num(a[2]) : len;
    if(start<0) start += len; if(end<0) end += len;
    if(start<0) start=0; if(end>len) end=len;
    if(start>end) start=end;
    if(a[0].type==VT_STRING){
        int sl=end-start;
        char *out=malloc(sl+1);
        memcpy(out, a[0].sval+start, sl); out[sl]='\0';
        VCGVal r=vcg_str(out); free(out); return r;
    }
    if(a[0].type==VT_ARRAY){
        VCGVal res=vcg_arr_new();
        for(int i=start;i<end;i++) arr_push(res.arr, a[0].arr->items[i]);
        return res;
    }
    return VCG_NIL;
}

/* ── v2.1 NEW: Base64 ── */
static const char B64_CHARS[]="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
BUILTIN(base64_encode_fn){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return vcg_str("");
    const unsigned char *s=(const unsigned char*)a[0].sval; size_t len=strlen(a[0].sval);
    size_t outlen = ((len+2)/3)*4;
    char *out=malloc(outlen+1); size_t oi=0;
    size_t i=0;
    for(; i+2<len; i+=3){
        unsigned v = (s[i]<<16)|(s[i+1]<<8)|s[i+2];
        out[oi++]=B64_CHARS[(v>>18)&0x3F]; out[oi++]=B64_CHARS[(v>>12)&0x3F];
        out[oi++]=B64_CHARS[(v>>6)&0x3F];  out[oi++]=B64_CHARS[v&0x3F];
    }
    if(len-i==1){
        unsigned v = s[i]<<16;
        out[oi++]=B64_CHARS[(v>>18)&0x3F]; out[oi++]=B64_CHARS[(v>>12)&0x3F];
        out[oi++]='='; out[oi++]='=';
    } else if(len-i==2){
        unsigned v = (s[i]<<16)|(s[i+1]<<8);
        out[oi++]=B64_CHARS[(v>>18)&0x3F]; out[oi++]=B64_CHARS[(v>>12)&0x3F];
        out[oi++]=B64_CHARS[(v>>6)&0x3F];  out[oi++]='=';
    }
    out[oi]='\0';
    VCGVal r=vcg_str(out); free(out); return r;
}
static int b64_val(char c){
    if(c>='A'&&c<='Z') return c-'A';
    if(c>='a'&&c<='z') return c-'a'+26;
    if(c>='0'&&c<='9') return c-'0'+52;
    if(c=='+') return 62;
    if(c=='/') return 63;
    return -1;
}
BUILTIN(base64_decode_fn){
    (void)line;
    if(!n||a[0].type!=VT_STRING) return vcg_str("");
    const char *s=a[0].sval; size_t len=strlen(s);
    char *out=malloc(len+1); size_t oi=0;
    int vals[4]; int vc=0;
    for(size_t i=0;i<len;i++){
        if(s[i]=='='||s[i]=='\n'||s[i]=='\r') continue;
        int v=b64_val(s[i]);
        if(v<0) continue;
        vals[vc++]=v;
        if(vc==4){
            out[oi++]=(char)((vals[0]<<2)|(vals[1]>>4));
            out[oi++]=(char)((vals[1]<<4)|(vals[2]>>2));
            out[oi++]=(char)((vals[2]<<6)|vals[3]);
            vc=0;
        }
    }
    if(vc==2){ out[oi++]=(char)((vals[0]<<2)|(vals[1]>>4)); }
    else if(vc==3){ out[oi++]=(char)((vals[0]<<2)|(vals[1]>>4)); out[oi++]=(char)((vals[1]<<4)|(vals[2]>>2)); }
    out[oi]='\0';
    VCGVal r=vcg_str(out); free(out); return r;
}

/* ── v2.1 NEW: Color / Style / Design ── */
typedef struct { const char *name; int r,g,b; } NamedColor;
static const NamedColor VCG_NAMED_COLORS[] = {
    {"red",       220, 53,  69 },
    {"green",     40,  167, 69 },
    {"blue",      13,  110, 253},
    {"yellow",    255, 193, 7  },
    {"orange",    253, 126, 20 },
    {"purple",    111, 66,  193},
    {"pink",      214, 51,  132},
    {"teal",      32,  201, 151},
    {"cyan",      13,  202, 240},
    {"black",     0,   0,   0  },
    {"white",     255, 255, 255},
    {"gray",      108, 117, 125},
    {"olive",     61,  74,  47 },   /* الثيم الزيتوني الأساسي لـ VCG */
    {"vcg_olive", 61,  74,  47 },
    {"vcg_dark",  26,  29,  20 },
    {"vcg_accent",77,  166, 90 },
    {NULL,0,0,0}
};
static int hexval(char c){
    if(c>='0'&&c<='9') return c-'0';
    if(c>='a'&&c<='f') return c-'a'+10;
    if(c>='A'&&c<='F') return c-'A'+10;
    return -1;
}
static VCGVal vcg_make_color_struct(int r,int g,int b,double alpha){
    VCGVal s = vcg_struct_new("Color");
    struct_set(s.obj,"r",VCG_INT(r));
    struct_set(s.obj,"g",VCG_INT(g));
    struct_set(s.obj,"b",VCG_INT(b));
    struct_set(s.obj,"a",VCG_FLOAT(alpha));
    char hex[8]; snprintf(hex,sizeof(hex),"#%02X%02X%02X",
        r<0?0:(r>255?255:r), g<0?0:(g>255?255:g), b<0?0:(b>255?255:b));
    struct_set(s.obj,"hex",vcg_str(hex));
    char rgb[40]; snprintf(rgb,sizeof(rgb),"rgb(%d,%d,%d)",r,g,b);
    struct_set(s.obj,"rgb",vcg_str(rgb));
    char rgba[60]; snprintf(rgba,sizeof(rgba),"rgba(%d,%d,%d,%.2f)",r,g,b,alpha);
    struct_set(s.obj,"rgba",vcg_str(rgba));
    return s;
}
/* color("#RRGGBB") | color("red") | color(r,g,b) | color(r,g,b,a) → struct{r,g,b,a,hex,rgb,rgba} */
static VCGVal color_logic(VCGVal *a, int n){
    if(!n) return vcg_make_color_struct(0,0,0,1.0);
    if(n>=3){
        int r=(int)as_num(a[0]), g=(int)as_num(a[1]), b=(int)as_num(a[2]);
        double al = n>=4 ? as_num(a[3]) : 1.0;
        return vcg_make_color_struct(r,g,b,al);
    }
    if(a[0].type==VT_STRING){
        const char *s=a[0].sval;
        if(s[0]=='#'){
            size_t len=strlen(s);
            int r=0,g=0,b=0;
            if(len==7){ r=hexval(s[1])*16+hexval(s[2]); g=hexval(s[3])*16+hexval(s[4]); b=hexval(s[5])*16+hexval(s[6]); }
            else if(len==4){ r=hexval(s[1])*17; g=hexval(s[2])*17; b=hexval(s[3])*17; }
            return vcg_make_color_struct(r,g,b,1.0);
        }
        for(int i=0; VCG_NAMED_COLORS[i].name; i++){
            if(strcasecmp(s, VCG_NAMED_COLORS[i].name)==0){
                const NamedColor *c=&VCG_NAMED_COLORS[i];
                return vcg_make_color_struct(c->r,c->g,c->b,1.0);
            }
        }
    }
    return vcg_make_color_struct(0,0,0,1.0);
}
BUILTIN(color_fn){ (void)line; return color_logic(a,n); }
/* color_argv(arr) — نفس منطق color() لكن مدخلاته مصفوفة واحدة (تُستخدم لدعم
   btn.color(...) / text.color(...) عبر تمرير معاملات متغيّرة args.. كمصفوفة) */
BUILTIN(color_argv_fn){
    (void)line;
    if(!n || a[0].type!=VT_ARRAY) return vcg_make_color_struct(0,0,0,1.0);
    VCGArray *ar=a[0].arr;
    VCGVal tmp[8]; int tn = ar->len>8?8:ar->len;
    for(int i=0;i<tn;i++) tmp[i]=ar->items[i];
    return color_logic(tmp, tn);
}

/* style({...}) / design({...}) — wraps/tags a struct so it can be passed to
   $set("style", style({...})) or $set("design", design({...})).
   Non-struct values pass through untouched but are still returned. */
BUILTIN(style_fn){
    (void)line;
    if(!n) return vcg_struct_new("Style");
    if(a[0].type==VT_STRUCT){ free(a[0].obj->type_name); a[0].obj->type_name=strdup("Style"); return a[0]; }
    VCGVal s=vcg_struct_new("Style"); struct_set(s.obj,"value",a[0]); return s;
}
BUILTIN(design_fn){
    (void)line;
    if(!n) return vcg_struct_new("Design");
    if(a[0].type==VT_STRUCT){ free(a[0].obj->type_name); a[0].obj->type_name=strdup("Design"); return a[0]; }
    VCGVal s=vcg_struct_new("Design"); struct_set(s.obj,"value",a[0]); return s;
}

/* store_zip() — returns the whole reactive ($set/$get) store zipped into
   an array of [key, value] pairs. Equivalent idea to "$get.zip()". */
extern VCGEnv *vcg_globals_for_stdlib(void);
BUILTIN(store_zip_fn){
    (void)a;(void)n;(void)line;
    VCGVal res = vcg_arr_new();
    VCGEnv *globals = vcg_globals_for_stdlib();
    if(!globals) return res;
    VCGVal *store = env_get(globals, "__store__");
    if(!store || store->type!=VT_STRUCT) return res;
    for(int i=0;i<store->obj->len;i++){
        VCGVal pair = vcg_arr_new();
        arr_push(pair.arr, vcg_str(store->obj->keys[i]));
        arr_push(pair.arr, store->obj->vals[i]);
        arr_push(res.arr, pair);
    }
    return res;
}

/* ── v2.1 NEW: UI components (text / text_s / btn / ui) ──
   هذه struct خفيفة لتمثيل عناصر واجهة بسيطة، يمكن دمجها مع $set("ui", ...)
   ثم قراءتها لاحقاً بمحرر/مولّد HTML خارجي عبر type_of(el) == "Text"/"Button"/"UI". */

/* el.color("#fff") / el.color("vcg_olive") / el.color(r,g,b[,a])
   تُستدعى كـ method على struct ناتج عن text()/text_s()/btn():
       btn("اضغط هنا").color("#FF6B6B")
       text("...").color("vcg_olive")
       btn("...").color(20,200,100,0.8)
   تُحدّث الحقل المناسب داخل style (bg لو Button، color لو Text) وترجع self لإتاحة chaining. */
BUILTIN(el_color_method){
    (void)line;
    if(!n || a[0].type!=VT_STRUCT) return n?a[0]:VCG_NIL;
    VCGVal self = a[0];
    VCGVal c = color_logic(a+1, n-1);
    VCGVal *styleField = struct_get(self.obj, "style");
    if(!styleField || styleField->type!=VT_STRUCT){
        VCGVal newStyle = vcg_struct_new("Style");
        struct_set(self.obj, "style", newStyle);
        styleField = struct_get(self.obj, "style");
    }
    if(self.obj->type_name && strcmp(self.obj->type_name,"Button")==0)
        struct_set(styleField->obj, "bg", *struct_get(c.obj,"hex"));
    else
        struct_set(styleField->obj, "color", *struct_get(c.obj,"hex"));
    struct_set(styleField->obj, "_color_obj", c); /* الكائن الكامل {r,g,b,a,hex,rgb,rgba} */
    return self;
}

/* text(content) → {type:"text", content, style:{}} */
BUILTIN(text_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Text");
    struct_set(s.obj,"content", n? a[0] : vcg_str(""));
    struct_set(s.obj,"style",   vcg_struct_new("Style"));
    VCGVal m; m.type=VT_BUILTIN; m.builtin=bi_el_color_method;
    struct_set(s.obj,"color", m);
    return s;
}
/* text_s(content, style) → نص بستايل مخصّص (text_s = "text styled") */
BUILTIN(text_s_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Text");
    struct_set(s.obj,"content", n>=1 ? a[0] : vcg_str(""));
    struct_set(s.obj,"style",   n>=2 ? a[1] : vcg_struct_new("Style"));
    VCGVal m; m.type=VT_BUILTIN; m.builtin=bi_el_color_method;
    struct_set(s.obj,"color", m);
    return s;
}
/* btn("نص الزر" [, onclick_name] [, style]) → {type:"button", label, onclick, style} */
BUILTIN(btn_fn){
    (void)line;
    VCGVal s = vcg_struct_new("Button");
    struct_set(s.obj,"label",   n>=1 ? a[0] : vcg_str("زر"));
    struct_set(s.obj,"onclick", n>=2 ? a[1] : VCG_NIL);
    struct_set(s.obj,"style",   n>=3 ? a[2] : vcg_struct_new("Style"));
    VCGVal m; m.type=VT_BUILTIN; m.builtin=bi_el_color_method;
    struct_set(s.obj,"color", m);
    return s;
}
/* ui(el1, el2, el3, ...) → يجمع عناصر الواجهة في شجرة واحدة { type:"ui", children:[...] }
   يُستخدم مباشرة مع $set("ui", ui(text(...), btn(...), ...)) */
BUILTIN(ui_fn){
    (void)line;
    VCGVal s = vcg_struct_new("UI");
    VCGVal children = vcg_arr_new();
    for(int i=0;i<n;i++) arr_push(children.arr, a[i]);
    struct_set(s.obj,"children", children);
    return s;
}

/* ── v2.1 NEW: Settings (إعدادات تطبيق/صفحة كاملة) ──
   تُبنى بالكامل كـ builtin methods (لا VCG classes) لضمان أن التغييرات
   تُحفظ فعلياً عند الإرجاع، مما يتيح استدعاءً متسلسلاً (chaining) موثوقاً:
       settings.name("...").package("...").version("...").icon("icons/icon.png")
       settings.background.color("#fff")
*/
BUILTIN(settings_name_m){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return n?a[0]:VCG_NIL;
    if(n>=2){ struct_set(a[0].obj,"_name",a[1]); return a[0]; }
    VCGVal *v=struct_get(a[0].obj,"_name"); return v?*v:vcg_str("");
}
BUILTIN(settings_package_m){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return n?a[0]:VCG_NIL;
    if(n>=2){ struct_set(a[0].obj,"_package",a[1]); return a[0]; }
    VCGVal *v=struct_get(a[0].obj,"_package"); return v?*v:vcg_str("");
}
BUILTIN(settings_version_m){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return n?a[0]:VCG_NIL;
    if(n>=2){ struct_set(a[0].obj,"_version",a[1]); return a[0]; }
    VCGVal *v=struct_get(a[0].obj,"_version"); return v?*v:vcg_str("");
}
BUILTIN(settings_icon_m){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return n?a[0]:VCG_NIL;
    if(n>=2){ struct_set(a[0].obj,"_icon",a[1]); return a[0]; }
    VCGVal *v=struct_get(a[0].obj,"_icon"); return v?*v:VCG_NIL;
}
/* background.color("#fff") | .color("vcg_olive") | .color(r,g,b[,a]) — يحدّث background.value
   ويُحدّث أيضاً _background_hex في الأب (Settings) إن وُجد ربط _parent. */
BUILTIN(settings_bg_color_m){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return n?a[0]:VCG_NIL;
    VCGVal c = color_logic(a+1, n-1);
    struct_set(a[0].obj,"value",c);
    VCGVal *parent = struct_get(a[0].obj,"_parent");
    if(parent && parent->type==VT_STRUCT){
        VCGVal *hex = struct_get(c.obj,"hex");
        if(hex) struct_set(parent->obj,"_background_hex", *hex);
    }
    return a[0];
}
/* settings.snapshot() → struct بكل القيم الحالية (name/package/version/icon/background) جاهز للتخزين */
BUILTIN(settings_snapshot_m){
    (void)line;
    if(!n||a[0].type!=VT_STRUCT) return vcg_struct_new("object");
    VCGVal out = vcg_struct_new("object");
    VCGVal *p;
    p=struct_get(a[0].obj,"_name");    struct_set(out.obj,"name",    p?*p:vcg_str(""));
    p=struct_get(a[0].obj,"_package"); struct_set(out.obj,"package", p?*p:vcg_str(""));
    p=struct_get(a[0].obj,"_version"); struct_set(out.obj,"version", p?*p:vcg_str(""));
    p=struct_get(a[0].obj,"_icon");    struct_set(out.obj,"icon",    p?*p:VCG_NIL);
    p=struct_get(a[0].obj,"_background_hex"); struct_set(out.obj,"background", p?*p:vcg_str(""));
    return out;
}
/* settings_new() → ينشئ كائن إعدادات تطبيق/صفحة جديد بكل الحقول والـ methods أعلاه */
BUILTIN(settings_new_fn){
    (void)n;(void)a;(void)line;
    VCGVal s = vcg_struct_new("Settings");
    struct_set(s.obj,"_name",    vcg_str("تطبيق VCG"));
    struct_set(s.obj,"_package", vcg_str("com.syrianvcg.app"));
    struct_set(s.obj,"_version", vcg_str("1.0.0"));
    struct_set(s.obj,"_icon",    VCG_NIL);

    VCGVal bg = vcg_struct_new("Background");
    VCGVal defaultColor = vcg_make_color_struct(26,29,20,1.0); /* vcg_dark */
    struct_set(bg.obj,"value", defaultColor);
    struct_set(bg.obj,"_parent", s);
    VCGVal bgColorMethod; bgColorMethod.type=VT_BUILTIN; bgColorMethod.builtin=bi_settings_bg_color_m;
    struct_set(bg.obj,"color", bgColorMethod);
    struct_set(s.obj,"background", bg);
    VCGVal *hex=struct_get(defaultColor.obj,"hex");
    struct_set(s.obj,"_background_hex", hex?*hex:vcg_str("#1A1D14"));

    VCGVal m;
    m.type=VT_BUILTIN; m.builtin=bi_settings_name_m;     struct_set(s.obj,"name",     m);
    m.type=VT_BUILTIN; m.builtin=bi_settings_package_m;  struct_set(s.obj,"package",  m);
    m.type=VT_BUILTIN; m.builtin=bi_settings_version_m;  struct_set(s.obj,"version",  m);
    m.type=VT_BUILTIN; m.builtin=bi_settings_icon_m;     struct_set(s.obj,"icon",     m);
    m.type=VT_BUILTIN; m.builtin=bi_settings_snapshot_m; struct_set(s.obj,"snapshot", m);
    return s;
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

    /* ── v0.2.1: new concepts ── */
    REG(env,"number",     number_fn);
    REG(env,"day",         day_fn);
    REG(env,"age",         age_fn);
    REG(env,"name",        name_fn);
    REG(env,"link",        link_fn);
    REG(env,"link_to",     link_to_fn);
    REG(env,"transmission",transmission_fn);
    REG(env,"admob",       admob_fn);
    REG(env,"firebase",    firebase_fn);
    REG(env,"pdf",         pdf_fn);
    REG(env,"container",   container_fn);
    REG(env,"music",        music_fn);
    REG(env,"loading",      loading_fn);
    REG(env,"bar",          bar_fn);
    REG(env,"edges",        edges_fn);
    REG(env,"impact",       impact_fn);

    /* Layout / position constants */
    env_set(env,"left",   vcg_str("left"),   1);
    env_set(env,"right",  vcg_str("right"),  1);
    env_set(env,"middle", vcg_str("middle"), 1);
    env_set(env,"above",  vcg_str("above"),  1);
    env_set(env,"below",  vcg_str("below"),  1);
    env_set(env,"topbar", vcg_str("topbar"), 1);
    env_set(env,"head",   vcg_str("head"),   1);

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
    REG(env,"kind",    kind_fn);
    REG(env,"copy",   copy_fn);

    env_set(env,"VCG_VERSION",  vcg_str("2.1.0"),      1);
    env_set(env,"VCG_DATE",     vcg_str("2026-06-21"),  1);
    env_set(env,"VCG_AUTHOR",   vcg_str("Syrian VCG"),  1);
    env_set(env,"VCG_EDITION",  vcg_str("Full Edition"),1);

    /* v2.1 String manipulation */
    REG(env,"split",       str_split);
    REG(env,"replace",     str_replace);
    REG(env,"trim",        str_trim);
    REG(env,"upper",       str_upper);
    REG(env,"lower",       str_lower);
    REG(env,"starts_with", str_starts_with);
    REG(env,"ends_with",   str_ends_with);

    /* v2.1 Array mutation/slicing */
    REG(env,"push",    arr_push_fn);
    REG(env,"pop",     arr_pop_fn);
    REG(env,"shift",   arr_shift_fn);
    REG(env,"unshift", arr_unshift_fn);
    REG(env,"reverse", arr_reverse_fn);
    REG(env,"sort",    arr_sort_fn);
    REG(env,"slice",   arr_str_slice);

    /* v2.1 Base64 */
    REG(env,"base64_encode", base64_encode_fn);
    REG(env,"base64_decode", base64_decode_fn);

    /* v2.1 Color / Style / Design */
    REG(env,"color",      color_fn);
    REG(env,"color_argv", color_argv_fn);
    REG(env,"style",      style_fn);
    REG(env,"design",     design_fn);
    REG(env,"store_zip",  store_zip_fn);

    /* v2.1 UI components */
    REG(env,"text",    text_fn);
    REG(env,"text_s",  text_s_fn);
    REG(env,"btn",     btn_fn);
    REG(env,"ui",      ui_fn);

    /* v2.1 App/Page Settings */
    REG(env,"settings_new", settings_new_fn);
}
