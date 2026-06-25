package com.syrianvcg.vcg;

import java.util.HashMap;
import java.util.Map;

public final class Environment {
    public final Environment parent;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Boolean> consts = new HashMap<>();

    public Environment() { this.parent = null; }
    public Environment(Environment parent) { this.parent = parent; }

    public void define(String name, Object value, boolean isConst) {
        values.put(name, value);
        consts.put(name, isConst);
    }

    public void define(String name, Object value) { define(name, value, false); }

    public Object get(String name) {
        Environment env = this;
        while (env != null) {
            if (env.values.containsKey(name)) return env.values.get(name);
            env = env.parent;
        }
        throw new VcgRuntimeError("متغيّر غير معروف / Undefined variable: '" + name + "'");
    }

    public boolean has(String name) {
        Environment env = this;
        while (env != null) {
            if (env.values.containsKey(name)) return true;
            env = env.parent;
        }
        return false;
    }

    public void set(String name, Object value) {
        Environment env = this;
        while (env != null) {
            if (env.values.containsKey(name)) {
                if (Boolean.TRUE.equals(env.consts.get(name))) {
                    throw new VcgRuntimeError("لا يمكن تعديل ثابت / Cannot reassign const: '" + name + "'");
                }
                env.values.put(name, value);
                return;
            }
            env = env.parent;
        }
        // implicit global define if not found anywhere (forgiving like the original VCG impl)
        define(name, value, false);
    }

    public static final class VcgRuntimeError extends RuntimeException {
        public VcgRuntimeError(String msg) { super(msg); }
    }
}
