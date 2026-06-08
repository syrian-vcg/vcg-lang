#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Code Generator  →  HTML + JavaScript  (v1.0, 2026-06-06)
   ================================================================ */

static void g_indent(CodeGen *g){ for(int i=0;i<g->indent;i++) fprintf(g->out,"  "); }
static void g_line(CodeGen *g,const char *s){ g_indent(g); fprintf(g->out,"%s\n",s); }
static int  g_tmp(CodeGen *g){ return g->tmp++; }

static void gen_expr(CodeGen *g, Node *n);
static void gen_stmt(CodeGen *g, Node *n);
static void gen_block(CodeGen *g, Node *n);

/* ── escape JS string ── */
static void emit_jsstr(CodeGen *g, const char *s){
    fputc('"',g->out);
    for(;*s;s++){
        if(*s=='"')  fputs("\\\"",g->out);
        else if(*s=='\\') fputs("\\\\",g->out);
        else if(*s=='\n') fputs("\\n",g->out);
        else if(*s=='\r') fputs("\\r",g->out);
        else if(*s=='\t') fputs("\\t",g->out);
        else fputc(*s,g->out);
    }
    fputc('"',g->out);
}

/* ── expression ── */
static void gen_expr(CodeGen *g, Node *n){
    if(!n){ fprintf(g->out,"null"); return; }
    switch(n->type){
    case ND_INT:    fprintf(g->out,"%d",(int)n->num); break;
    case ND_FLOAT:  fprintf(g->out,"%.10g",n->num);   break;
    case ND_BOOL:   fprintf(g->out,"%s",n->bval?"true":"false"); break;
    case ND_NIL:    fprintf(g->out,"null"); break;
    case ND_STRING: emit_jsstr(g,n->str);  break;

    case ND_IDENT:
        fprintf(g->out,"_v('%s',_e)",n->name); break;

    case ND_ARRAY:
        fprintf(g->out,"[");
        for(int i=0;i<n->nchildren;i++){
            if(i) fprintf(g->out,",");
            gen_expr(g,n->children[i]);
        }
        fprintf(g->out,"]"); break;

    case ND_STRUCT_LIT:
        fprintf(g->out,"{");
        for(int i=0;i<n->nfinits;i++){
            if(i) fprintf(g->out,",");
            fprintf(g->out,"\"%s\":",n->finits[i].key);
            gen_expr(g,n->finits[i].val);
        }
        fprintf(g->out,"}"); break;

    case ND_BINOP:
        fprintf(g->out,"_bin(");
        emit_jsstr(g,n->op);
        fprintf(g->out,",");
        gen_expr(g,n->left);
        fprintf(g->out,",");
        gen_expr(g,n->right);
        fprintf(g->out,")");
        break;

    case ND_UNOP:
        if(strcmp(n->op,"not")==0||strcmp(n->op,"!")==0){
            fprintf(g->out,"!_truthy("); gen_expr(g,n->right); fprintf(g->out,")");
        } else if(strcmp(n->op,"-")==0){
            fprintf(g->out,"-("); gen_expr(g,n->right); fprintf(g->out,")");
        } else if(strcmp(n->op,"~")==0){
            fprintf(g->out,"~("); gen_expr(g,n->right); fprintf(g->out,")");
        } else {
            gen_expr(g,n->left?n->left:n->right);
        }
        break;

    case ND_TERNARY:
        fprintf(g->out,"(_truthy("); gen_expr(g,n->cond);
        fprintf(g->out,")?");       gen_expr(g,n->body);
        fprintf(g->out,":");        gen_expr(g,n->alt);
        fprintf(g->out,")");
        break;

    case ND_CALL:
        fprintf(g->out,"_call(");
        gen_expr(g,n->left);
        fprintf(g->out,",[");
        for(int i=0;i<n->nchildren;i++){if(i)fprintf(g->out,",");gen_expr(g,n->children[i]);}
        fprintf(g->out,"],_e)");
        break;

    case ND_METHOD_CALL:
        fprintf(g->out,"_method(");
        gen_expr(g,n->left);
        fprintf(g->out,",");
        emit_jsstr(g,n->name);
        fprintf(g->out,",[");
        for(int i=0;i<n->nchildren;i++){if(i)fprintf(g->out,",");gen_expr(g,n->children[i]);}
        fprintf(g->out,"],_e)");
        break;

    case ND_INDEX:
        fprintf(g->out,"_idx(");
        gen_expr(g,n->left);
        fprintf(g->out,",");
        gen_expr(g,n->right);
        fprintf(g->out,")");
        break;

    case ND_FIELD:
        fprintf(g->out,"_field(");
        gen_expr(g,n->left);
        fprintf(g->out,",");
        emit_jsstr(g,n->name);
        fprintf(g->out,")");
        break;

    case ND_TYPEOF:
        fprintf(g->out,"_typeof("); gen_expr(g,n->right); fprintf(g->out,")"); break;

    case ND_SIZEOF:
        fprintf(g->out,"_sizeof("); gen_expr(g,n->right); fprintf(g->out,")"); break;

    case ND_INPUT:
        if(n->left){ fprintf(g->out,"prompt("); gen_expr(g,n->left); fprintf(g->out,")||\"\""); }
        else fprintf(g->out,"prompt(\"input:\")||\"\"");
        break;

    case ND_LAMBDA:
        fprintf(g->out,"(function(");
        for(int i=0;i<n->nparams;i++){if(i)fprintf(g->out,",");fprintf(g->out,"_p_%s",n->params[i]);}
        fprintf(g->out,"){\n");
        g->indent++;
        /* bind params */
        fprintf(g->out,"  var _e2=_scope(_e);\n");
        for(int i=0;i<n->nparams;i++)
            fprintf(g->out,"  _def(_e2,'%s',_p_%s);\n",n->params[i],n->params[i]);
        gen_block(g,n->body);
        g->indent--;
        g_indent(g); fprintf(g->out,"})");
        break;

    /* v3.0 in expressions */
    case ND_SET:
        fprintf(g->out,"_vcg_set(");
        gen_expr(g,n->left);
        fprintf(g->out,",");
        gen_expr(g,n->right);
        fprintf(g->out,")");
        break;
    case ND_GET:
        fprintf(g->out,"_vcg_get(");
        gen_expr(g,n->left);
        fprintf(g->out,")");
        break;
    case ND_X:
        fprintf(g->out,"_vcg_x(");
        gen_expr(g,n->right);
        fprintf(g->out,")");
        break;
    case ND_CHANNEL_RECV:
        fprintf(g->out,"_vcg_recv(");
        gen_expr(g,n->right);
        fprintf(g->out,")");
        break;

    default:
        fprintf(g->out,"null"); break;
    }
}

