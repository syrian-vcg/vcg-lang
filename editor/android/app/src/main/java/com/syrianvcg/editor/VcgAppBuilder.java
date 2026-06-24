package com.syrianvcg.editor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

/**
 * VcgAppBuilder — بنّاء التطبيقات في VCG Editor
 *
 * يُنفَّذ تلقائياً عندما يفتح المستخدم ملف Generate_Stack.apk.yml
 * في المحرر ويضغط زر التشغيل.
 *
 * الميزات:
 *   • يقرأ $set.name_app / $set.app.package / $set.app.version / $set.get.icon
 *   • يدعم data_vcg و get.data لتخزين البيانات
 *   • يُنشئ APK وهمي (قالب مُعبَّأ) + ZIP كامل للمشروع + PDF وثائق
 *   • يُحرِّك شريط التقدم عبر BuildListener
 */
public class VcgAppBuilder {

    // ── ثوابت ───────────────────────────────────────────────────────
    public static final String STACK_FILE = "Generate_Stack.apk.yml";

    // ── واجهة المستمع ────────────────────────────────────────────────
    public interface BuildListener {
        void onProgress(int percent, String message);
        void onSuccess(BuildResult result);
        void onError(String error);
    }

    // ── نتيجة البناء ─────────────────────────────────────────────────
    public static class BuildResult {
        public File apkFile;
        public File zipFile;
        public File pdfFile;
        public String appName;
        public String appPackage;
        public String appVersion;
        public long buildTimeMs;

        public List<Uri> getAllUris(Context ctx) {
            List<Uri> uris = new ArrayList<>();
            if (apkFile != null && apkFile.exists())
                uris.add(shareUri(ctx, apkFile));
            if (zipFile != null && zipFile.exists())
                uris.add(shareUri(ctx, zipFile));
            if (pdfFile != null && pdfFile.exists())
                uris.add(shareUri(ctx, pdfFile));
            return uris;
        }

        private Uri shareUri(Context ctx, File f) {
            return FileProvider.getUriForFile(
                ctx, ctx.getPackageName() + ".fileprovider", f);
        }
    }

    // ── AppMeta ──────────────────────────────────────────────────────
    public static class AppMeta {
        public String name    = "VCG App";
        public String pkg     = "com.vcg.app";
        public String version = "1.0.0";
        public String icon    = "";
        public boolean outputApk = true;
        public boolean outputZip = true;
        public boolean outputPdf = true;
    }

    // ── نقطة الدخول الرئيسية ─────────────────────────────────────────
    public static void build(
            Context ctx,
            VcgStorage storage,
            VcgProject project,
            BuildListener listener) {

        new Thread(() -> {
            Handler ui = new Handler(Looper.getMainLooper());
            try {
                long t0 = System.currentTimeMillis();

                // 1) اقرأ ملفات المشروع
                emit(ui, listener, 5, "📂 جارٍ قراءة ملفات المشروع...");
                List<VcgFile> files = storage.getFilesInProject(project.getId());
                List<VcgAsset> assets = storage.getAssetsInProject(project.getId());

                // 2) استخرج بيانات التطبيق من الكود
                emit(ui, listener, 15, "🔍 تحليل إعدادات التطبيق...");
                AppMeta meta = extractMeta(files, project);

                // 3) بناء APK
                File outDir = buildDir(ctx);
                BuildResult result = new BuildResult();
                result.appName    = meta.name;
                result.appPackage = meta.pkg;
                result.appVersion = meta.version;

                if (meta.outputApk) {
                    emit(ui, listener, 30, "⚙️ جارٍ بناء APK...");
                    result.apkFile = buildApk(ctx, outDir, meta, files, assets);
                    emit(ui, listener, 60, "✅ APK جاهز: " + result.apkFile.getName());
                }

                // 4) بناء ZIP
                if (meta.outputZip) {
                    emit(ui, listener, 65, "📦 جارٍ ضغط ملفات المشروع...");
                    result.zipFile = buildZip(outDir, meta, files, assets);
                    emit(ui, listener, 80, "✅ ZIP جاهز: " + result.zipFile.getName());
                }

                // 5) بناء PDF
                if (meta.outputPdf) {
                    emit(ui, listener, 85, "📄 جارٍ إنشاء وثيقة PDF...");
                    result.pdfFile = buildPdf(outDir, meta, files);
                    emit(ui, listener, 95, "✅ PDF جاهز: " + result.pdfFile.getName());
                }

                result.buildTimeMs = System.currentTimeMillis() - t0;
                emit(ui, listener, 100, "🎉 اكتمل البناء في " + result.buildTimeMs + "ms");

                ui.post(() -> listener.onSuccess(result));

            } catch (Exception e) {
                ui.post(() -> listener.onError(e.getMessage() != null
                    ? e.getMessage() : "خطأ غير معروف أثناء البناء"));
            }
        }, "vcg-build-thread").start();
    }

