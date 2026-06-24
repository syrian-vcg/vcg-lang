package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.List;

/** Represents 'from..to' (exclusive of 'to'), per the VCG spec: 1..10 -> [1,2,...,9]. */
public final class VcgRange {
    public final int from, to;
    public VcgRange(int from, int to) { this.from = from; this.to = to; }

    public List<Object> toList() {
        List<Object> list = new ArrayList<>();
        if (from <= to) {
            for (int i = from; i < to; i++) list.add((double) i);
        } else {
            for (int i = from; i > to; i--) list.add((double) i);
        }
        return list;
    }
}
