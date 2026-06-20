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
 * VcgStorage — تخزين المشاريع، الملفات، والأصول (صور/فيديو) محلياً
 * يستخدم SharedPreferences + JSON لتمثيل البيانات المهيكلة
 */
public class VcgStorage {

    private static final String PREFS_PROJECTS = "vcg_projects";
    private static final String PREFS_FILES    = "vcg_files_v2";
    private static final String PREFS_ASSETS   = "vcg_assets_v2";
    private static final String KEY_PROJECTS   = "project_list";

    private final SharedPreferences projectsPrefs;
    private final SharedPreferences filesPrefs;
    private final SharedPreferences assetsPrefs;
    private final Context ctx;

    public VcgStorage(Context ctx) {
        this.ctx = ctx;
        projectsPrefs = ctx.getSharedPreferences(PREFS_PROJECTS, Context.MODE_PRIVATE);
        filesPrefs    = ctx.getSharedPreferences(PREFS_FILES, Context.MODE_PRIVATE);
        assetsPrefs   = ctx.getSharedPreferences(PREFS_ASSETS, Context.MODE_PRIVATE);
    }

    // ═══════════════════ PROJECTS ═══════════════════

    public void saveProject(VcgProject p) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", p.getId());
            o.put("name", p.getName());
            o.put("description", p.getDescription());
            o.put("colorTag", p.getColorTag());
            o.put("createdAt", p.getCreatedAt());
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
        Collections.sort(result, new Comparator<VcgProject>() {
            @Override public int compare(VcgProject a, VcgProject b) {
                return Long.compare(b.getLastModified(), a.getLastModified());
            }
        });
        return result;
    }

    public void deleteProject(String id) {
        for (VcgFile f : getFilesInProject(id)) {
            deleteFile(id, f.getName());
        }
        for (VcgAsset a : getAssetsInProject(id)) {
            deleteAsset(a.getId());
        }
        projectsPrefs.edit().remove(id).apply();
        removeFromProjectIndex(id);
    }

    public boolean hasAnyProject() {
        String index = projectsPrefs.getString(KEY_PROJECTS, "");
        return !index.isEmpty();
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
        String newIndex = index.isEmpty() ? id : index + "|||" + id;
        projectsPrefs.edit().putString(KEY_PROJECTS, newIndex).apply();
    }

    private void removeFromProjectIndex(String id) {
        String index = projectsPrefs.getString(KEY_PROJECTS, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String p : index.split("\\|\\|\\|")) {
            if (!p.equals(id)) {
                if (sb.length() > 0) sb.append("|||");
                sb.append(p);
            }
        }
        projectsPrefs.edit().putString(KEY_PROJECTS, sb.toString()).apply();
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ═══════════════════ FILES ═══════════════════

    public void saveFile(VcgFile file) {
        filesPrefs.edit().putString(file.getStorageKey(), file.getContent()).apply();
        addFileToIndex(file.getProjectId(), file.getName());
        VcgProject p = getProject(file.getProjectId());
        if (p != null) { p.touch(); saveProject(p); }
    }

    public VcgFile getFile(String projectId, String name) {
        String content = filesPrefs.getString(projectId + "::" + name, null);
        if (content == null) return null;
        return new VcgFile(projectId, name, content);
    }

    public List<VcgFile> getFilesInProject(String projectId) {
        List<VcgFile> result = new ArrayList<>();
        String index = filesPrefs.getString("index::" + projectId, "");
        if (index.isEmpty()) return result;
        for (String name : index.split("\\|\\|\\|")) {
            if (!name.isEmpty()) {
                String content = filesPrefs.getString(projectId + "::" + name, "");
                result.add(new VcgFile(projectId, name, content));
            }
        }
        return result;
    }

    public void deleteFile(String projectId, String name) {
        filesPrefs.edit().remove(projectId + "::" + name).apply();
        removeFileFromIndex(projectId, name);
    }

    public boolean fileExists(String projectId, String name) {
        return filesPrefs.contains(projectId + "::" + name);
    }

    public void renameFile(String projectId, String oldName, String newName) {
        VcgFile f = getFile(projectId, oldName);
        if (f == null) return;
        deleteFile(projectId, oldName);
        saveFile(new VcgFile(projectId, newName, f.getContent()));
    }

    private void addFileToIndex(String projectId, String name) {
        String key = "index::" + projectId;
        String index = filesPrefs.getString(key, "");
        if (!index.isEmpty()) {
            for (String n : index.split("\\|\\|\\|")) if (n.equals(name)) return;
        }
        String newIndex = index.isEmpty() ? name : index + "|||" + name;
        filesPrefs.edit().putString(key, newIndex).apply();
    }

    private void removeFileFromIndex(String projectId, String name) {
        String key = "index::" + projectId;
        String index = filesPrefs.getString(key, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String n : index.split("\\|\\|\\|")) {
            if (!n.equals(name)) {
                if (sb.length() > 0) sb.append("|||");
                sb.append(n);
            }
        }
        filesPrefs.edit().putString(key, sb.toString()).apply();
    }

    // ═══════════════════ ASSETS (images/video) ═══════════════════

    public VcgAsset saveAsset(String projectId, String name, String mimeType, String base64Data, long size) {
        String id = newId();
        VcgAsset asset = new VcgAsset(id, projectId, name, mimeType, base64Data, size);
        try {
            JSONObject o = new JSONObject();
            o.put("id", asset.getId());
            o.put("projectId", asset.getProjectId());
            o.put("name", asset.getName());
            o.put("mimeType", asset.getMimeType());
            o.put("data", asset.getBase64Data());
            o.put("size", asset.getSize());
            o.put("addedAt", asset.getAddedAt());
            assetsPrefs.edit().putString(id, o.toString()).apply();
            addAssetToIndex(projectId, id);
        } catch (JSONException ignored) {}
        return asset;
    }

    public VcgAsset getAsset(String id) {
        String raw = assetsPrefs.getString(id, null);
        if (raw == null) return null;
        return parseAsset(raw);
    }

    public List<VcgAsset> getAssetsInProject(String projectId) {
        List<VcgAsset> result = new ArrayList<>();
        String index = assetsPrefs.getString("index::" + projectId, "");
        if (index.isEmpty()) return result;
        for (String id : index.split("\\|\\|\\|")) {
            if (id.isEmpty()) continue;
            String raw = assetsPrefs.getString(id, null);
            if (raw != null) {
                VcgAsset a = parseAsset(raw);
                if (a != null) result.add(a);
            }
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
        String key = "index::" + projectId;
        String index = assetsPrefs.getString(key, "");
        String newIndex = index.isEmpty() ? id : index + "|||" + id;
        assetsPrefs.edit().putString(key, newIndex).apply();
    }

    private void removeAssetFromIndex(String projectId, String id) {
        String key = "index::" + projectId;
        String index = assetsPrefs.getString(key, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String i : index.split("\\|\\|\\|")) {
            if (!i.equals(id)) {
                if (sb.length() > 0) sb.append("|||");
                sb.append(i);
            }
        }
        assetsPrefs.edit().putString(key, sb.toString()).apply();
    }

    // ═══════════════════ MIGRATION from old flat-file storage ═══════════════════

    /** Migrates old "vcg_files"/"vcg_index" flat storage (pre-projects) into a default project. */
    public void migrateLegacyIfNeeded() {
        SharedPreferences legacyIndexPrefs = ctx.getSharedPreferences("vcg_index", Context.MODE_PRIVATE);
        SharedPreferences legacyFilesPrefs  = ctx.getSharedPreferences("vcg_files", Context.MODE_PRIVATE);
        String legacyIndex = legacyIndexPrefs.getString("file_list", "");
        if (legacyIndex.isEmpty()) return;
        if (hasAnyProject()) return;

        VcgProject defaultProject = new VcgProject(newId(), "مشروعي الأول", "تم استيراده من النسخة السابقة", "#4DC95A");
        saveProject(defaultProject);
        for (String name : legacyIndex.split("\\|\\|\\|")) {
            if (name.isEmpty()) continue;
            String content = legacyFilesPrefs.getString(name, "");
            saveFile(new VcgFile(defaultProject.getId(), name, content));
        }
        legacyIndexPrefs.edit().clear().apply();
    }
}