    // ── استخراج AppMeta من كود VCG ───────────────────────────────────
    public static AppMeta extractMeta(List<VcgFile> files, VcgProject project) {
        AppMeta meta = new AppMeta();
        meta.name = project.getName();
        meta.pkg  = "com.vcg." + project.getName()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");

        // ابحث في Generate_Stack.apk.yml أولاً
        VcgFile stackFile = null;
        for (VcgFile f : files) {
            if (STACK_FILE.equals(f.getName())) { stackFile = f; break; }
        }
        if (stackFile != null && stackFile.getContent() != null) {
            parseYmlMeta(stackFile.getContent(), meta);
        }

        // ابحث في ملفات VCG عن $set.name_app / $set.app.package ...
        for (VcgFile f : files) {
            if (f.getName() != null && f.getName().endsWith(".vcg") && f.getContent() != null) {
                parseVcgMeta(f.getContent(), meta);
            }
        }
        return meta;
    }

    private static void parseYmlMeta(String yml, AppMeta meta) {
        for (String raw : yml.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("name:"))
                meta.name = stripQuotes(line.substring(5).trim());
            else if (line.startsWith("package:"))
                meta.pkg = stripQuotes(line.substring(8).trim());
            else if (line.startsWith("version:"))
                meta.version = stripQuotes(line.substring(8).trim());
            else if (line.startsWith("icon:"))
                meta.icon = stripQuotes(line.substring(5).trim());
            else if (line.startsWith("apk:"))
                meta.outputApk = !line.contains("false");
            else if (line.startsWith("zip:"))
                meta.outputZip = !line.contains("false");
            else if (line.startsWith("pdf:"))
                meta.outputPdf = !line.contains("false");
        }
    }

    private static void parseVcgMeta(String code, AppMeta meta) {
        // $set.name_app("...")
        String name = extractArg(code, "$set.name_app");
        if (name != null) meta.name = name;
        // $set.app.package("...")
        String pkg = extractArg(code, "$set.app.package");
        if (pkg != null) meta.pkg = pkg;
        // $set.app.version("...")
        String ver = extractArg(code, "$set.app.version");
        if (ver != null) meta.version = ver;
        // $set.get.icon("...")
        String icon = extractArg(code, "$set.get.icon");
        if (icon != null) meta.icon = icon;
    }

    private static String extractArg(String code, String key) {
        int idx = code.indexOf(key);
        if (idx < 0) return null;
        int open = code.indexOf('(', idx);
        int close = code.indexOf(')', open);
        if (open < 0 || close < 0) return null;
        String inner = code.substring(open + 1, close).trim();
        return stripQuotes(inner);
    }

    private static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) ||
            (s.startsWith("'")  && s.endsWith("'")))
            return s.substring(1, s.length() - 1);
        // remove YAML inline comment
        int hash = s.indexOf('#');
        if (hash > 0) s = s.substring(0, hash).trim();
        return s;
    }

    // ── بناء APK (قالب مُعبَّأ) ─────────────────────────────────────
    private static File buildApk(Context ctx, File outDir, AppMeta meta,
                                 List<VcgFile> files, List<VcgAsset> assets) throws IOException {
        String safeName = safe(meta.name);
        File apk = new File(outDir, safeName + "-" + meta.version + ".apk");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(apk))) {
            // AndroidManifest.xml مُوَلَّد
            writeZ(zos, "AndroidManifest.xml",
                buildManifest(meta).getBytes(StandardCharsets.UTF_8));

            // classes.dex رمزي
            writeZ(zos, "classes.dex",
                ("# VCG Generated DEX placeholder\n# app: " + meta.pkg).getBytes());

            // مصادر VCG
            for (VcgFile f : files) {
                if (f.getContent() != null)
                    writeZ(zos, "assets/vcg/" + f.getName(),
                        f.getContent().getBytes(StandardCharsets.UTF_8));
            }

            // الأصول
            for (VcgAsset a : assets) {
                try {
                    byte[] bytes = Base64.decode(a.getBase64Data(), Base64.DEFAULT);
                    writeZ(zos, "assets/" + a.getName(), bytes);
                } catch (Exception ignored) {}
            }

            // res/values/strings.xml
            writeZ(zos, "res/values/strings.xml",
                buildStringsXml(meta).getBytes(StandardCharsets.UTF_8));

            // META-INF
            writeZ(zos, "META-INF/MANIFEST.MF",
                ("Manifest-Version: 1.0\nBuilt-By: VCG-Editor\nCreated-By: VCG v2.0\n"
                    + "App-Name: " + meta.name + "\nApp-Package: " + meta.pkg + "\n"
                    + "App-Version: " + meta.version + "\n").getBytes());
        }
        return apk;
    }

    // ── بناء ZIP للمشروع كاملاً ─────────────────────────────────────
    private static File buildZip(File outDir, AppMeta meta,
                                 List<VcgFile> files, List<VcgAsset> assets) throws IOException {
        String safeName = safe(meta.name);
        File zip = new File(outDir, safeName + "-" + meta.version + "-project.zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            // manifest
            JSONObject manifest = new JSONObject();
            try {
                manifest.put("vcg_export", "app-project");
                manifest.put("app_name", meta.name);
                manifest.put("package", meta.pkg);
                manifest.put("version", meta.version);
                manifest.put("exported_at", new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
            } catch (Exception ignored) {}
            writeZ(zos, "manifest.json", manifest.toString().getBytes(StandardCharsets.UTF_8));

            // Generate_Stack.apk.yml
            writeZ(zos, STACK_FILE, buildStackYml(meta).getBytes(StandardCharsets.UTF_8));

            // ملفات VCG
            for (VcgFile f : files) {
                if (f.getContent() != null)
                    writeZ(zos, "src/" + f.getName(),
                        f.getContent().getBytes(StandardCharsets.UTF_8));
            }
            // الأصول
            for (VcgAsset a : assets) {
                try {
                    byte[] bytes = Base64.decode(a.getBase64Data(), Base64.DEFAULT);
                    writeZ(zos, "assets/" + a.getName(), bytes);
                } catch (Exception ignored) {}
            }
        }
        return zip;
    }

    // ── بناء PDF (HTML→متن مُعبَّأ مع رأس) ──────────────────────────
    private static File buildPdf(File outDir, AppMeta meta, List<VcgFile> files) throws IOException {
        String safeName = safe(meta.name);
        File pdf = new File(outDir, safeName + "-" + meta.version + "-docs.pdf");

        // نُنشئ PDF بصيغة نصية مُبسَّطة (PDF text-only شرعي)
        StringBuilder body = new StringBuilder();
        body.append("VCG App Documentation\n\n");
        body.append("App Name    : ").append(meta.name).append("\n");
        body.append("Package     : ").append(meta.pkg).append("\n");
        body.append("Version     : ").append(meta.version).append("\n");
        body.append("Built By    : VCG Editor v2.0\n");
        body.append("Built At    : ")
            .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date()))
            .append("\n\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        body.append("Source Files\n");
        body.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        for (VcgFile f : files) {
            body.append("▸ ").append(f.getName()).append("\n");
            if (f.getContent() != null) {
                for (String line : f.getContent().split("\n")) {
                    body.append("  ").append(line).append("\n");
                }
            }
            body.append("\n");
        }

        writePdf(pdf, meta.name + " v" + meta.version, body.toString());
        return pdf;
    }

    /** كاتب PDF خفيف الوزن يتوافق مع مواصفة PDF 1.4 */
    private static void writePdf(File out, String title, String bodyText) throws IOException {
        // PDF structure: header + catalog + pages + content + xref + trailer
        List<Long> offsets = new ArrayList<>();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // Wrap bodyText lines into 80-char segments for PDF stream
        StringBuilder stream = new StringBuilder();
        // BT = Begin Text
        stream.append("BT\n");
        stream.append("/F1 10 Tf\n");   // font
        stream.append("50 780 Td\n");   // start pos
        stream.append("14 TL\n");       // leading

        for (String rawLine : bodyText.split("\n")) {
            // escape PDF special chars
            String esc = rawLine
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
            stream.append("(").append(esc).append(") '\n");
        }
        stream.append("ET\n");

        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);

        // ── obj 1: catalog
        // ── obj 2: pages dict
        // ── obj 3: page
        // ── obj 4: content stream
        // ── obj 5: font

        String pdfHeader = "%PDF-1.4\n";
        buf.write(pdfHeader.getBytes());

        offsets.add((long) buf.size());
        String obj1 = "1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n";
        buf.write(obj1.getBytes());

        offsets.add((long) buf.size());
        String obj2 = "2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n";
        buf.write(obj2.getBytes());

        offsets.add((long) buf.size());
        String obj3 = "3 0 obj\n<</Type /Page /Parent 2 0 R"
            + " /MediaBox [0 0 595 842]"
            + " /Contents 4 0 R"
            + " /Resources <</Font <</F1 5 0 R>>>>>>\nendobj\n";
        buf.write(obj3.getBytes());

        offsets.add((long) buf.size());
        String obj4 = "4 0 obj\n<</Length " + streamBytes.length + ">>\nstream\n";
        buf.write(obj4.getBytes());
        buf.write(streamBytes);
        buf.write("\nendstream\nendobj\n".getBytes());

        offsets.add((long) buf.size());
        String obj5 = "5 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Courier>>\nendobj\n";
        buf.write(obj5.getBytes());

        // xref
        long xrefOffset = buf.size();
        buf.write("xref\n0 6\n0000000000 65535 f \n".getBytes());
        for (Long o : offsets) {
            buf.write(String.format("%010d 00000 n \n", o).getBytes());
        }

        String trailer = "trailer\n<</Size 6 /Root 1 0 R>>\nstartxref\n"
            + xrefOffset + "\n%%EOF\n";
        buf.write(trailer.getBytes());

        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(buf.toByteArray());
        }
    }

    // ── مساعدات XML ─────────────────────────────────────────────────
    private static String buildManifest(AppMeta meta) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"" + meta.pkg + "\"\n"
            + "    android:versionCode=\"1\"\n"
            + "    android:versionName=\"" + meta.version + "\">\n"
            + "  <uses-sdk android:minSdkVersion=\"21\" android:targetSdkVersion=\"34\"/>\n"
            + "  <application\n"
            + "      android:label=\"" + meta.name + "\"\n"
            + "      android:icon=\"@mipmap/ic_launcher\">\n"
            + "    <activity android:name=\".MainActivity\" android:exported=\"true\">\n"
            + "      <intent-filter>\n"
            + "        <action android:name=\"android.intent.action.MAIN\"/>\n"
            + "        <category android:name=\"android.intent.category.LAUNCHER\"/>\n"
            + "      </intent-filter>\n"
            + "    </activity>\n"
            + "  </application>\n"
            + "</manifest>\n";
    }

    private static String buildStringsXml(AppMeta meta) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "  <string name=\"app_name\">" + meta.name + "</string>\n"
            + "  <string name=\"app_version\">" + meta.version + "</string>\n"
            + "  <string name=\"app_package\">" + meta.pkg + "</string>\n"
            + "</resources>\n";
    }

    private static String buildStackYml(AppMeta meta) {
        return "# Generate_Stack.apk.yml — Generated by VCG Editor\n"
            + "app:\n"
            + "  name:    \"" + meta.name + "\"\n"
            + "  package: \"" + meta.pkg + "\"\n"
            + "  version: \"" + meta.version + "\"\n"
            + "  icon:    \"" + meta.icon + "\"\n"
            + "outputs:\n"
            + "  apk: true\n"
            + "  zip: true\n"
            + "  pdf: true\n";
    }

    // ── أدوات مساعدة ────────────────────────────────────────────────
    private static void writeZ(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    private static File buildDir(Context ctx) {
        File dir = new File(ctx.getCacheDir(), "vcg_builds");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static String safe(String name) {
        return name.replaceAll("[^\\p{L}\\p{N}_\\-]+", "_");
    }

    private static void emit(Handler ui, BuildListener l, int pct, String msg) {
        ui.post(() -> l.onProgress(pct, msg));
        try { Thread.sleep(120); } catch (InterruptedException ignored) {}
    }

    // ── كشف ملف Generate_Stack.apk.yml ──────────────────────────────
    public static boolean isStackFile(String fileName) {
        return STACK_FILE.equals(fileName);
    }
}
