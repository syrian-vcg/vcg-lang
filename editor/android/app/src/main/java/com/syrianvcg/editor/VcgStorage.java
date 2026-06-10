package com.syrianvcg.editor;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VcgStorage {
    private static final String PREFS_FILES  = "vcg_files";
    private static final String PREFS_INDEX  = "vcg_index";
    private static final String KEY_INDEX    = "file_list";
    private static final String SEPARATOR    = "|||";

    private final SharedPreferences filesPrefs;
    private final SharedPreferences indexPrefs;

    public VcgStorage(Context ctx) {
        filesPrefs = ctx.getSharedPreferences(PREFS_FILES, Context.MODE_PRIVATE);
        indexPrefs = ctx.getSharedPreferences(PREFS_INDEX, Context.MODE_PRIVATE);
    }

    public void saveFile(VcgFile file) {
        // Save content
        filesPrefs.edit().putString(file.getName(), file.getContent()).apply();
        // Update index
        addToIndex(file.getName());
    }

    public VcgFile getFile(String name) {
        String content = filesPrefs.getString(name, null);
        if (content == null) return null;
        return new VcgFile(name, content);
    }

    public List<VcgFile> getAllFiles() {
        List<VcgFile> result = new ArrayList<>();
        String index = indexPrefs.getString(KEY_INDEX, "");
        if (index.isEmpty()) return result;
        String[] names = index.split("\\|\\|\\|");
        for (String name : names) {
            if (!name.isEmpty()) {
                String content = filesPrefs.getString(name, "");
                result.add(new VcgFile(name, content));
            }
        }
        return result;
    }

    public void deleteFile(String name) {
        filesPrefs.edit().remove(name).apply();
        removeFromIndex(name);
    }

    public boolean fileExists(String name) {
        return filesPrefs.contains(name);
    }

    private void addToIndex(String name) {
        String index = indexPrefs.getString(KEY_INDEX, "");
        // Don't add if already exists
        if (!index.isEmpty()) {
            String[] parts = index.split("\\|\\|\\|");
            for (String p : parts) if (p.equals(name)) return;
        }
        String newIndex = index.isEmpty() ? name : index + SEPARATOR + name;
        indexPrefs.edit().putString(KEY_INDEX, newIndex).apply();
    }

    private void removeFromIndex(String name) {
        String index = indexPrefs.getString(KEY_INDEX, "");
        if (index.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        String[] parts = index.split("\\|\\|\\|");
        for (String p : parts) {
            if (!p.equals(name)) {
                if (sb.length() > 0) sb.append(SEPARATOR);
                sb.append(p);
            }
        }
        indexPrefs.edit().putString(KEY_INDEX, sb.toString()).apply();
    }
}
