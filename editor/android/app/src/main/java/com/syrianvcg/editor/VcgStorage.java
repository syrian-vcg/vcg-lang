package com.syrianvcg.editor;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * VcgStorage — تخزين المشاريع، الملفات (مع دعم المجلدات)، والأصول محلياً.
 *
 * هيكل التخزين للملفات:
 *   key: "projectId::path"   value: محتوى الملف (أو "__FOLDER__" للمجلدات)
 *   index: "index::projectId" = "path1|||path2|||..."
 */
public class VcgStorage {

    private static final String PREFS_PROJECTS = "vcg_projects";
    private static final String PREFS_FILES    = "vcg_files_v2";
    private static final String PREFS_ASSETS   = "vcg_assets_v2";
    private static final String KEY_PROJECTS   = "project_list";

    /** قيمة خاصة تُخزَّن كمحتوى المجلد لتمييزه عن الملفات */
    private static final String FOLDER_MARKER  = "__FOLDER__";

    private final SharedPreferences projectsPrefs;
    private final SharedPreferences filesPrefs;
    private final SharedPreferences assetsPrefs;
    private final Context ctx;

    public VcgStorage(Context ctx) {
        this.ctx = ctx;
        projectsPrefs = ctx.getSharedPreferences(PREFS_PROJECTS, Context.MODE_PRIVATE);
        filesPrefs    = ctx.getSharedPreferences(PREFS_FILES,    Context.MODE_PRIVATE);
        assetsPrefs   = ctx.getSharedPreferences(PREFS_ASSETS,   Context.MODE_PRIVATE);
    }

    private static String sanitizeForIndex(String raw) {
        if (raw == null) return "";
        return raw.replace("|||", "___");
    }

    // ═══════════════════ PROJECTS ═══════════════════

    public void saveProject(VcgProject p) {
        try {
            JSONObject o = new JSONObject();
            o.put("id",           p.getId());
            o.put("name",         p.getName());
            o.put("description",  p.getDescription());
            o.put("colorTag",     p.getColorTag());
            o.put("createdAt",    p.getCreatedAt());
            o.put("lastModified", p.getLastModified());
            projectsPrefs.edit().putString(p.getId(), o.toString()).apply();
            addToProjectIndex(p.getId());
        } catch (JSONException ignored) {}
    }

    public VcgProject getProject(String id) {
        String raw = projectsPrefs.getString(id, null);
        if (raw == null) return null;
        return parseProject(raw);
    }

    public List<VcgProject> getAllProjects() {
        List<VcgProject> result = new ArrayList<>();
        String index = projectsPrefs.getString(KEY_PROJECTS, "");
        if (!index.isEmpty()) {
            for (String id : index.split("\\|\\|\\|")) {
                if (id.isEmpty()) continue;
                String raw = projectsPrefs.getString(id, null);
                if (raw != null) {
                    VcgProject p = parseProject(raw);
                    if (p != null) result.add(p);
                }
            }
        }
        Collections.sort(result, (a, b) ->
            Long.compare(b.getLastModified(), a.getLastModified()));
        return result;
    }

    public void deleteProject(String id) {
        for (VcgFile f : getAllInProject(id)) deleteEntry(id, f.getPath());
        for (VcgAsset a : getAssetsInProject(id)) deleteAsset(a.getId());
        projectsPrefs.edit().remove(id).apply();
        removeFromProjectIndex(id);
    }

    public boolean hasAnyProject() {
        return !projectsPrefs.getString(KEY_PROJECTS, "").isEmpty();
    }

    private VcgProject parseProject(String raw) {
        try {
            JSONObject o = new JSONObject(raw);
            VcgProject p = new VcgProject(
                o.getString("id"), o.getString("name"),
                o.optString("description", ""), o.optString("colorTag", "#4DC95A"));
            p.setCreatedAt(o.optLong("createdAt", System.currentTimeMillis()));
            p.setLastModified(o.optLong("lastModified", System.currentTimeMillis()));
            return p;
        } catch (JSONException e) { return null; }
    }

    private void addToProjectIndex(String id) {
        String index = projectsPrefs.getString(KEY_PROJECTS, "");
        if (!index.isEmpty()) {
            for (String p : index.split("\\|\\|\\|")) if (p.equals(id)) return;
        }
        projectsPrefs.edit().putString(KEY_PROJECTS,
            index.isEmpty() ? id : index + "|||" + id).apply();
    }

