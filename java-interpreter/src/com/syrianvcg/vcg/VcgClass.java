package com.syrianvcg.vcg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VcgClass implements VcgCallable {
    public final String name;
    public final VcgClass superclass;
    public final Map<String, VcgFunction> methods = new HashMap<>();

    public VcgClass(String name, VcgClass superclass) {
        this.name = name;
        this.superclass = superclass;
    }

    public VcgFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        if (superclass != null) return superclass.findMethod(name);
        return null;
    }

    @Override
    public Object call(Interpreter interp, List<Object> args) {
        VcgInstance instance = new VcgInstance(this);
        VcgFunction init = findMethod("init");
        if (init != null) {
            init.bind(instance).call(interp, args);
        }
        return instance;
    }

    @Override
    public String name() { return name; }
}
