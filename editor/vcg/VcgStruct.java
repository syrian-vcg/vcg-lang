package com.syrianvcg.vcg;

import java.util.LinkedHashMap;
import java.util.Map;

/** A VCG struct/object value: an ordered string->value map, optionally tagged with a 'kind'
 *  (e.g. "Text", "Button", "Style", "Color") so kind(x) can distinguish UI elements. */
public final class VcgStruct {
    public final Map<String, Object> fields = new LinkedHashMap<>();
    public String kind = "struct";

    public VcgStruct() {}
    public VcgStruct(String kind) { this.kind = kind; }

    public Object get(String key) { return fields.get(key); }
    public void set(String key, Object value) { fields.put(key, value); }
    public boolean has(String key) { return fields.containsKey(key); }

    public VcgStruct copy() {
        VcgStruct s = new VcgStruct(this.kind);
        s.fields.putAll(this.fields);
        return s;
    }
}
