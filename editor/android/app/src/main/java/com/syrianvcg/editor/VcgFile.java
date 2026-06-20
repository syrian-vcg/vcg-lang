package com.syrianvcg.editor;

public class VcgFile {
    private String projectId;
    private String name;
    private String content;
    private long lastModified;

    public VcgFile(String projectId, String name, String content) {
        this.projectId = projectId;
        this.name = name;
        this.content = content;
        this.lastModified = System.currentTimeMillis();
    }

    // Backward-compatible constructor (defaults to "default" project)
    public VcgFile(String name, String content) {
        this("default", name, content);
    }

    public String getProjectId()   { return projectId; }
    public String getName()        { return name; }
    public String getContent()     { return content; }
    public long   getLastModified(){ return lastModified; }
    public void   setContent(String c){ this.content = c; this.lastModified = System.currentTimeMillis(); }
    public void   setLastModified(long t) { this.lastModified = t; }

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

    /** Unique storage key combining project + filename */
    public String getStorageKey() {
        return projectId + "::" + name;
    }
}
