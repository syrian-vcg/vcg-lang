package com.syrianvcg.editor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class VcgExport {

    private VcgExport() {}

    private static File exportsDir(Context ctx) {
        File dir = new File(ctx.getCacheDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static Uri shareUri(Context ctx, File file) {
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
    }

    private static void startShare(Context ctx, Uri uri, String mime, String chooserTitle) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mime);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void exportProject(Context ctx, VcgStorage storage, VcgProject project) throws IOException {
        String safeName = project.getName().replaceAll("[^\\p{L}\\p{N}_\\-]+", "_");
        File zipFile = new File(exportsDir(ctx), safeName + ".vcgzip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            JSONObject manifest = new JSONObject();
            try {
                manifest.put("format", "vcg-project-export");
                manifest.put("version", 1);
                manifest.put("name", project.getName());
                manifest.put("description", project.getDescription());
                manifest.put("exportedAt", System.currentTimeMillis());
            } catch (Exception ignored) {}
            writeEntry(zos, "manifest.json", manifest.toString().getBytes("UTF-8"));

            List<VcgFile> files = storage.getFilesInProject(project.getId());
            for (VcgFile f : files) {
                writeEntry(zos, "files/" + f.getName(),
                    f.getContent() != null ? f.getContent().getBytes("UTF-8") : new byte[0]);
            }

            List<VcgAsset> assets = storage.getAssetsInProject(project.getId());
            for (VcgAsset a : assets) {
                byte[] bytes = Base64.decode(a.getBase64Data(), Base64.DEFAULT);
                writeEntry(zos, "assets/" + a.getName(), bytes);
            }
        }

        startShare(ctx, shareUri(ctx, zipFile), "application/zip",
            "تصدير مشروع \"" + project.getName() + "\"");
    }

    public static void exportFullBackup(Context ctx, VcgStorage storage) throws IOException {
        JSONObject root = new JSONObject();
        try {
            root.put("format", "vcg-full-backup");
            root.put("version", 1);
            root.put("exportedAt", System.currentTimeMillis());
            JSONArray projectsArr = new JSONArray();
            for (VcgProject p : storage.getAllProjects()) {
                JSONObject pj = new JSONObject();
                pj.put("id", p.getId());
                pj.put("name", p.getName());
                pj.put("description", p.getDescription());
                JSONArray filesArr = new JSONArray();
                for (VcgFile f : storage.getFilesInProject(p.getId())) {
                    JSONObject fj = new JSONObject();
                    fj.put("name", f.getName());
                    fj.put("content", f.getContent());
                    filesArr.put(fj);
                }
                pj.put("files", filesArr);
                projectsArr.put(pj);
            }
            root.put("projects", projectsArr);
        } catch (Exception e) {
            throw new IOException("فشل بناء النسخة الاحتياطية: " + e.getMessage());
        }

        // ✅ الإصلاح الأول: فصل toString(2) في try/catch منفصل
        String jsonContent;
        try {
            jsonContent = root.toString(2);
        } catch (org.json.JSONException e) {
            throw new IOException(e.getMessage());
        }

        File jsonFile = new File(exportsDir(ctx), "vcg_backup.json");
        try (FileOutputStream fos = new FileOutputStream(jsonFile)) {
            fos.write(jsonContent.getBytes("UTF-8"));
        }

        startShare(ctx, shareUri(ctx, jsonFile), "application/json", "نسخة احتياطية كاملة من VCG Editor");
    }

    private static void writeEntry(ZipOutputStream zos, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private static String safeEntryFileName(String entryName) {
        String name = entryName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replace("..", "_").trim();
        return name.isEmpty() ? "file" : name;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    private static String guessMime(String fileName) {
        String n = fileName.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".mp4")) return "video/mp4";
        if (n.endsWith(".webm")) return "video/webm";
        if (n.endsWith(".mov")) return "video/quicktime";
        return "application/octet-stream";
    }

    public static VcgProject importProject(Context ctx, VcgStorage storage, Uri zipUri) throws IOException {
        String manifestName = null;
        String manifestDesc = null;
        List<String[]> pendingFiles = new ArrayList<>();
        List<Object[]> pendingAssets = new ArrayList<>();

        // ✅ الإصلاح الثاني: catch (IOException) بدلاً من catch (JSONException)
        try (InputStream rawIn = ctx.getContentResolver().openInputStream(zipUri)) {
            if (rawIn == null) throw new IOException("تعذّر فتح الملف المحدد");
            ZipInputStream zis = new ZipInputStream(rawIn);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }
                String path = entry.getName().replace('\\', '/');
                byte[] data = readAllBytes(zis);
                zis.closeEntry();

                if (path.equals("manifest.json")) {
                    try {
                        JSONObject m = new JSONObject(new String(data, StandardCharsets.UTF_8));
                        manifestName = m.optString("name", null);
                        manifestDesc = m.optString("description", "");
                    } catch (Exception ignored) {}
                } else if (path.startsWith("files/")) {
                    String fname = safeEntryFileName(path);
                    pendingFiles.add(new String[]{ fname, new String(data, StandardCharsets.UTF_8) });
                } else if (path.startsWith("assets/")) {
                    String aname = safeEntryFileName(path);
                    pendingAssets.add(new Object[]{ aname, guessMime(aname), data });
                }
            }
        } catch (IOException e) {
            throw new IOException("ملف الأرشيف تالف أو غير متوافق: " + e.getMessage());
        }

        if (pendingFiles.isEmpty() && pendingAssets.isEmpty() && manifestName == null) {
            throw new IOException("لم يتم العثور على بيانات مشروع صالحة داخل هذا الأرشيف");
        }

        String baseName = (manifestName == null || manifestName.trim().isEmpty())
            ? "مشروع مستورد" : manifestName.trim();
        String finalName = baseName + " (مستورد)";
        int suffix = 2;
        for (VcgProject existing : storage.getAllProjects()) {
            if (existing.getName().equals(finalName)) {
                finalName = baseName + " (مستورد " + suffix + ")";
                suffix++;
            }
        }

        VcgProject project = new VcgProject(VcgStorage.newId(), finalName,
            manifestDesc != null ? manifestDesc : "", "#6AB0FF");
        storage.saveProject(project);

        for (String[] f : pendingFiles) {
            storage.saveFile(new VcgFile(project.getId(), f[0], f[1]));
        }
        for (Object[] a : pendingAssets) {
            String aname = (String) a[0];
            String mime = (String) a[1];
            byte[] bytes = (byte[]) a[2];
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            storage.saveAsset(project.getId(), aname, mime, base64, bytes.length);
        }
        if (pendingFiles.isEmpty()) {
            storage.saveFile(new VcgFile(project.getId(), "main.vcg",
                "# " + finalName + "\nshow(\"مرحباً من " + finalName + "\")\n"));
        }
        return project;
    }
}
