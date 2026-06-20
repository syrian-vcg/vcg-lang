package com.syrianvcg.editor;

/**
 * VcgAsset — يمثل ملف صورة أو فيديو أو وسائط مرفوع داخل المشروع
 * يتم تخزينه كـ Base64 Data URL لسهولة الاستخدام مباشرة داخل VCG (img/video)
 */
public class VcgAsset {
    private String id;
    private String projectId;
    private String name;
    private String mimeType;
    private String base64Data; // raw base64 (no data: prefix)
    private long size;
    private long addedAt;

    public VcgAsset(String id, String projectId, String name, String mimeType, String base64Data, long size) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.mimeType = mimeType;
        this.base64Data = base64Data;
        this.size = size;
        this.addedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getMimeType() { return mimeType; }
    public String getBase64Data() { return base64Data; }
    public long getSize() { return size; }
    public long getAddedAt() { return addedAt; }

    public String getDataUrl() {
        return "data:" + mimeType + ";base64," + base64Data;
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public String getSizeLabel() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }

    /** Reference snippet to insert into editor, e.g. img("asset:id") */
    public String getAssetRef() {
        return "asset:" + id;
    }
}
