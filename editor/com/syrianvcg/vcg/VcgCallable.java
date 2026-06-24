package com.syrianvcg.vcg;

import java.util.List;

/** Anything that can be invoked with VCG call syntax: user functions, lambdas, native builtins, classes. */
public interface VcgCallable {
    Object call(Interpreter interp, List<Object> args);
    default String name() { return "<callable>"; }
}
