#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Interpreter  —  tree-walking evaluator  (v1.0, 2026-06-06)
   ================================================================ */

static VCGVal eval(Interpreter *I, Node *n, VCGEnv *env);

/* ── helpers ── */
#define ERR(I,line,fmt,...) do { \
    snprintf((I)->error_msg,512,(fmt),##__VA_ARGS__); \
    (I)->error_line=(line); (I)->signal=SIG_THROW; \
    return VCG_NIL; } while(0)

static double as_num(VCGVal v) {
    if (v.type==VT_INT)   return (double)v.ival;
    if (v.type==VT_FLOAT) return v.fval;
    if (v.type==VT_STRING) return atof(v.sval);
    return 0;
}
static VCGVal num_val(double d) {
    if (d == (long long)d && fabs(d) < 1e15) return VCG_INT((int)d);
    return VCG_FLOAT(d);
}

/* ── binary operations ── */
static VCGVal eval_binop(Interpreter *I, Node *n, VCGEnv *env) {
    VCGVal L = eval(I, n->left,  env); if(I->signal) return VCG_NIL;
    VCGVal R = eval(I, n->right, env); if(I->signal) return VCG_NIL;
    const char *op = n->op;

    /* string concat */
    if(strcmp(op,"+")==0 && (L.type==VT_STRING||R.type==VT_STRING)){
        char *ls=vcg_tostr(L), *rs=vcg_tostr(R);
        char *s=malloc(strlen(ls)+strlen(rs)+1); strcpy(s,ls); strcat(s,rs);
        VCGVal res=vcg_str(s); free(s); free(ls); free(rs); return res;
    }
    /* array concat */
    if(strcmp(op,"+")==0 && L.type==VT_ARRAY && R.type==VT_ARRAY){
        VCGVal res=vcg_arr_new();
        for(int i=0;i<L.arr->len;i++) res.arr->items[res.arr->len++]=L.arr->items[i];
        for(int i=0;i<R.arr->len;i++) res.arr->items[res.arr->len++]=R.arr->items[i];
        return res;
    }
    /* range operator */
    if(strcmp(op,"..")==0){
        VCGVal res=vcg_arr_new();
        int from=(int)as_num(L), to=(int)as_num(R);
        int step = from<=to ? 1 : -1;
        for(int i=from; step>0?i<=to:i>=to; i+=step){
            if(res.arr->len>=VCG_MAX_ARRAY) break;
            if(res.arr->len>=res.arr->cap){
                res.arr->cap=res.arr->cap?res.arr->cap*2:16;
                res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));
            }
            res.arr->items[res.arr->len++]=VCG_INT(i);
        }
        return res;
    }
    /* logical short-circuit */
    if(strcmp(op,"and")==0) return VCG_BOOL(vcg_truthy(L)&&vcg_truthy(R));
    if(strcmp(op,"or")==0)  return VCG_BOOL(vcg_truthy(L)||vcg_truthy(R));
    /* equality */
    if(strcmp(op,"==")==0) return VCG_BOOL(vcg_equal(L,R));
    if(strcmp(op,"!=")==0) return VCG_BOOL(!vcg_equal(L,R));
    /* comparison */
    double lv=as_num(L), rv=as_num(R);
    if(strcmp(op,"<" )==0) return VCG_BOOL(lv<rv);
    if(strcmp(op,">" )==0) return VCG_BOOL(lv>rv);
    if(strcmp(op,"<=")==0) return VCG_BOOL(lv<=rv);
    if(strcmp(op,">=")==0) return VCG_BOOL(lv>=rv);
    /* arithmetic */
    if(strcmp(op,"+")==0)  return num_val(lv+rv);
    if(strcmp(op,"-")==0)  return num_val(lv-rv);
    if(strcmp(op,"*")==0)  return num_val(lv*rv);
    if(strcmp(op,"/")==0){
        if(rv==0){ ERR(I,n->line,"Division by zero"); }
        return num_val(lv/rv);
    }
    if(strcmp(op,"%")==0){
        if(rv==0){ ERR(I,n->line,"Modulo by zero"); }
        return num_val(fmod(lv,rv));
    }
    if(strcmp(op,"**")==0) return num_val(pow(lv,rv));
    /* bitwise */
    if(strcmp(op,"|")==0)  return VCG_INT((int)lv|(int)rv);
    if(strcmp(op,"&")==0)  return VCG_INT((int)lv&(int)rv);
    if(strcmp(op,"<<")==0) return VCG_INT((int)lv<<(int)rv);
    if(strcmp(op,">>")==0) return VCG_INT((int)lv>>(int)rv);
    ERR(I,n->line,"Unknown operator '%s'", op);
}

/* ── call function ── */
static VCGVal call_func(Interpreter *I, VCGVal fn_val, VCGVal *args, int nargs, int line) {
    if(fn_val.type==VT_BUILTIN) return fn_val.builtin(args, nargs, line);
    if(fn_val.type!=VT_FUNC){ ERR(I,line,"Not callable"); }
    VCGFunc *fn = fn_val.fn;
    if(++I->call_depth > VCG_MAX_CALL_DEPTH){ ERR(I,line,"Stack overflow"); }
    VCGEnv *fenv = env_new(fn->closure ? fn->closure : I->globals);
    for(int i=0;i<fn->nparams;i++){
        if(fn->variadic && i==fn->nparams-1){
            VCGVal rest=vcg_arr_new();
            for(int j=i;j<nargs;j++){
                if(rest.arr->len>=rest.arr->cap){
                    rest.arr->cap=rest.arr->cap?rest.arr->cap*2:8;
                    rest.arr->items=realloc(rest.arr->items,rest.arr->cap*sizeof(VCGVal));
                }
                rest.arr->items[rest.arr->len++]=args[j];
            }
            env_set(fenv,fn->params[i],rest,0); break;
        }
        env_set(fenv,fn->params[i], i<nargs?args[i]:VCG_NIL, 0);
    }
    eval(I, fn->body, fenv);
    I->call_depth--;
    VCGVal ret = VCG_NIL;
    if(I->signal==SIG_RETURN){ ret=I->signal_val; I->signal=SIG_NONE; I->signal_val=VCG_NIL; }
    env_free(fenv);
    return ret;
}