/* ── LHS assignment target ── */
static void gen_lhs_set(CodeGen *g, Node *lhs, const char *val_expr){
    if(lhs->type==ND_IDENT){
        g_indent(g); fprintf(g->out,"_set(_e,'%s',%s);\n",lhs->name,val_expr);
    } else if(lhs->type==ND_INDEX){
        g_indent(g);
        fprintf(g->out,"_setidx(");
        gen_expr(g,lhs->left);
        fprintf(g->out,",");
        gen_expr(g,lhs->right);
        fprintf(g->out,",%s);\n",val_expr);
    } else if(lhs->type==ND_FIELD){
        g_indent(g);
        fprintf(g->out,"_setfield(");
        gen_expr(g,lhs->left);
        fprintf(g->out,",'%s',%s);\n",lhs->name,val_expr);
    }
}

/* ── block ── */
static void gen_block(CodeGen *g, Node *n){
    if(!n) return;
    if(n->type==ND_BLOCK){
        for(int i=0;i<n->nchildren;i++) gen_stmt(g,n->children[i]);
    } else gen_stmt(g,n);
}

/* ── statement ── */
static void gen_stmt(CodeGen *g, Node *n){
    if(!n) return;
    g_indent(g);
    switch(n->type){

    case ND_LET: case ND_CONST:
        fprintf(g->out,"_def(_e,'%s',",n->name);
        if(n->right) gen_expr(g,n->right); else fprintf(g->out,"null");
        fprintf(g->out,");\n"); break;

    case ND_ASSIGN: {
        /* temp var to avoid double-eval */
        int t=g_tmp(g);
        fprintf(g->out,"var _t%d=",t); gen_expr(g,n->right); fprintf(g->out,";\n");
        char buf[32]; snprintf(buf,32,"_t%d",t);
        gen_lhs_set(g,n->left,buf); break;
    }

    case ND_OP_ASSIGN: {
        /* x += expr  →  _set(_e,'x', _bin('+',_v('x'),expr)) */
        g_indent(g);
        fprintf(g->out,"_set(_e,'%s',_bin('%c',_v('%s',_e),",
            n->left->name, n->op[0], n->left->name);
        gen_expr(g,n->right);
        fprintf(g->out,"));\n"); break;
    }

    case ND_SHOW:
        fprintf(g->out,"_vcg_print([");
        for(int i=0;i<n->nchildren;i++){if(i)fprintf(g->out,",");gen_expr(g,n->children[i]);}
        fprintf(g->out,"]);\n"); break;

    case ND_HTML:
        fprintf(g->out,"_vcg_html(");
        gen_expr(g,n->right);
        fprintf(g->out,");\n"); break;

    case ND_IF:
        fprintf(g->out,"if(_truthy("); gen_expr(g,n->cond); fprintf(g->out,")){\n");
        g->indent++;
        fprintf(g->out,"  var _e=_scope(_e);\n");
        gen_block(g,n->body);
        g->indent--;
        g_indent(g); fprintf(g->out,"}");
        if(n->alt){
            fprintf(g->out," else {\n");
            g->indent++;
            fprintf(g->out,"  var _e=_scope(_e);\n");
            gen_block(g,n->alt);
            g->indent--;
            g_indent(g); fprintf(g->out,"}");
        }
        fprintf(g->out,"\n"); break;

    case ND_WHILE: {
        int t=g_tmp(g);
        fprintf(g->out,"var _wc%d=0;\n",t);
        g_indent(g); fprintf(g->out,"while(_truthy("); gen_expr(g,n->cond); fprintf(g->out,")&&_wc%d++<1e7){\n",t);
        g->indent++;
        g_line(g,"var _e=_scope(_e);");
        gen_block(g,n->body);
        g->indent--;
        g_indent(g); fprintf(g->out,"}\n"); break;
    }

    case ND_REPEAT: {
        int t=g_tmp(g);
        fprintf(g->out,"for(var _i%d=0,_n%d=_bin('|',",t,t);
        gen_expr(g,n->cond);
        fprintf(g->out,",0);_i%d<_n%d;_i%d++){\n",t,t,t);
        g->indent++;
        g_line(g,"var _e=_scope(_e);");
        gen_block(g,n->body);
        g->indent--;
        g_indent(g); fprintf(g->out,"}\n"); break;
    }

    case ND_FOR_IN: {
        int t=g_tmp(g);
        fprintf(g->out,"var _it%d=",t); gen_expr(g,n->init); fprintf(g->out,";\n");
        g_indent(g); fprintf(g->out,"var _arr%d=typeof _it%d==='string'?_it%d.split(''):(_it%d instanceof Array?_it%d:Object.values(_it%d));\n",t,t,t,t,t,t);
        g_indent(g); fprintf(g->out,"for(var _j%d=0;_j%d<_arr%d.length;_j%d++){\n",t,t,t,t);
        g->indent++;
        g_line(g,"var _e=_scope(_e);");
        g_indent(g); fprintf(g->out,"_def(_e,'%s',_arr%d[_j%d]);\n",n->name,t,t);
        gen_block(g,n->body);
        g->indent--;
        g_indent(g); fprintf(g->out,"}\n"); break;
    }

    case ND_FUNC:
        fprintf(g->out,"_def(_e,'%s',(function _f_%s(",n->name,n->name);
        for(int i=0;i<n->nparams;i++){if(i)fprintf(g->out,",");fprintf(g->out,"_p_%s",n->params[i]);}
        fprintf(g->out,"){\n");
        g->indent++;
        g_line(g,"var _e=_scope(_e);");
        for(int i=0;i<n->nparams;i++){
            g_indent(g);
            fprintf(g->out,"_def(_e,'%s',_p_%s);\n",n->params[i],n->params[i]);
        }
        gen_block(g,n->body);
        g_line(g,"return null;");
        g->indent--;
        g_indent(g); fprintf(g->out,"}));\n"); break;

    case ND_RETURN:
        fprintf(g->out,"return ");
        if(n->right) gen_expr(g,n->right); else fprintf(g->out,"null");
        fprintf(g->out,";\n"); break;

    case ND_BREAK:    fprintf(g->out,"break;\n");    break;
    case ND_CONTINUE: fprintf(g->out,"continue;\n"); break;

    case ND_ASSERT:
        fprintf(g->out,"if(!_truthy("); gen_expr(g,n->cond);
        fprintf(g->out,")) throw new Error(");
        if(n->right) gen_expr(g,n->right); else fprintf(g->out,"'Assertion failed'");
        fprintf(g->out,");\n"); break;

    case ND_THROW:
        fprintf(g->out,"throw new Error(String("); gen_expr(g,n->right); fprintf(g->out,"));\n"); break;

    case ND_TRY_CATCH:
        fprintf(g->out,"try{\n");
        g->indent++; gen_block(g,n->body); g->indent--;
        g_indent(g); fprintf(g->out,"}catch(_err){\n");
        g->indent++;
        g_line(g,"var _e=_scope(_e);");
        if(n->name){ g_indent(g); fprintf(g->out,"_def(_e,'%s',_err.message||String(_err));\n",n->name); }
        gen_block(g,n->alt);
        g->indent--;
        g_indent(g); fprintf(g->out,"}\n"); break;

    case ND_MATCH: {
        int t=g_tmp(g);
        fprintf(g->out,"var _mc%d=",t); gen_expr(g,n->cond); fprintf(g->out,";\n");
        for(int i=0;i<n->narms;i++){
            g_indent(g);
            fprintf(g->out,i?"else if":"if");
            fprintf(g->out,"(_vcg_eq(_mc%d,",t);
            gen_expr(g,n->arms_cond[i]);
            fprintf(g->out,")){\n");
            g->indent++; gen_stmt(g,n->arms_body[i]); g->indent--;
            g_indent(g); fprintf(g->out,"}");
        }
        fprintf(g->out,"\n"); break;
    }

    case ND_STRUCT:
        fprintf(g->out,"_def(_e,'%s',{__type__:'%s'",n->name,n->name);
        for(int i=0;i<n->nfields;i++) fprintf(g->out,",'%s':null",n->fields[i]);
        fprintf(g->out,"});\n"); break;

    case ND_BLOCK:
        fprintf(g->out,"{\n");
        g->indent++; gen_block(g,n); g->indent--;
        g_indent(g); fprintf(g->out,"}\n"); break;

    default: {
        /* expression statement */
        gen_expr(g,n); fprintf(g->out,";\n"); break;
    }
    }
}

