package com.syrianvcg.vcg;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VcgInstance {
    public final VcgClass klass;
    public final Map<String, Object> fields = new LinkedHashMap<>();

    public VcgInstance(VcgClass klass) { this.klass = klass; }

    public Object get(String name) {
        if (fields.containsKey(name)) return fields.get(name);
        VcgFunction method = klass.findMethod(name);
        if (method != null) return method.bind(this);
        throw new Environment.VcgRuntimeError("لا توجد خاصية / No property '" + name + "' on instance of " + klass.name);
    }

    public void set(String name, Object value) { fields.put(name, value); }

    @Override
    public String toString() { return "<" + klass.name + " instance>"; }
}
