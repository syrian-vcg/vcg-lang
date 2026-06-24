package com.syrianvcg.editor;

import java.util.Date;

/**
 * VcgProject — يمثل مشروع يحتوي على عدة ملفات .vcg وأصول (صور/فيديو)
 */
public class VcgProject {
    private String id;
    private String name;
    private String description;
    private long createdAt;
    private long lastModified;
    private String colorTag; // hex color for the project card accent

    public VcgProject(String id, String name, String description, String colorTag) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.colorTag = colorTag == null ? "#4DC95A" : colorTag;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = this.createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; touch(); }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; touch(); }
    public long getCreatedAt() { return createdAt; }
    public long getLastModified() { return lastModified; }
    public String getColorTag() { return colorTag; }
    public void setColorTag(String c) { this.colorTag = c; touch(); }

    public void touch() { this.lastModified = System.currentTimeMillis(); }

    public void setCreatedAt(long t) { this.createdAt = t; }
    public void setLastModified(long t) { this.lastModified = t; }
}