/* ── HTML shell ── */
void codegen_init(CodeGen *g, FILE *out, const char *title, const char *theme){
    g->out=out; g->indent=0; g->tmp=0;
    strncpy(g->title,title?title:"VCG Program",255);
    strncpy(g->theme,theme?theme:"dark",63);
}

void codegen_emit(CodeGen *g, Node *root){
    FILE *o=g->out;

    fprintf(o,
"<!DOCTYPE html>\n<html lang=\"ar\" dir=\"rtl\">\n<head>\n"
"<meta charset=\"UTF-8\">\n"
"<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
"<title>%s — VCG</title>\n"
"<style>\n"
"@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;700;900&family=JetBrains+Mono:wght@400;700&display=swap');\n"
":root{--bg:#0a0f1a;--panel:#111827;--border:#1e3050;--accent:#00d4ff;--green:#00ff9d;--text:#e2e8f0;--muted:#4a5568;}\n"
"*{box-sizing:border-box;margin:0;padding:0}\n"
"body{background:var(--bg);color:var(--text);font-family:'Cairo',sans-serif;min-height:100vh;display:flex;flex-direction:column;align-items:center;padding:2rem 1rem}\n"
"header{width:100%%;max-width:820px;display:flex;align-items:center;gap:1rem;margin-bottom:2rem;padding-bottom:1rem;border-bottom:1px solid var(--border)}\n"
".logo{width:48px;height:48px;background:linear-gradient(135deg,#2d5a1b,#1a3a0a);border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:1.4rem;box-shadow:0 0 16px rgba(0,255,0,.2)}\n"
"h1{font-size:1.3rem;font-weight:900;color:var(--accent)}\n"
"#out{width:100%%;max-width:820px;background:var(--panel);border:1px solid var(--border);border-radius:14px;padding:1.8rem;min-height:180px;font-family:'JetBrains Mono',monospace;font-size:.9rem;line-height:2;box-shadow:0 4px 30px rgba(0,0,0,.4)}\n"
".ln{display:block;padding:.1rem .4rem;border-radius:4px}\n"
".ln:hover{background:rgba(0,212,255,.04)}\n"
".err{color:#f87171}\n"
"footer{margin-top:1.5rem;color:var(--muted);font-size:.75rem}\n"
"footer b{color:#00d4ff}\n"
"</style></head>\n<body>\n"
"<header><div class=\"logo\">✦</div><div><h1>%s</h1><p style=\"font-size:.8rem;color:var(--muted)\">Syrian Private Programming VCG v2.0</p></div></header>\n"
"<div id=\"out\"></div>\n"
"<footer>Compiled by <b>vcgc</b> — Syrian VCG Language</footer>\n"
"<script>\n"
"/* ── VCG Runtime ────────────────────── */\n"
"var _OUT=document.getElementById('out');\n"
"function _vcg_print(arr){\n"
"  var s=arr.map(function(v){return v===null?'nil':v===true?'true':v===false?'false':Array.isArray(v)?'['+v.map(String).join(', ')+']':typeof v==='object'&&v?JSON.stringify(v):String(v);}).join(' ');\n"
"  var el=document.createElement('span');el.className='ln';el.textContent=s;\n"
"  _OUT.appendChild(el);\n"
"}\n"
"function _vcg_html(s){_OUT.insertAdjacentHTML('beforeend',String(s));}\n"
"function _truthy(v){return v!==null&&v!==false&&v!==0&&v!==''&&v!==undefined;}\n"
"function _vcg_eq(a,b){if(a===b)return true;if(a===null&&b===null)return true;if(typeof a!=='object'&&typeof b!=='object')return a==b;return false;}\n"
"function _bin(op,a,b){\n"
"  if(op==='+'&&(typeof a==='string'||typeof b==='string'))return String(a)+String(b);\n"
"  if(op==='+'&&Array.isArray(a)&&Array.isArray(b))return a.concat(b);\n"
"  if(op==='..'){\n"
"    var r=[],f=Math.floor(a),t=Math.floor(b),s=f<=t?1:-1;\n"
"    for(var i=f;s>0?i<=t:i>=t;i+=s)r.push(i);return r;\n"
"  }\n"
"  switch(op){case '+':return a+b;case '-':return a-b;case '*':return a*b;\n"
"    case '/':if(b===0)throw new Error('Division by zero');return a/b;\n"
"    case '%%':if(b===0)throw new Error('Modulo by zero');return a%%b;\n"
"    case '**':return Math.pow(a,b);\n"
"    case '==':return _vcg_eq(a,b);case '!=':return !_vcg_eq(a,b);\n"
"    case '<':return a<b;case '>':return a>b;case '<=':return a<=b;case '>=':return a>=b;\n"
"    case 'and':return _truthy(a)&&_truthy(b);\n"
"    case 'or': return _truthy(a)||_truthy(b);\n"
"    case '|':return (a|0)|(b|0);case '&':return (a|0)&(b|0);\n"
"    case '<<':return (a|0)<<(b|0);case '>>':return (a|0)>>(b|0);\n"
"  }return null;\n"
"}\n"
"function _scope(p){return Object.create(p||null);}\n"
"function _def(e,k,v){Object.defineProperty(e,k,{value:v,writable:true,configurable:true,enumerable:true});return v;}\n"
"function _set(e,k,v){\n"
"  var c=e;\n"
"  while(c){if(Object.prototype.hasOwnProperty.call(c,k)){c[k]=v;return v;}c=Object.getPrototypeOf(c);}\n"
"  e[k]=v;return v;\n"
"}\n"
"function _v(k,e){\n"
"  var c=e;\n"
"  while(c){if(k in c)return c[k];c=Object.getPrototypeOf(c);}\n"
"  throw new Error('Undefined: '+k);\n"
"}\n"
"function _idx(o,i){if(o===null)return null;if(Array.isArray(o)){var j=i<0?o.length+i:i;return j>=0&&j<o.length?o[j]:null;}if(typeof o==='string'){var j=i<0?o.length+i:i;return j>=0&&j<o.length?o[j]:null;}if(typeof o==='object')return o[i]!==undefined?o[i]:null;return null;}\n"
"function _setidx(o,i,v){if(Array.isArray(o)){if(i<0)i=o.length+i;o[i]=v;}else if(typeof o==='object')o[i]=v;}\n"
"function _field(o,k){return o&&typeof o==='object'&&k in o?o[k]:null;}\n"
"function _setfield(o,k,v){if(o&&typeof o==='object')o[k]=v;}\n"
"function _typeof(v){if(v===null)return 'nil';if(v===true||v===false)return 'bool';if(Array.isArray(v))return 'array';if(typeof v==='object')return 'struct';return typeof v;}\n"
"function _sizeof(v){if(Array.isArray(v))return v.length;if(typeof v==='string')return v.length;if(v&&typeof v==='object')return Object.keys(v).length;return 1;}\n"
"function _call(f,args,e){if(typeof f==='function')return f.apply(null,args);throw new Error('Not callable');}\n"
"function _method(obj,name,args,e){\n"
"  if(Array.isArray(obj)){\n"
"    if(name==='push'){obj.push.apply(obj,args);return obj.length;}\n"
"    if(name==='pop')return obj.pop();\n"
"    if(name==='len'||name==='length')return obj.length;\n"
"    if(name==='join')return obj.join(args[0]!==undefined?args[0]:',');\n"
"    if(name==='contains')return obj.indexOf(args[0])>=0;\n"
"    if(name==='reverse'){obj.reverse();return obj;}\n"
"    if(name==='slice')return obj.slice(args[0]||0,args[1]);\n"
"    if(name==='sort'){obj.sort(function(a,b){return a-b;});return obj;}\n"
"  }\n"
"  if(typeof obj==='string'){\n"
"    if(name==='len'||name==='length')return obj.length;\n"
"    if(name==='upper')return obj.toUpperCase();\n"
"    if(name==='lower')return obj.toLowerCase();\n"
"    if(name==='trim')return obj.trim();\n"
"    if(name==='split')return obj.split(args[0]!==undefined?args[0]:' ');\n"
"    if(name==='contains')return obj.includes(String(args[0]));\n"
"    if(name==='startswith')return obj.startsWith(String(args[0]));\n"
"    if(name==='endswith')return obj.endsWith(String(args[0]));\n"
"    if(name==='replace')return obj.replaceAll(String(args[0]),String(args[1]));\n"
"    if(name==='tonum')return parseFloat(obj);\n"
"    if(name==='indexof')return obj.indexOf(String(args[0]));\n"
"  }\n"
"  if(obj&&typeof obj==='object'&&typeof obj[name]==='function')return obj[name].apply(obj,args);\n"
"  throw new Error('No method: '+name);\n"
"}\n"
"/* ── Stdlib ─────────────────────── */\n"
"var _e=_scope(null);\n"
"_def(_e,'abs',function(x){return Math.abs(x);});\n"
"_def(_e,'floor',function(x){return Math.floor(x);});\n"
"_def(_e,'ceil',function(x){return Math.ceil(x);});\n"
"_def(_e,'round',function(x){return Math.round(x);});\n"
"_def(_e,'sqrt',function(x){return Math.sqrt(x);});\n"
"_def(_e,'pow',function(b,e2){return Math.pow(b,e2);});\n"
"_def(_e,'log',function(x){return Math.log(x);});\n"
"_def(_e,'log2',function(x){return Math.log2(x);});\n"
"_def(_e,'log10',function(x){return Math.log10(x);});\n"
"_def(_e,'sin',function(x){return Math.sin(x);});\n"
"_def(_e,'cos',function(x){return Math.cos(x);});\n"
"_def(_e,'tan',function(x){return Math.tan(x);});\n"
"_def(_e,'min',function(){return Math.min.apply(null,arguments);});\n"
"_def(_e,'max',function(){return Math.max.apply(null,arguments);});\n"
"_def(_e,'clamp',function(v,lo,hi){return Math.max(lo,Math.min(hi,v));});\n"
"_def(_e,'rand',function(a,b){if(a===undefined)return Math.random();if(b===undefined)return Math.floor(Math.random()*a);return Math.floor(Math.random()*(b-a))+a;});\n"
"_def(_e,'range',function(a,b,s){var r=[],f=b===undefined?0:a,t=b===undefined?a:b,st=s||1;for(var i=f;i<t;i+=st)r.push(i);return r;});\n"
"_def(_e,'len',function(x){if(Array.isArray(x))return x.length;if(typeof x==='string')return x.length;if(x&&typeof x==='object')return Object.keys(x).length;return 0;});\n"
"_def(_e,'str',function(x){return x===null?'nil':x===true?'true':x===false?'false':Array.isArray(x)?'['+x.map(String).join(', ')+']':String(x);});\n"
"_def(_e,'int',function(x){return parseInt(x)||0;});\n"
"_def(_e,'float',function(x){return parseFloat(x)||0;});\n"
"_def(_e,'bool',function(x){return _truthy(x);});\n"
"_def(_e,'char',function(n){return String.fromCharCode(n);});\n"
"_def(_e,'ord',function(s){return s?s.charCodeAt(0):0;});\n"
"_def(_e,'keys',function(o){return o&&typeof o==='object'?Object.keys(o):[];});\n"
"_def(_e,'values',function(o){return o&&typeof o==='object'?Object.values(o):[];});\n"
"_def(_e,'join',function(a,s){return Array.isArray(a)?a.join(s||','):String(a);});\n"
"_def(_e,'format',function(){var s=arguments[0];for(var i=1;i<arguments.length;i++)s=s.replace('%%s',arguments[i]);return s;});\n"
"_def(_e,'typeof',_typeof);\n"
"_def(_e,'sizeof',_sizeof);\n"
"_def(_e,'isnil',function(x){return x===null||x===undefined;});\n"
"_def(_e,'isnum',function(x){return typeof x==='number';});\n"
"_def(_e,'isstr',function(x){return typeof x==='string';});\n"
"_def(_e,'isarr',function(x){return Array.isArray(x);});\n"
"_def(_e,'PI',Math.PI);\n"
"_def(_e,'E',Math.E);\n"
"_def(_e,'true',true);\n"
"_def(_e,'false',false);\n"
"_def(_e,'nil',null);\n"
"_def(_e,'show',function(){_vcg_print(Array.from(arguments));return null;});\n"
"_def(_e,'print',function(){_vcg_print(Array.from(arguments));return null;});\n"
"_def(_e,'input',function(p){return prompt(p||'input:')||'';});\n"
"/* ── v3.0 Reactive Runtime ───────── */\n"
"var _vcg_store={};\n"
"var _vcg_watchers={};\n"
"var _vcg_exports={};\n"
"var _vcg_writeonly={};\n"
"function _vcg_set(k,v){\n"
"  _vcg_store[k]=v;\n"
"  if(_vcg_watchers[k]) _vcg_watchers[k](v);\n"
"  return v;\n"
"}\n"
"function _vcg_get(k){ return _vcg_store[k]!==undefined?_vcg_store[k]:null; }\n"
"function _vcg_w(name,val,env){\n"
"  _def(env,name,val);\n"
"  _vcg_writeonly[name]=true;\n"
"  _vcg_print(['[w] '+name+' \u2190 '+String(val)]);\n"
"  return val;\n"
"}\n"
"function _vcg_x(v){ return typeof v==='function'?v():v; }\n"
"function _vcg_channel(name,init){\n"
"  var ch={_name:name,_buf:[]};\n"
"  if(init!==undefined) ch._buf.push(init);\n"
"  return ch;\n"
"}\n"
"function _vcg_send(ch,val){\n"
"  if(ch&&ch._buf) ch._buf.push(val);\n"
"  return val;\n"
"}\n"
"function _vcg_recv(ch){\n"
"  if(ch&&ch._buf&&ch._buf.length>0) return ch._buf.shift();\n"
"  return null;\n"
"}\n"
"function _vcg_pipe(val){\n"
"  var fns=Array.from(arguments).slice(1);\n"
"  return fns.reduce(function(v,f){return typeof f==='function'?f(v):v;},val);\n"
"}\n"
"_def(_e,'watch',function(k,fn){ _vcg_watchers[k]=fn; });"
"_def(_e,'store',function(){ return _vcg_store; });"
"_def(_e,'exports',function(){ return _vcg_exports; });"
"_def(_e,'send',_vcg_send);"
"_def(_e,'recv',_vcg_recv);"
"_def(_e,'pipe',_vcg_pipe);"
"_def(_e,'freeze',function(v){return Object.freeze(v)||v;});"
"_def(_e,'VCG_VERSION','3.0.0');"
"_def(_e,'VCG_DATE','2026-06-06');"
"/* ── User Program ────────────────── */\n"
"try{\n",
        g->title, g->title);

    /* emit user code */
    g->indent = 1;
    if(root){
        for(int i=0;i<root->nchildren;i++) gen_stmt(g,root->children[i]);
    }

    fprintf(o,
"}catch(e){\n"
"  var el=document.createElement('span');el.className='ln err';\n"
"  el.textContent='Runtime Error: '+e.message;_OUT.appendChild(el);\n"
"  console.error(e);\n"
"}\n"
"</script></body></html>\n");
}
