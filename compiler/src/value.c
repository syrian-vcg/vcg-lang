#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "../include/vcg.h"

/* ================================================================
   VCG Runtime Values + Environment
   ================================================================ */

/* ── String value ── */
VCGVal vcg_str(const char *s) {
    VCGVal v; v.type = VT_STRING;
    v.sval = s ? strdup(s) : strdup("");
    return v;
}

/* ── Array ── */
VCGVal vcg_arr_new(void) {
    VCGVal v; v.type = VT_ARRAY;
    v.arr = calloc(1, sizeof(VCGArray));
    v.arr->refs = 1;
    return v;
}
void arr_push(VCGArray *a, VCGVal item) {
    if (a->len >= a->cap) {
        a->cap = a->cap ? a->cap * 2 : 8;
        a->items = realloc(a->items, a->cap * sizeof(VCGVal));
    }
    a->items[a->len++] = item;
}

/* ── Struct ── */
VCGVal vcg_struct_new(const char *type_name) {
    VCGVal v; v.type = VT_STRUCT;
    v.obj = calloc(1, sizeof(VCGStruct));
    v.obj->refs = 1;
    v.obj->type_name = type_name ? strdup(type_name) : strdup("object");
    return v;
}
void struct_set(VCGStruct *s, const char *key, VCGVal val) {
    for (int i = 0; i < s->len; i++) {
        if (strcmp(s->keys[i], key) == 0) { s->vals[i] = val; return; }
    }
    if (s->len >= s->cap) {
        s->cap = s->cap ? s->cap * 2 : 8;
        s->keys = realloc(s->keys, s->cap * sizeof(char*));
        s->vals = realloc(s->vals, s->cap * sizeof(VCGVal));
    }
    s->keys[s->len] = strdup(key);
    s->vals[s->len] = val;
    s->len++;
}
VCGVal *struct_get(VCGStruct *s, const char *key) {
    for (int i = 0; i < s->len; i++)
        if (strcmp(s->keys[i], key) == 0) return &s->vals[i];
    return NULL;
}

/* ── Truthiness ── */
int vcg_truthy(VCGVal v) {
    switch (v.type) {
        case VT_NIL:    return 0;
        case VT_BOOL:   return v.bval;
        case VT_INT:    return v.ival != 0;
        case VT_FLOAT:  return v.fval != 0.0;
        case VT_STRING: return v.sval && v.sval[0] != '\0';
        case VT_ARRAY:  return v.arr && v.arr->len > 0;
        default:        return 1;
    }
}

/* ── Equality ── */
int vcg_equal(VCGVal a, VCGVal b) {
    if (a.type == VT_NIL && b.type == VT_NIL) return 1;
    if (a.type == VT_BOOL && b.type == VT_BOOL) return a.bval == b.bval;
    if ((a.type==VT_INT||a.type==VT_FLOAT) && (b.type==VT_INT||b.type==VT_FLOAT)) {
        double av = a.type==VT_INT ? a.ival : a.fval;
        double bv = b.type==VT_INT ? b.ival : b.fval;
        return av == bv;
    }
    if (a.type==VT_STRING && b.type==VT_STRING)
        return strcmp(a.sval, b.sval) == 0;
    return 0;
}

/* ── to-string ── */
char *vcg_tostr(VCGVal v) {
    char buf[256];
    switch (v.type) {
        case VT_NIL:    return strdup("nil");
        case VT_BOOL:   return strdup(v.bval ? "true" : "false");
        case VT_INT:    snprintf(buf,sizeof(buf),"%d",v.ival); return strdup(buf);
        case VT_FLOAT: {
            snprintf(buf,sizeof(buf),"%.10g",v.fval);
            return strdup(buf);
        }
        case VT_STRING: return strdup(v.sval ? v.sval : "");
        case VT_ARRAY: {
            /* [a, b, c] */
            char *out = strdup("[");
            for (int i = 0; i < v.arr->len; i++) {
                if (i) { char *tmp=out; out=malloc(strlen(tmp)+3); strcpy(out,tmp); strcat(out,", "); free(tmp); }
                char *s = vcg_tostr(v.arr->items[i]);
                char *tmp=out; out=malloc(strlen(tmp)+strlen(s)+2); strcpy(out,tmp); strcat(out,s); free(tmp); free(s);
            }
            char *tmp=out; out=malloc(strlen(tmp)+2); strcpy(out,tmp); strcat(out,"]"); free(tmp);
            return out;
        }
        case VT_STRUCT: {
            snprintf(buf,sizeof(buf),"<%s>", v.obj->type_name ? v.obj->type_name : "struct");
            return strdup(buf);
        }
        case VT_FUNC:    return strdup("<func>");
        case VT_BUILTIN: return strdup("<builtin>");
        default:         return strdup("?");
    }
}

/* ── Free value internals ── */
void vcg_val_free(VCGVal *v) {
    if (!v) return;
    if (v->type == VT_STRING) { free(v->sval); v->sval = NULL; }
    /* arrays and structs are ref-counted elsewhere */
}

/* ================================================================
   ENVIRONMENT
   ================================================================ */
VCGEnv *env_new(VCGEnv *parent) {
    VCGEnv *e = calloc(1, sizeof(VCGEnv));
    e->parent = parent;
    e->refs   = 1;
    return e;
}

void env_set(VCGEnv *e, const char *name, VCGVal val, int is_const) {
    /* check existing in current scope */
    for (int i = 0; i < e->len; i++) {
        if (strcmp(e->bindings[i].name, name) == 0) {
            if (e->bindings[i].is_const) {
                fprintf(stderr, "[VCG] Cannot reassign const '%s'\n", name);
                return;
            }
            e->bindings[i].val = val;
            return;
        }
    }
    if (e->len >= e->cap) {
        e->cap = e->cap ? e->cap * 2 : 16;
        e->bindings = realloc(e->bindings, e->cap * sizeof(VCGBinding));
    }
    strncpy(e->bindings[e->len].name, name, VCG_MAX_IDENT-1);
    e->bindings[e->len].val      = val;
    e->bindings[e->len].is_const = is_const;
    e->len++;
}

int env_assign(VCGEnv *e, const char *name, VCGVal val) {
    for (VCGEnv *cur = e; cur; cur = cur->parent) {
        for (int i = 0; i < cur->len; i++) {
            if (strcmp(cur->bindings[i].name, name) == 0) {
                if (cur->bindings[i].is_const) {
                    fprintf(stderr, "[VCG] Cannot reassign const '%s'\n", name);
                    return 0;
                }
                cur->bindings[i].val = val;
                return 1;
            }
        }
    }
    return 0;
}

VCGVal *env_get(VCGEnv *e, const char *name) {
    for (VCGEnv *cur = e; cur; cur = cur->parent) {
        for (int i = 0; i < cur->len; i++) {
            if (strcmp(cur->bindings[i].name, name) == 0)
                return &cur->bindings[i].val;
        }
    }
    return NULL;
}

void env_free(VCGEnv *e) {
    if(!e) return;
    if(--e->refs > 0) return;
    if (!e) return;
    free(e->bindings);
    free(e);
}
