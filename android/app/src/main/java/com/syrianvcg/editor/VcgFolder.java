package com.syrianvcg.editor;

/**
 * VcgFolder — مجلد لتنظيم الملفات داخل المشروع.
 * المجلدات في VCG Editor مستوى واحد فقط (بدون تعشيش) حالياً، تماماً كما
 * تفعل أغلب محررات الموبايل البسيطة؛ كل ملف ينتمي لمجلد واحد أو للجذر.
 */
public class VcgFolder {
    private String id;
    private String projectId;
    private String name;
    private long createdAt;

    public VcgFolder(String id, String projectId, String name) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId()        { return id; }
    public String getProjectId() { return projectId; }
    public String getName()      { return name; }
    public long   getCreatedAt() { return createdAt; }
    public void   setName(String n) { this.name = n; }
    public void   setCreatedAt(long t) { this.createdAt = t; }
}