    private void removeFromProjectIndex(String id) {
        String index = projectsPrefs.getString(KEY_PROJECTS, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String p : index.split("\\|\\|\\|")) {
            if (!p.equals(id)) { if (sb.length() > 0) sb.append("|||"); sb.append(p); }
        }
        projectsPrefs.edit().putString(KEY_PROJECTS, sb.toString()).apply();
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ═══════════════════ FILES & FOLDERS ═══════════════════

    /**
     * حفظ ملف أو إنشاء مجلد.
     * المجلدات تُخزَّن بنفس الآلية لكن بمحتوى FOLDER_MARKER.
     */
    public void saveFile(VcgFile file) {
        String safePath = sanitizeForIndex(file.getPath());
        String value    = file.isFolder() ? FOLDER_MARKER : (file.getContent() != null ? file.getContent() : "");
        filesPrefs.edit().putString(file.getProjectId() + "::" + safePath, value).apply();
        addToFileIndex(file.getProjectId(), safePath);
        // تأكد أن المجلدات الأب موجودة تلقائياً
        ensureParentFolders(file.getProjectId(), safePath);
        VcgProject p = getProject(file.getProjectId());
        if (p != null) { p.touch(); saveProject(p); }
    }

    /** إنشاء مجلد */
    public void createFolder(String projectId, String folderPath) {
        saveFile(VcgFile.folder(projectId, folderPath));
    }

    /** جلب ملف أو مجلد بمساره */
    public VcgFile getFile(String projectId, String path) {
        String safePath = sanitizeForIndex(path);
        String value    = filesPrefs.getString(projectId + "::" + safePath, null);
        if (value == null) return null;
        if (FOLDER_MARKER.equals(value)) return VcgFile.folder(projectId, safePath);
        return new VcgFile(projectId, safePath, value);
    }

    /**
     * جلب محتويات مجلد معيّن فقط (غير متكرر — المستوى المباشر).
     * @param projectId  معرّف المشروع
     * @param folderPath المسار مثل "" للجذر، أو "lib/" لمجلد lib
     */
    public List<VcgFile> getFilesInFolder(String projectId, String folderPath) {
        // نُوحِّد صيغة المسار
        if (folderPath == null) folderPath = "";
        if (!folderPath.isEmpty() && !folderPath.endsWith("/")) folderPath += "/";

        List<VcgFile> result = new ArrayList<>();
        String index = filesPrefs.getString("index::" + projectId, "");
        if (index.isEmpty()) return result;

        for (String path : index.split("\\|\\|\\|")) {
            if (path.isEmpty()) continue;
            // هذا المسار يجب أن يكون مباشراً داخل folderPath
            if (!isDirectChild(path, folderPath)) continue;
            String value = filesPrefs.getString(projectId + "::" + path, null);
            if (value == null) continue;
            if (FOLDER_MARKER.equals(value)) {
                result.add(VcgFile.folder(projectId, path));
            } else {
                result.add(new VcgFile(projectId, path, value));
            }
        }

        // رتّب: المجلدات أولاً ثم الملفات أبجدياً
        Collections.sort(result, (a, b) -> {
            if (a.isFolder() && !b.isFolder()) return -1;
            if (!a.isFolder() && b.isFolder())  return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return result;
    }

    /** جلب الملفات في الجذر (للتوافق مع الكود القديم) */
    public List<VcgFile> getFilesInProject(String projectId) {
        return getFilesInFolder(projectId, "");
    }

    /** جلب كل العناصر في المشروع بلا استثناء (للتصدير والحذف) */
    public List<VcgFile> getAllInProject(String projectId) {
        List<VcgFile> result = new ArrayList<>();
        String index = filesPrefs.getString("index::" + projectId, "");
        if (index.isEmpty()) return result;
        for (String path : index.split("\\|\\|\\|")) {
            if (path.isEmpty()) continue;
            String value = filesPrefs.getString(projectId + "::" + path, null);
            if (value == null) continue;
            if (FOLDER_MARKER.equals(value)) result.add(VcgFile.folder(projectId, path));
            else result.add(new VcgFile(projectId, path, value));
        }
        return result;
    }

    public void deleteFile(String projectId, String path) {
        deleteEntry(projectId, path);
    }

    /**
     * حذف مجلد وكل محتوياته بشكل متكرر.
     */
    public void deleteFolder(String projectId, String folderPath) {
        if (!folderPath.endsWith("/")) folderPath += "/";
        String index = filesPrefs.getString("index::" + projectId, "");
        if (index.isEmpty()) return;
        List<String> toDelete = new ArrayList<>();
        for (String path : index.split("\\|\\|\\|")) {
            if (path.isEmpty()) continue;
            if (path.equals(folderPath) || path.startsWith(folderPath)) {
                toDelete.add(path);
            }
        }
        for (String path : toDelete) deleteEntry(projectId, path);
    }

    private void deleteEntry(String projectId, String path) {
        String safePath = sanitizeForIndex(path);
        filesPrefs.edit().remove(projectId + "::" + safePath).apply();
        removeFromFileIndex(projectId, safePath);
    }

    public boolean fileExists(String projectId, String path) {
        return filesPrefs.contains(projectId + "::" + sanitizeForIndex(path));
    }

    public void renameFile(String projectId, String oldPath, String newPath) {
        VcgFile f = getFile(projectId, oldPath);
        if (f == null) return;
        deleteEntry(projectId, oldPath);
        saveFile(new VcgFile(projectId, newPath, f.getContent()));
    }

    /**
     * إعادة تسمية مجلد — تُغيّر مسار كل محتوياته أيضاً.
     */
    public void renameFolder(String projectId, String oldPath, String newName) {
        if (!oldPath.endsWith("/")) oldPath += "/";
        String parentPath = getParentPath(oldPath);
        String newPath = parentPath + newName + "/";

        List<VcgFile> all = getAllInProject(projectId);
        for (VcgFile f : all) {
            if (f.getPath().equals(oldPath) || f.getPath().startsWith(oldPath)) {
                String updatedPath = newPath + f.getPath().substring(oldPath.length());
                String value = f.isFolder() ? FOLDER_MARKER : f.getContent();
                deleteEntry(projectId, f.getPath());
                filesPrefs.edit().putString(projectId + "::" + updatedPath, value).apply();
                addToFileIndex(projectId, updatedPath);
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * هل path طفل مباشر لـ parentFolder؟
     * "lib/utils.vcg"  في "" → لا (هذا في lib)
     * "main.vcg"       في "" → نعم
     * "lib/"           في "" → نعم
     * "lib/utils.vcg"  في "lib/" → نعم
     * "lib/a/b.vcg"    في "lib/" → لا (مستوى أعمق)
     */
    private boolean isDirectChild(String path, String parentFolder) {
        if (!path.startsWith(parentFolder)) return false;
        String relative = path.substring(parentFolder.length());
        if (relative.isEmpty()) return false;
        // المسار المباشر: لا يحتوي شرطة مائلة في المنتصف
        // مجلد مباشر: "lib/" → relative = "lib/" → شرطة واحدة في النهاية فقط
        if (relative.endsWith("/")) {
            // مجلد: يجب ألا يحتوي شرطة أخرى قبل النهاية
            return relative.indexOf('/') == relative.length() - 1;
        } else {
            // ملف: لا يحتوي أي شرطة
            return !relative.contains("/");
        }
    }

    /**
     * تأكد أن كل المجلدات الوسيطة في المسار مُنشأة.
     * "lib/utils/main.vcg" → ينشئ "lib/" و "lib/utils/" تلقائياً
     */
    private void ensureParentFolders(String projectId, String path) {
        String[] parts = path.split("/");
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            current.append(parts[i]).append("/");
            String folderPath = current.toString();
            if (!fileExists(projectId, folderPath)) {
                filesPrefs.edit().putString(projectId + "::" + folderPath, FOLDER_MARKER).apply();
                addToFileIndex(projectId, folderPath);
            }
        }
    }

    private String getParentPath(String path) {
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(0, slash + 1) : "";
    }

    private void addToFileIndex(String projectId, String path) {
        String safePath = sanitizeForIndex(path);
        String key      = "index::" + projectId;
        String index    = filesPrefs.getString(key, "");
        if (!index.isEmpty()) {
            for (String n : index.split("\\|\\|\\|")) if (n.equals(safePath)) return;
        }
        filesPrefs.edit().putString(key,
            index.isEmpty() ? safePath : index + "|||" + safePath).apply();
    }

    private void removeFromFileIndex(String projectId, String path) {
        String safePath = sanitizeForIndex(path);
        String key      = "index::" + projectId;
        String index    = filesPrefs.getString(key, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String n : index.split("\\|\\|\\|")) {
            if (!n.equals(safePath)) { if (sb.length() > 0) sb.append("|||"); sb.append(n); }
        }
        filesPrefs.edit().putString(key, sb.toString()).apply();
    }

    // ═══════════════════ ASSETS ═══════════════════

    public VcgAsset saveAsset(String projectId, String name, String mimeType, String base64Data, long size) {
        String id = newId();
        VcgAsset asset = new VcgAsset(id, projectId, name, mimeType, base64Data, size);
        try {
            JSONObject o = new JSONObject();
            o.put("id",        asset.getId());
            o.put("projectId", asset.getProjectId());
            o.put("name",      asset.getName());
            o.put("mimeType",  asset.getMimeType());
            o.put("data",      asset.getBase64Data());
            o.put("size",      asset.getSize());
            o.put("addedAt",   asset.getAddedAt());
            assetsPrefs.edit().putString(id, o.toString()).apply();
            addAssetToIndex(projectId, id);
        } catch (JSONException ignored) {}
        return asset;
    }

    public VcgAsset getAsset(String id) {
        String raw = assetsPrefs.getString(id, null);
        return raw == null ? null : parseAsset(raw);
    }

    public List<VcgAsset> getAssetsInProject(String projectId) {
        List<VcgAsset> result = new ArrayList<>();
        String index = assetsPrefs.getString("index::" + projectId, "");
        if (index.isEmpty()) return result;
        for (String id : index.split("\\|\\|\\|")) {
            if (id.isEmpty()) continue;
            VcgAsset a = getAsset(id);
            if (a != null) result.add(a);
        }
        return result;
    }

    public void deleteAsset(String id) {
        VcgAsset a = getAsset(id);
        assetsPrefs.edit().remove(id).apply();
        if (a != null) removeAssetFromIndex(a.getProjectId(), id);
    }

    private VcgAsset parseAsset(String raw) {
        try {
            JSONObject o = new JSONObject(raw);
            return new VcgAsset(
                o.getString("id"), o.getString("projectId"), o.getString("name"),
                o.getString("mimeType"), o.getString("data"), o.optLong("size", 0));
        } catch (JSONException e) { return null; }
    }

    private void addAssetToIndex(String projectId, String id) {
        String key   = "index::" + projectId;
        String index = assetsPrefs.getString(key, "");
        assetsPrefs.edit().putString(key, index.isEmpty() ? id : index + "|||" + id).apply();
    }

    private void removeAssetFromIndex(String projectId, String id) {
        String key   = "index::" + projectId;
        String index = assetsPrefs.getString(key, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String i : index.split("\\|\\|\\|")) {
            if (!i.equals(id)) { if (sb.length() > 0) sb.append("|||"); sb.append(i); }
        }
        assetsPrefs.edit().putString(key, sb.toString()).apply();
    }

    // ═══════════════════ MIGRATION ═══════════════════

    public void migrateLegacyIfNeeded() {
        SharedPreferences legacyIndexPrefs = ctx.getSharedPreferences("vcg_index", Context.MODE_PRIVATE);
        SharedPreferences legacyFilesPrefs = ctx.getSharedPreferences("vcg_files",  Context.MODE_PRIVATE);
        String legacyIndex = legacyIndexPrefs.getString("file_list", "");
        if (legacyIndex.isEmpty() || hasAnyProject()) return;
        VcgProject defaultProject = new VcgProject(newId(), "مشروعي الأول",
            "تم استيراده من النسخة السابقة", "#4DC95A");
        saveProject(defaultProject);
        for (String name : legacyIndex.split("\\|\\|\\|")) {
            if (name.isEmpty()) continue;
            saveFile(new VcgFile(defaultProject.getId(), name,
                legacyFilesPrefs.getString(name, "")));
        }
        legacyIndexPrefs.edit().clear().apply();
    }
}
