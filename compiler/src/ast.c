#include <stdlib.h>
#include <string.h>
#include "../include/vcg.h"

Node *nd_new(NodeType type, int line) {
    Node *n = calloc(1, sizeof(Node));
    n->type = type;
    n->line = line;
    return n;
}

void nd_add_child(Node *parent, Node *child) {
    if (parent->nchildren >= parent->cap) {
        parent->cap = parent->cap ? parent->cap * 2 : 8;
        parent->children = realloc(parent->children, parent->cap * sizeof(Node*));
    }
    parent->children[parent->nchildren++] = child;
}

void nd_free(Node *n) {
    if (!n) return;
    free(n->str);
    free(n->name);
    nd_free(n->left);
    nd_free(n->right);
    nd_free(n->cond);
    nd_free(n->body);
    nd_free(n->alt);
    nd_free(n->init);
    nd_free(n->update);
    for (int i = 0; i < n->nchildren; i++) nd_free(n->children[i]);
    free(n->children);
    for (int i = 0; i < n->nparams; i++) free(n->params[i]);
    free(n->params);
    if (n->param_defaults) {
        for (int i = 0; i < n->nparams; i++) nd_free(n->param_defaults[i]);
        free(n->param_defaults);
    }
    free(n->param_defaults_mask);
    for (int i = 0; i < n->nfields; i++) free(n->fields[i]);
    free(n->fields);
    if (n->finits) {
        for (int i = 0; i < n->nfinits; i++) { free(n->finits[i].key); nd_free(n->finits[i].val); }
        free(n->finits);
    }
    for (int i = 0; i < n->narms; i++) { nd_free(n->arms_cond[i]); nd_free(n->arms_body[i]); }
    free(n->arms_cond);
    free(n->arms_body);
    free(n);
}
