package com.syrianvcg.vcg;

import java.util.ArrayList;
import java.util.List;

/** Collects everything the program "shows" / "prints" / emits as html, in order. */
public final class OutputSink {

    public enum Kind { TEXT, HTML }

    public static final class Entry {
        public final Kind kind;
        public final String value;
        public Entry(Kind kind, String value) { this.kind = kind; this.value = value; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final boolean echoToStdout;

    public OutputSink(boolean echoToStdout) { this.echoToStdout = echoToStdout; }

    public void text(String s) {
        entries.add(new Entry(Kind.TEXT, s));
        if (echoToStdout) System.out.println(s);
    }

    public void html(String s) {
        entries.add(new Entry(Kind.HTML, s));
        if (echoToStdout) System.out.println("[html] " + s);
    }

    public List<Entry> entries() { return entries; }
}
