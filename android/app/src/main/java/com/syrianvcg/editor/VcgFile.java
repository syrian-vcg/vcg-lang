package com.syrianvcg.editor;

public class VcgFile {
    /** Root-level marker: a file with this folderId is not inside any folder. */
    public static final String ROOT = "";

    private String projectId;
    private String name;
    private String content;
    private long lastModified;
    private String folderId = ROOT;

    public VcgFile(String projectId, String name, String content) {
        this.projectId = projectId;
        this.name = name;
        this.content = content;
        this.lastModified = System.currentTimeMillis();
    }

    public VcgFile(String projectId, String name, String content, String folderId) {
        this(projectId, name, content);
        this.folderId = folderId == null ? ROOT : folderId;
    }

    // Backward-compatible constructor (defaults to "default" project)
    public VcgFile(String name, String content) {
        this("default", name, content);
    }

    public String getProjectId()   { return projectId; }
    public String getName()        { return name; }
    public String getContent()     { return content; }
    public long   getLastModified(){ return lastModified; }
    public String getFolderId()    { return folderId == null ? ROOT : folderId; }
    public boolean isInRoot()      { return getFolderId().isEmpty(); }
    public void   setContent(String c){ this.content = c; this.lastModified = System.currentTimeMillis(); }
    public void   setLastModified(long t) { this.lastModified = t; }
    public void   setFolderId(String f) { this.folderId = f == null ? ROOT : f; }

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
