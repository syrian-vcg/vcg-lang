package com.syrianvcg.editor;

import java.util.Date;

public class VcgFile {
    private String name;
    private String content;
    private long lastModified;

    public VcgFile(String name, String content) {
        this.name = name;
        this.content = content;
        this.lastModified = System.currentTimeMillis();
    }

    public String getName()        { return name; }
    public String getContent()     { return content; }
    public long   getLastModified(){ return lastModified; }
    public void   setContent(String c){ this.content = c; this.lastModified = System.currentTimeMillis(); }

    public String getPreview() {
        if (content == null || content.isEmpty()) return "(فارغ)";
        String[] lines = content.split("\n");
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) return t.length() > 50 ? t.substring(0, 50) + "…" : t;
        }
        return lines[0].length() > 50 ? lines[0].substring(0, 50) + "…" : lines[0];
    }

    public int getLineCount() {
        if (content == null || content.isEmpty()) return 0;
        return content.split("\n", -1).length;
    }
}
