package com.syrianvcg.editor;

/**
 * VcgFile — يمثّل ملفاً أو مجلداً داخل مشروع.
 *
 * المسار (path) يخزّن الموقع الكامل داخل المشروع مثل:
 *   "main.vcg"           ← ملف في الجذر
 *   "lib/utils.vcg"      ← ملف داخل مجلد
 *   "src/ui/button.vcg"  ← ملف داخل مجلدين
 *   "lib/"               ← مجلد (ينتهي بـ /)
 *
 * اسم العرض (getName) يُرجع آخر جزء من المسار فقط.
 */
public class VcgFile {
    private String projectId;
    private String path;      // المسار الكامل مثل "lib/utils.vcg" أو "lib/"
    private String content;
    private long lastModified;
    private boolean isFolder;

    /** ملف عادي */
    public VcgFile(String projectId, String path, String content) {
        this.projectId    = projectId;
        this.path         = normalizePath(path);
        this.content      = content;
        this.lastModified = System.currentTimeMillis();
        this.isFolder     = false;
    }

    /** مجلد — المحتوى فارغ دائماً */
    public static VcgFile folder(String projectId, String folderPath) {
        VcgFile f = new VcgFile(projectId, ensureTrailingSlash(folderPath), "");
        f.isFolder = true;
        return f;
    }

    // ── Backward-compatible constructor ───────────────────────
    public VcgFile(String name, String content) {
        this("default", name, content);
    }

    // ── Getters ───────────────────────────────────────────────

    public String getProjectId()    { return projectId; }
    public String getPath()         { return path; }
    public String getContent()      { return content; }
    public long   getLastModified() { return lastModified; }
    public boolean isFolder()       { return isFolder; }

    /**
     * اسم العرض: آخر جزء من المسار.
     * "lib/utils.vcg" → "utils.vcg"
     * "lib/"          → "lib"
     * "main.vcg"      → "main.vcg"
     */
    public String getName() {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(slash + 1) : p;
    }

    /**
     * المجلد الأب لهذا العنصر.
     * "lib/utils.vcg" → "lib/"
     * "lib/"          → ""   (الجذر)
     * "main.vcg"      → ""   (الجذر)
     */
    public String getParentPath() {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(0, slash + 1) : "";
    }

    /** مفتاح التخزين الفريد */
    public String getStorageKey() {
        return projectId + "::" + path;
    }

    // ── Setters ───────────────────────────────────────────────

    public void setContent(String c) {
        this.content      = c;
        this.lastModified = System.currentTimeMillis();
    }

    public void setLastModified(long t) { this.lastModified = t; }

    // ── معاينة المحتوى ────────────────────────────────────────

    public String getPreview() {
        if (isFolder) return "مجلد";
        if (content == null || content.isEmpty()) return "(فارغ)";
        String[] lines = content.split("\n");
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#"))
                return t.length() > 50 ? t.substring(0, 50) + "…" : t;
        }
        return lines[0].length() > 50 ? lines[0].substring(0, 50) + "…" : lines[0];
    }

    public int getLineCount() {
        if (isFolder || content == null || content.isEmpty()) return 0;
        return content.split("\n", -1).length;
    }

    // ── Helpers ───────────────────────────────────────────────

    private static String normalizePath(String path) {
        if (path == null) return "";
        // أزل الشرطة المائلة من البداية
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static String ensureTrailingSlash(String path) {
        path = normalizePath(path);
        return path.endsWith("/") ? path : path + "/";
    }
}