/* ── main eval ── */
static VCGVal eval(Interpreter *I, Node *n, VCGEnv *env) {
    if(!n || I->signal) return VCG_NIL;

    switch(n->type){
    case ND_INT:    return VCG_INT((int)n->num);
    case ND_FLOAT:  return VCG_FLOAT(n->num);
    case ND_BOOL:   return VCG_BOOL(n->bval);
    case ND_NIL:    return VCG_NIL;
    case ND_STRING: return vcg_str(n->str);

    case ND_ARRAY: {
        VCGVal arr=vcg_arr_new();
        for(int i=0;i<n->nchildren;i++){
            VCGVal v=eval(I,n->children[i],env);
            if(I->signal) return VCG_NIL;
            if(arr.arr->len>=arr.arr->cap){
                arr.arr->cap=arr.arr->cap?arr.arr->cap*2:8;
                arr.arr->items=realloc(arr.arr->items,arr.arr->cap*sizeof(VCGVal));
            }
            arr.arr->items[arr.arr->len++]=v;
        }
        return arr;
    }

    case ND_STRUCT_LIT: {
        VCGVal obj=vcg_struct_new("object");
        for(int i=0;i<n->nfinits;i++){
            VCGVal v=eval(I,n->finits[i].val,env);
            if(I->signal) return VCG_NIL;
            struct_set(obj.obj, n->finits[i].key, v);
        }
        return obj;
    }

    case ND_IDENT: {
        VCGVal *v = env_get(env, n->name);
        if(!v){ ERR(I,n->line,"Undefined variable '%s'", n->name); }
        return *v;
    }

    case ND_INDEX: {
        VCGVal obj=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        VCGVal idx=eval(I,n->right,env); if(I->signal) return VCG_NIL;
        if(obj.type==VT_ARRAY){
            int i=(int)as_num(idx);
            if(i<0) i+=obj.arr->len;
            if(i<0||i>=obj.arr->len){ ERR(I,n->line,"Array index %d out of bounds",i); }
            return obj.arr->items[i];
        }
        if(obj.type==VT_STRING){
            char *s=obj.sval; int i=(int)as_num(idx); int len=(int)strlen(s);
            if(i<0) i+=len;
            if(i<0||i>=len){ ERR(I,n->line,"String index out of bounds"); }
            char buf[2]={s[i],'\0'}; return vcg_str(buf);
        }
        if(obj.type==VT_STRUCT){
            char *key=vcg_tostr(idx);
            VCGVal *v=struct_get(obj.obj,key); free(key);
            if(!v) return VCG_NIL;
            return *v;
        }
        ERR(I,n->line,"Cannot index this type");
    }

    case ND_FIELD: {
        VCGVal obj=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        if(obj.type==VT_STRUCT){
            VCGVal *v=struct_get(obj.obj,n->name);
            if(!v){ ERR(I,n->line,"No field '%s'",n->name); }
            return *v;
        }
        ERR(I,n->line,"Cannot access field '%s'",n->name);
    }

    case ND_UNOP: {
        const char *op=n->op;
        if(strcmp(op,"not")==0||strcmp(op,"!")==0){
            VCGVal v=eval(I,n->right,env); if(I->signal) return VCG_NIL;
            return VCG_BOOL(!vcg_truthy(v));
        }
        if(strcmp(op,"-")==0){
            VCGVal v=eval(I,n->right,env); if(I->signal) return VCG_NIL;
            return num_val(-as_num(v));
        }
        if(strcmp(op,"~")==0){
            VCGVal v=eval(I,n->right,env); if(I->signal) return VCG_NIL;
            return VCG_INT(~(int)as_num(v));
        }
        if(strcmp(op,"++pre")==0||strcmp(op,"--pre")==0){
            int delta=op[0]=='+'?1:-1;
            if(n->right->type==ND_IDENT){
                VCGVal *v=env_get(env,n->right->name);
                if(!v){ ERR(I,n->line,"Undefined '%s'",n->right->name); }
                *v=num_val(as_num(*v)+delta); return *v;
            }
        }
        if(strcmp(op,"++post")==0||strcmp(op,"--post")==0){
            int delta=op[0]=='+'?1:-1;
            if(n->left->type==ND_IDENT){
                VCGVal *v=env_get(env,n->left->name);
                if(!v){ ERR(I,n->line,"Undefined '%s'",n->left->name); }
                VCGVal old=*v; *v=num_val(as_num(*v)+delta); return old;
            }
        }
        return VCG_NIL;
    }

    case ND_BINOP: return eval_binop(I, n, env);

    case ND_TERNARY: {
        VCGVal c=eval(I,n->cond,env); if(I->signal) return VCG_NIL;
        return vcg_truthy(c)?eval(I,n->body,env):eval(I,n->alt,env);
    }

    case ND_LET: case ND_CONST: {
        VCGVal v = n->right ? eval(I,n->right,env) : VCG_NIL;
        if(I->signal) return VCG_NIL;
        env_set(env, n->name, v, n->type==ND_CONST);
        return VCG_NIL;
    }

    case ND_ASSIGN: {
        VCGVal v=eval(I,n->right,env); if(I->signal) return VCG_NIL;
        Node *lhs=n->left;
        if(lhs->type==ND_IDENT){
            if(!env_assign(env,lhs->name,v))
                env_set(env,lhs->name,v,0);
        } else if(lhs->type==ND_INDEX){
            VCGVal obj=eval(I,lhs->left,env); if(I->signal) return VCG_NIL;
            VCGVal idx=eval(I,lhs->right,env); if(I->signal) return VCG_NIL;
            if(obj.type==VT_ARRAY){
                int i=(int)as_num(idx);
                if(i<0) i+=obj.arr->len;
                if(i>=0&&i<obj.arr->len) obj.arr->items[i]=v;
            } else if(obj.type==VT_STRUCT){
                char *k=vcg_tostr(idx); struct_set(obj.obj,k,v); free(k);
            }
        } else if(lhs->type==ND_FIELD){
            VCGVal obj=eval(I,lhs->left,env); if(I->signal) return VCG_NIL;
            if(obj.type==VT_STRUCT) struct_set(obj.obj,lhs->name,v);
        }
        return v;
    }

    case ND_OP_ASSIGN: {
        VCGVal rhs=eval(I,n->right,env); if(I->signal) return VCG_NIL;
        Node *lhs=n->left;
        if(lhs->type==ND_IDENT){
            VCGVal *cur_v=env_get(env,lhs->name);
            if(!cur_v){ ERR(I,n->line,"Undefined '%s'",lhs->name); }
            double cv=as_num(*cur_v), rv=as_num(rhs);
            double res;
            if(strcmp(n->op,"+=")==0) res=cv+rv;
            else if(strcmp(n->op,"-=")==0) res=cv-rv;
            else if(strcmp(n->op,"*=")==0) res=cv*rv;
            else if(strcmp(n->op,"/=")==0){ if(rv==0){ERR(I,n->line,"Division by zero");} res=cv/rv; }
            else res=cv;
            *cur_v=num_val(res);
        }
        return VCG_NIL;
    }

    case ND_BLOCK: {
        VCGEnv *benv = env_new(env);
        for(int i=0;i<n->nchildren&&!I->signal;i++) eval(I,n->children[i],benv);
        env_free(benv);
        return VCG_NIL;
    }

    case ND_IF: {
        VCGVal c=eval(I,n->cond,env); if(I->signal) return VCG_NIL;
        if(vcg_truthy(c)) eval(I,n->body,env);
        else if(n->alt) eval(I,n->alt,env);
        return VCG_NIL;
    }

    case ND_WHILE: {
        while(!I->signal){
            VCGVal c=eval(I,n->cond,env); if(I->signal) break;
            if(!vcg_truthy(c)) break;
            eval(I,n->body,env);
            if(I->signal==SIG_BREAK){I->signal=SIG_NONE;break;}
            if(I->signal==SIG_CONTINUE){I->signal=SIG_NONE;continue;}
        }
        return VCG_NIL;
    }

    case ND_REPEAT: {
        VCGVal cnt=eval(I,n->cond,env); if(I->signal) return VCG_NIL;
        int times=(int)as_num(cnt);
        for(int i=0;i<times&&!I->signal;i++){
            eval(I,n->body,env);
            if(I->signal==SIG_BREAK){I->signal=SIG_NONE;break;}
            if(I->signal==SIG_CONTINUE){I->signal=SIG_NONE;continue;}
        }
        return VCG_NIL;
    }

    case ND_FOR_IN: {
        VCGVal iterable=eval(I,n->init,env); if(I->signal) return VCG_NIL;
        VCGEnv *lenv=env_new(env);
        if(iterable.type==VT_ARRAY){
            for(int i=0;i<iterable.arr->len&&!I->signal;i++){
                env_set(lenv,n->name,iterable.arr->items[i],0);
                eval(I,n->body,lenv);
                if(I->signal==SIG_BREAK){I->signal=SIG_NONE;break;}
                if(I->signal==SIG_CONTINUE){I->signal=SIG_NONE;continue;}
            }
        } else if(iterable.type==VT_STRING){
            char *s=iterable.sval;
            for(int i=0;s[i]&&!I->signal;i++){
                char buf[2]={s[i],'\0'};
                env_set(lenv,n->name,vcg_str(buf),0);
                eval(I,n->body,lenv);
                if(I->signal==SIG_BREAK){I->signal=SIG_NONE;break;}
                if(I->signal==SIG_CONTINUE){I->signal=SIG_NONE;continue;}
            }
        }
        env_free(lenv); return VCG_NIL;
    }

    case ND_BREAK:    I->signal=SIG_BREAK;    return VCG_NIL;
    case ND_CONTINUE: I->signal=SIG_CONTINUE; return VCG_NIL;

    case ND_RETURN: {
        I->signal_val = n->right ? eval(I,n->right,env) : VCG_NIL;
        I->signal=SIG_RETURN; return VCG_NIL;
    }

    case ND_THROW: {
        I->signal_val = eval(I,n->right,env);
        I->signal=SIG_THROW;
        char *s=vcg_tostr(I->signal_val);
        snprintf(I->error_msg,512,"%s",s); free(s);
        I->error_line=n->line; return VCG_NIL;
    }

    case ND_TRY_CATCH: {
        eval(I,n->body,env);
        if(I->signal==SIG_THROW){
            I->signal=SIG_NONE;
            VCGEnv *cenv=env_new(env);
            if(n->name) env_set(cenv,n->name,vcg_str(I->error_msg),0);
            eval(I,n->alt,cenv);
            env_free(cenv);
        }
        return VCG_NIL;
    }

    case ND_ASSERT: {
        VCGVal c=eval(I,n->cond,env); if(I->signal) return VCG_NIL;
        if(!vcg_truthy(c)){
            char msg[512]="Assertion failed";
            if(n->right){ VCGVal m=eval(I,n->right,env); char *s=vcg_tostr(m); snprintf(msg,512,"%s",s); free(s); }
            ERR(I,n->line,"%s",msg);
        }
        return VCG_NIL;
    }

    case ND_FUNC: {
        VCGVal fv; fv.type=VT_FUNC;
        fv.fn=calloc(1,sizeof(VCGFunc));
        fv.fn->body=n->body;
        fv.fn->params=n->params; fv.fn->nparams=n->nparams;
        fv.fn->variadic=n->variadic;
        /* snapshot the entire scope chain into a flat persistent env */
        {
            VCGEnv *snap=env_new(I->globals);
            for(VCGEnv *cur=env; cur && cur!=I->globals; cur=cur->parent)
                for(int _i=0;_i<cur->len;_i++)
                    env_set(snap,cur->bindings[_i].name,cur->bindings[_i].val,0);
            snap->refs=2; /* keep alive */
            fv.fn->closure=snap;
        }
        fv.fn->name=n->name;
        env_set(env,n->name,fv,0); return VCG_NIL;
    }

    case ND_LAMBDA: {
        VCGVal fv; fv.type=VT_FUNC;
        fv.fn=calloc(1,sizeof(VCGFunc));
        fv.fn->body=n->body;
        fv.fn->params=n->params; fv.fn->nparams=n->nparams;
        fv.fn->variadic=n->variadic;
        {
            VCGEnv *snap=env_new(I->globals);
            for(VCGEnv *cur=env; cur && cur!=I->globals; cur=cur->parent)
                for(int _i=0;_i<cur->len;_i++)
                    env_set(snap,cur->bindings[_i].name,cur->bindings[_i].val,0);
            snap->refs=2;
            fv.fn->closure=snap;
        }
        fv.fn->name="<lambda>";
        return fv;
    }

    case ND_STRUCT: {
        /* Register struct constructor */
        VCGVal ctor; ctor.type=VT_BUILTIN;
        /* We store struct info in a struct value */
        VCGVal proto=vcg_struct_new(n->name);
        for(int i=0;i<n->nfields;i++) struct_set(proto.obj,n->fields[i],VCG_NIL);
        env_set(env,n->name,proto,0);
        return VCG_NIL;
    }

    case ND_NEW: {
        VCGVal tmpl_v; VCGVal *tp=env_get(env,n->name);
        VCGVal obj=vcg_struct_new(n->name);
        if(tp&&tp->type==VT_STRUCT){
            /* copy default fields */
            for(int i=0;i<tp->obj->len;i++)
                struct_set(obj.obj,tp->obj->keys[i],tp->obj->vals[i]);
        }
        /* set fields by position */
        VCGVal *base=tp&&tp->type==VT_STRUCT?tp:NULL;
        for(int i=0;i<n->nchildren;i++){
            VCGVal v=eval(I,n->children[i],env); if(I->signal) return VCG_NIL;
            if(base&&i<base->obj->len) struct_set(obj.obj,base->obj->keys[i],v);
        }
        (void)tmpl_v;
        return obj;
    }

    case ND_CALL: {
        VCGVal fn=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        VCGVal args[VCG_MAX_PARAMS]; int nargs=0;
        for(int i=0;i<n->nchildren&&i<VCG_MAX_PARAMS;i++){
            args[nargs++]=eval(I,n->children[i],env);
            if(I->signal) return VCG_NIL;
        }
        return call_func(I,fn,args,nargs,n->line);
    }

    case ND_METHOD_CALL: {
        VCGVal obj=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        /* built-in methods */
        VCGVal args[VCG_MAX_PARAMS]; int nargs=0;
        for(int i=0;i<n->nchildren&&i<VCG_MAX_PARAMS;i++){
            args[nargs++]=eval(I,n->children[i],env);
            if(I->signal) return VCG_NIL;
        }
        const char *m=n->name;
        /* Array methods */
        if(obj.type==VT_ARRAY){
            if(strcmp(m,"push")==0){
                for(int i=0;i<nargs;i++){
                    if(obj.arr->len>=obj.arr->cap){
                        obj.arr->cap=obj.arr->cap?obj.arr->cap*2:8;
                        obj.arr->items=realloc(obj.arr->items,obj.arr->cap*sizeof(VCGVal));
                    }
                    obj.arr->items[obj.arr->len++]=args[i];
                }
                return VCG_INT(obj.arr->len);
            }
            if(strcmp(m,"pop")==0){
                if(obj.arr->len==0) return VCG_NIL;
                return obj.arr->items[--obj.arr->len];
            }
            if(strcmp(m,"len")==0||strcmp(m,"length")==0) return VCG_INT(obj.arr->len);
            if(strcmp(m,"join")==0){
                char *sep=(nargs>0&&args[0].type==VT_STRING)?args[0].sval:"";
                char *out=strdup(""); size_t outlen=0;
                for(int i=0;i<obj.arr->len;i++){
                    if(i){ size_t sl=strlen(sep); out=realloc(out,outlen+sl+1); memcpy(out+outlen,sep,sl+1); outlen+=sl; }
                    char *s=vcg_tostr(obj.arr->items[i]);
                    size_t sl=strlen(s); out=realloc(out,outlen+sl+1); memcpy(out+outlen,s,sl+1); outlen+=sl; free(s);
                }
                VCGVal res=vcg_str(out); free(out); return res;
            }
            if(strcmp(m,"contains")==0){
                for(int i=0;i<obj.arr->len;i++) if(vcg_equal(obj.arr->items[i],args[0])) return VCG_TRUE;
                return VCG_FALSE;
            }
            if(strcmp(m,"reverse")==0){
                for(int i=0,j=obj.arr->len-1;i<j;i++,j--){
                    VCGVal tmp=obj.arr->items[i]; obj.arr->items[i]=obj.arr->items[j]; obj.arr->items[j]=tmp;
                }
                return obj;
            }
            if(strcmp(m,"slice")==0){
                int from=nargs>0?(int)as_num(args[0]):0;
                int to=nargs>1?(int)as_num(args[1]):obj.arr->len;
                if(from<0) from=0; if(to>obj.arr->len) to=obj.arr->len;
                VCGVal res=vcg_arr_new();
                for(int i=from;i<to;i++){
                    if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
                    res.arr->items[res.arr->len++]=obj.arr->items[i];
                }
                return res;
            }
        }
        /* String methods */
        if(obj.type==VT_STRING){
            char *s=obj.sval;
            if(strcmp(m,"len")==0||strcmp(m,"length")==0) return VCG_INT((int)strlen(s));
            if(strcmp(m,"upper")==0){
                char *r=strdup(s); for(int i=0;r[i];i++) r[i]=toupper((unsigned char)r[i]);
                VCGVal res=vcg_str(r); free(r); return res;
            }
            if(strcmp(m,"lower")==0){
                char *r=strdup(s); for(int i=0;r[i];i++) r[i]=tolower((unsigned char)r[i]);
                VCGVal res=vcg_str(r); free(r); return res;
            }
            if(strcmp(m,"trim")==0){
                char *r=strdup(s); int i=0,j=(int)strlen(r)-1;
                while(r[i]==' '||r[i]=='\t'||r[i]=='\n') i++;
                while(j>i&&(r[j]==' '||r[j]=='\t'||r[j]=='\n')) j--;
                r[j+1]='\0'; VCGVal res=vcg_str(r+i); free(r); return res;
            }
            if(strcmp(m,"split")==0){
                char *sep=(nargs>0&&args[0].type==VT_STRING)?args[0].sval:" ";
                VCGVal res=vcg_arr_new();
                char *dup=strdup(s), *tok=strtok(dup,sep);
                while(tok){
                    if(res.arr->len>=res.arr->cap){res.arr->cap=res.arr->cap?res.arr->cap*2:8;res.arr->items=realloc(res.arr->items,res.arr->cap*sizeof(VCGVal));}
                    res.arr->items[res.arr->len++]=vcg_str(tok);
                    tok=strtok(NULL,sep);
                }
                free(dup); return res;
            }
            if(strcmp(m,"contains")==0){
                if(nargs>0&&args[0].type==VT_STRING) return VCG_BOOL(strstr(s,args[0].sval)!=NULL);
                return VCG_FALSE;
            }
            if(strcmp(m,"startswith")==0){
                if(nargs>0&&args[0].type==VT_STRING){
                    size_t pl=strlen(args[0].sval);
                    return VCG_BOOL(strncmp(s,args[0].sval,pl)==0);
                }
                return VCG_FALSE;
            }
            if(strcmp(m,"endswith")==0){
                if(nargs>0&&args[0].type==VT_STRING){
                    size_t sl2=strlen(s),pl=strlen(args[0].sval);
                    return VCG_BOOL(sl2>=pl&&strcmp(s+sl2-pl,args[0].sval)==0);
                }
                return VCG_FALSE;
            }
            if(strcmp(m,"replace")==0){
                if(nargs>=2&&args[0].type==VT_STRING&&args[1].type==VT_STRING){
                    char *from=args[0].sval, *to2=args[1].sval;
                    size_t fl=strlen(from),tl=strlen(to2);
                    char *r=strdup(s); char *p;
                    while((p=strstr(r,from))!=NULL){
                        size_t pos=p-r, rl=strlen(r);
                        r=realloc(r,rl-fl+tl+1);
                        memmove(r+pos+tl,r+pos+fl,rl-pos-fl+1);
                        memcpy(r+pos,to2,tl);
                    }
                    VCGVal res=vcg_str(r); free(r); return res;
                }
                return obj;
            }
            if(strcmp(m,"tonum")==0) return num_val(atof(s));
        }
        /* Struct methods */
        if(obj.type==VT_STRUCT){
            VCGVal *method=struct_get(obj.obj,m);
            if(method&&method->type==VT_FUNC){
                VCGVal all_args[VCG_MAX_PARAMS+1];
                all_args[0]=obj;
                for(int i=0;i<nargs;i++) all_args[i+1]=args[i];
                return call_func(I,*method,all_args,nargs+1,n->line);
            }
        }
        ERR(I,n->line,"No method '%s'", m);
    }

    case ND_SHOW: {
        if(I->html_out){
            fprintf(I->html_out,"  _vcg_print(");
            /* We'll collect values and print to the running HTML */
        }
        for(int i=0;i<n->nchildren;i++){
            VCGVal v=eval(I,n->children[i],env); if(I->signal) return VCG_NIL;
            char *s=vcg_tostr(v);
            if(i) printf(" ");
            printf("%s",s);
            free(s);
        }
        printf("\n"); return VCG_NIL;
    }

    case ND_INPUT: {
        if(n->left){ VCGVal p=eval(I,n->left,env); char *s=vcg_tostr(p); printf("%s",s); free(s); fflush(stdout); }
        char buf[VCG_MAX_STRING]; if(!fgets(buf,sizeof(buf),stdin)) return vcg_str("");
        buf[strcspn(buf,"\n")]='\0'; return vcg_str(buf);
    }

    case ND_HTML: {
        if(n->right){ VCGVal v=eval(I,n->right,env); char *s=vcg_tostr(v); printf("%s\n",s); free(s); }
        return VCG_NIL;
    }

    case ND_TYPEOF: {
        VCGVal v=eval(I,n->right,env); if(I->signal) return VCG_NIL;
        const char *names[]={"nil","bool","int","float","string","array","func","struct","builtin"};
        return vcg_str(names[v.type]);
    }
    case ND_SIZEOF: {
        VCGVal v=eval(I,n->right,env); if(I->signal) return VCG_NIL;
        if(v.type==VT_ARRAY)  return VCG_INT(v.arr->len);
        if(v.type==VT_STRING) return VCG_INT((int)strlen(v.sval));
        return VCG_INT(1);
    }

    case ND_MATCH: {
        VCGVal c=eval(I,n->cond,env); if(I->signal) return VCG_NIL;
        for(int i=0;i<n->narms;i++){
            VCGVal arm=eval(I,n->arms_cond[i],env); if(I->signal) return VCG_NIL;
            if(vcg_equal(c,arm)||vcg_truthy(arm)){
                eval(I,n->arms_body[i],env); return VCG_NIL;
            }
        }
        return VCG_NIL;
    }

    case ND_PROGRAM: {
        VCGEnv *benv=(n->type==ND_PROGRAM)?env:env_new(env);
        for(int i=0;i<n->nchildren&&!I->signal;i++) eval(I,n->children[i],benv);
        if(n->type!=ND_PROGRAM) env_free(benv);
        return VCG_NIL;
    }

    /* ═══════════════════════════════════════════════════════
       v3.0  NEW CONCEPTS
       ═══════════════════════════════════════════════════════ */

    /* ── public decl ──
       public let x = val  /  public func f() {}
       In interpreter: just execute inner decl (export via __exports__ object) */
    case ND_PUBLIC: {
        if(n->right) eval(I, n->right, env);
        if(!I->signal && n->right && n->right->name){
            /* register in __exports__ */
            VCGVal *exports = env_get(I->globals, "__exports__");
            if(exports && exports->type==VT_STRUCT){
                VCGVal *v = env_get(env, n->right->name);
                if(v) struct_set(exports->obj, n->right->name, *v);
            }
        }
        return VCG_NIL;
    }

    /* ── $set(key, val) ──
       Reactive property store: writes key→val into __store__ dict.
       Also triggers any registered watchers in __watchers__. */
    case ND_SET: {
        VCGVal key = eval(I, n->left,  env); if(I->signal) return VCG_NIL;
        VCGVal val = eval(I, n->right, env); if(I->signal) return VCG_NIL;
        char *k = vcg_tostr(key);

        /* write to global reactive store */
        VCGVal *store = env_get(I->globals, "__store__");
        if(store && store->type==VT_STRUCT)
            struct_set(store->obj, k, val);

        /* fire watchers: __watchers__[key](newVal) */
        VCGVal *watchers = env_get(I->globals, "__watchers__");
        if(watchers && watchers->type==VT_STRUCT){
            VCGVal *wfn = struct_get(watchers->obj, k);
            if(wfn && (wfn->type==VT_FUNC||wfn->type==VT_BUILTIN)){
                VCGVal args[1] = {val};
                call_func(I, *wfn, args, 1, n->line);
                if(I->signal==SIG_RETURN||I->signal==SIG_THROW) I->signal=SIG_NONE;
            }
        }
        free(k);
        return val;
    }

    /* ── $get(key) ──
       Reactive property read: reads from __store__ dict. */
    case ND_GET: {
        VCGVal key = eval(I, n->left, env); if(I->signal) return VCG_NIL;
        char *k = vcg_tostr(key);
        VCGVal *store = env_get(I->globals, "__store__");
        VCGVal result = VCG_NIL;
        if(store && store->type==VT_STRUCT){
            VCGVal *v = struct_get(store->obj, k);
            if(v) result = *v;
        }
        free(k); return result;
    }

    /* ── w name = expr ──
       Write-only binding: value is stored but any READ raises error.
       Uses __writeonly__ set to track names. */
    case ND_W: {
        VCGVal val = n->right ? eval(I, n->right, env) : VCG_NIL;
        if(I->signal) return VCG_NIL;
        /* Mark as write-only */
        VCGVal *wo = env_get(I->globals, "__writeonly__");
        if(wo && wo->type==VT_STRUCT)
            struct_set(wo->obj, n->name, VCG_TRUE);
        /* Still store so internal code can use it */
        env_set(env, n->name, val, 0);
        /* Print confirmation like a write log */
        char buf[512];
        char *vs = vcg_tostr(val);
        snprintf(buf, 512, "[w] %s ← %s", n->name, vs);
        free(vs);
        if(I->html_out) fprintf(I->html_out, "/* %s */\n", buf);
        else            printf("%s\n", buf);
        return val;
    }

    /* ── x expr ──
       Execute: immediately evaluate and run expression.
       If result is a function → call it with no args.
       Useful for IIFE pattern: x func() { show("ran!") } */
    case ND_X: {
        VCGVal val = eval(I, n->right, env); if(I->signal) return VCG_NIL;
        if(val.type==VT_FUNC || val.type==VT_BUILTIN){
            /* auto-invoke */
            return call_func(I, val, NULL, 0, n->line);
        }
        /* Otherwise: just return the value (expression execute) */
        return val;
    }

    /* ── c name = expr ──
       Channel variable: a named async queue (simulated as array).
       c chan creates __chan_<name>__ array.
       Send:  chan <- val   (push)
       Recv:  val = <-chan  (pop from front) */
    case ND_C: {
        if(!n->name) return VCG_NIL;
        /* Create channel: store directly as VCGArray under the variable name */
        VCGVal ch = vcg_arr_new();
        if(n->right){
            VCGVal init = eval(I, n->right, env); if(I->signal) return VCG_NIL;
            arr_push(ch.arr, init);
        }
        env_set(env, n->name, ch, 0);
        return ch;
    }

    /* ── channel send/recv (ND_CHANNEL_SEND / ND_CHANNEL_RECV) ── */
    case ND_CHANNEL_SEND: {
        /* name <- val  :  push val into channel array */
        VCGVal ch_name = eval(I, n->left,  env); if(I->signal) return VCG_NIL;
        VCGVal val     = eval(I, n->right, env); if(I->signal) return VCG_NIL;
        char *key = ch_name.type==VT_STRING ? ch_name.sval : NULL;
        if(key){
            VCGVal *ch = env_get(env, key);
            if(ch && ch->type==VT_ARRAY) arr_push(ch->arr, val);
        }
        return val;
    }
    case ND_CHANNEL_RECV: {
        /* <-name : pop from front of channel */
        VCGVal ch_name = eval(I, n->right, env); if(I->signal) return VCG_NIL;
        char *key = ch_name.type==VT_STRING ? ch_name.sval : NULL;
        if(key){
            VCGVal *ch = env_get(env, key);
            if(ch && ch->type==VT_ARRAY && ch->arr->len>0){
                VCGVal front = ch->arr->items[0];
                /* shift left */
                memmove(ch->arr->items, ch->arr->items+1,
                        (ch->arr->len-1)*sizeof(VCGVal));
                ch->arr->len--;
                return front;
            }
        }
        return VCG_NIL;
    }

    /* ═══════════════════════════════════════════════════════
       v1.0  UI/Media nodes — emit HTML in interpreter mode
       ═══════════════════════════════════════════════════════ */

    case ND_YOUTUBE: {
        VCGVal url_v = eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *url = vcg_tostr(url_v);
        char vid_id[128]; strncpy(vid_id, url, 127); vid_id[127]=0;
        char *vp = strstr(url, "v=");
        if(vp){ strncpy(vid_id, vp+2, 11); vid_id[11]=0; }
        char *vsl = strstr(url, "youtu.be/");
        if(vsl){ strncpy(vid_id, vsl+9, 11); vid_id[11]=0; }
        printf("<div style='position:relative;padding-bottom:56.25%%;height:0;overflow:hidden;border-radius:12px;margin:1rem 0'>"
               "<iframe style='position:absolute;top:0;left:0;width:100%%;height:100%%' "
               "src='https://www.youtube.com/embed/%s?rel=0' "
               "frameborder='0' allowfullscreen></iframe></div>\n", vid_id);
        free(url); return VCG_NIL;
    }

    case ND_FACEBOOK: {
        VCGVal u=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *url=vcg_tostr(u);
        char *txt=n->right?vcg_tostr(eval(I,n->right,env)):strdup("Facebook");
        printf("<a href='%s' target='_blank' style='background:#1877f2;color:white;"
               "padding:.5rem 1.2rem;border-radius:8px;text-decoration:none;"
               "font-weight:700;display:inline-block;margin:.25rem'>%s</a>\n", url, txt);
        free(url); free(txt); return VCG_NIL;
    }

    case ND_INSTAGRAM: {
        VCGVal u=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *handle=vcg_tostr(u);
        char *txt=n->right?vcg_tostr(eval(I,n->right,env)):strdup(handle);
        char href[512];
        if(strstr(handle,"http")) snprintf(href,512,"%s",handle);
        else snprintf(href,512,"https://instagram.com/%s",handle[0]=='@'?handle+1:handle);
        printf("<a href='%s' target='_blank' style='background:linear-gradient(45deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888);color:white;"
               "padding:.5rem 1.2rem;border-radius:8px;text-decoration:none;"
               "font-weight:700;display:inline-block;margin:.25rem'>%s</a>\n", href, txt);
        free(handle); free(txt); return VCG_NIL;
    }

    case ND_XSOCIAL: {
        VCGVal u=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *handle=vcg_tostr(u);
        char *txt=n->right?vcg_tostr(eval(I,n->right,env)):strdup(handle);
        char href[512];
        if(strstr(handle,"http")) snprintf(href,512,"%s",handle);
        else snprintf(href,512,"https://x.com/%s",handle[0]=='@'?handle+1:handle);
        printf("<a href='%s' target='_blank' style='background:#000;color:white;"
               "padding:.5rem 1.2rem;border-radius:8px;text-decoration:none;"
               "font-weight:700;display:inline-block;margin:.25rem'>X: %s</a>\n", href, txt);
        free(handle); free(txt); return VCG_NIL;
    }

    case ND_URL: {
        VCGVal u=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *href=vcg_tostr(u);
        char *txt=n->right?vcg_tostr(eval(I,n->right,env)):strdup(href);
        printf("<a href='%s' target='_blank' style='color:#4dc95a;"
               "text-decoration:underline;font-weight:600'>%s</a>\n", href, txt);
        free(href); free(txt); return VCG_NIL;
    }

    case ND_BTN: {
        VCGVal lbl=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *txt=vcg_tostr(lbl);
        char *act=n->right?vcg_tostr(eval(I,n->right,env)):strdup("");
        printf("<button onclick='%s' style='background:linear-gradient(135deg,#2d5a1b,#4a9020);"
               "color:white;border:none;padding:.6rem 1.5rem;border-radius:8px;"
               "font-weight:700;cursor:pointer;margin:.25rem'>%s</button>\n", act, txt);
        free(txt); free(act); return VCG_NIL;
    }

    case ND_KEY: {
        VCGVal k=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *txt=vcg_tostr(k);
        printf("<kbd style='background:#1a3a0a;color:#a8e080;border:1px solid #2d5a1b;"
               "border-radius:5px;padding:.2rem .6rem;font-family:monospace;"
               "font-size:.85rem;box-shadow:0 2px 0 #0a1a05'>%s</kbd>\n", txt);
        free(txt); return VCG_NIL;
    }

    case ND_VIDEO: {
        VCGVal u=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *src=vcg_tostr(u);
        char *w=n->right?vcg_tostr(eval(I,n->right,env)):strdup("100%");
        char *h3=n->nchildren>0?vcg_tostr(eval(I,n->children[0],env)):strdup("auto");
        printf("<video controls style='width:%s;height:%s;border-radius:10px;"
               "max-width:100%%;margin:.5rem 0'>"
               "<source src='%s'></video>\n", w, h3, src);
        free(src); free(w); free(h3); return VCG_NIL;
    }

    case ND_IMG: {
        VCGVal u=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        char *src=vcg_tostr(u);
        char *alt=n->right?vcg_tostr(eval(I,n->right,env)):strdup("");
        char *ww=n->nchildren>0?vcg_tostr(eval(I,n->children[0],env)):strdup("auto");
        printf("<img src='%s' alt='%s' style='width:%s;"
               "border-radius:8px;max-width:100%%;margin:.5rem 0;display:block' loading='lazy'>\n",
               src, alt, ww);
        free(src); free(alt); free(ww); return VCG_NIL;
    }

    case ND_H: {
        VCGVal lv=eval(I,n->left,env); if(I->signal) return VCG_NIL;
        int lvl=(int)as_num(lv); if(lvl<1)lvl=1; if(lvl>6)lvl=6;
        char *txt=n->right?vcg_tostr(eval(I,n->right,env)):strdup("");
        const char *sizes[]={"2.2rem","1.8rem","1.5rem","1.25rem","1.1rem","1rem"};
        printf("<h%d style='color:#a8e080;font-weight:900;font-size:%s;"
               "margin:.8rem 0 .4rem;border-bottom:2px solid #1e4020;"
               "padding-bottom:.3rem'>%s</h%d>\n", lvl, sizes[lvl-1], txt, lvl);
        free(txt); return VCG_NIL;
    }

    case ND_L: {
        printf("<ul style='list-style:none;padding:.5rem 0;margin:.5rem 0'>\n");
        for(int i=0;i<n->nchildren;i++){
            VCGVal v=eval(I,n->children[i],env); if(I->signal) return VCG_NIL;
            char *txt=vcg_tostr(v);
            printf("<li style='padding:.3rem .5rem;border-right:3px solid #2d5a1b;"
                   "margin:.2rem 0;color:#e8f5e0'>&#9658; %s</li>\n", txt);
            free(txt);
        }
        printf("</ul>\n"); return VCG_NIL;
    }

        case ND_IMPORT: return VCG_NIL; /* handled at top level */

    default: return VCG_NIL;
    }
}

/* ── public API ── */
void interp_init(Interpreter *I) {
    memset(I, 0, sizeof(*I));
    I->globals = env_new(NULL);
    stdlib_register(I->globals);
    /* v3.0 reactive infrastructure */
    env_set(I->globals, "__store__",    vcg_struct_new("store"),    0);
    env_set(I->globals, "__exports__",  vcg_struct_new("exports"),  0);
    env_set(I->globals, "__watchers__", vcg_struct_new("watchers"), 0);
    env_set(I->globals, "__writeonly__",vcg_struct_new("writeonly"),0);

    /* Built-in: watch(key, fn) — register reactive watcher */
    VCGVal watch_fn; watch_fn.type=VT_BUILTIN;
    watch_fn.builtin = NULL; /* filled below via stdlib */
    (void)watch_fn;
}

VCGVal interp_run(Interpreter *I, Node *program, FILE *html_out) {
    I->html_out = html_out;
    return eval(I, program, I->globals);
}

void interp_free(Interpreter *I) {
    env_free(I->globals);
}
