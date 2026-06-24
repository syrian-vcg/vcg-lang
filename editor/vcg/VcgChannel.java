package com.syrianvcg.vcg;

import java.util.ArrayDeque;
import java.util.Deque;

public final class VcgChannel {
    private final Deque<Object> queue = new ArrayDeque<>();

    public void send(Object v) { queue.addLast(v); }
    public Object recv() { return queue.isEmpty() ? null : queue.removeFirst(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}
