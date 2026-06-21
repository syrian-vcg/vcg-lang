package com.syrianvcg.editor;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * VcgGitHub — تكامل بسيط مع GitHub عبر REST API باستخدام Personal Access Token (PAT).
 * كل الدوال هنا متزامنة (Blocking) ويجب استدعاؤها من Thread خلفي، لا من الواجهة الرئيسية.
 */
public final class VcgGitHub {

    private VcgGitHub() {}

    public static class Repo {
        public final String fullName;
        public final boolean isPrivate;
        public Repo(String fullName, boolean isPrivate) {
            this.fullName = fullName; this.isPrivate = isPrivate;
        }
        @Override public String toString() { return fullName + (isPrivate ? "  🔒" : ""); }
    }

    public static class GithubException extends Exception {
        public GithubException(String msg) { super(msg); }
    }

    private static HttpURLConnection open(String urlStr, String token, String method) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        c.setRequestProperty("User-Agent", "SyrianVCGEditor");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        return c;
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toString("UTF-8");
    }

    private static String bodyOrError(HttpURLConnection c) throws IOException {
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String body = readAll(stream);
        if (code < 200 || code >= 300) {
            String msg = "GitHub API (" + code + ")";
            try { msg = new JSONObject(body).optString("message", msg); } catch (Exception ignored) {}
            throw new IOException(msg);
        }
        return body;
    }

    /** يتحقق من صلاحية الرمز ويرجع اسم المستخدم. */
    public static String validateTokenAndGetUsername(String token) throws IOException, GithubException {
        if (token == null || token.trim().isEmpty()) throw new GithubException("الرمز فارغ");
        HttpURLConnection c = open("https://api.github.com/user", token, "GET");
        try {
            String body = bodyOrError(c);
            JSONObject obj = new JSONObject(body);
            return obj.optString("login", null);
        } catch (org.json.JSONException e) {
            throw new GithubException("تعذّر قراءة استجابة GitHub");
        } finally {
            c.disconnect();
        }
    }

    /** يجلب أول 100 مستودع للمستخدم، مرتّبة بحسب آخر تحديث. */
    public static List<Repo> listRepos(String token) throws IOException, GithubException {
        HttpURLConnection c = open("https://api.github.com/user/repos?per_page=100&sort=updated", token, "GET");
        List<Repo> result = new ArrayList<>();
        try {
            String body = bodyOrError(c);
            JSONArray arr = new JSONArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject r = arr.getJSONObject(i);
                result.add(new Repo(r.getString("full_name"), r.optBoolean("private", false)));
            }
            return result;
        } catch (org.json.JSONException e) {
            throw new GithubException("تعذّر قراءة قائمة المستودعات");
        } finally {
            c.disconnect();
        }
    }

    /** ينشئ مستودعاً جديداً في حساب المستخدم، ويرجع full_name (owner/repo). */
    public static String createRepo(String token, String name, boolean isPrivate) throws IOException, GithubException {
        HttpURLConnection c = open("https://api.github.com/user/repos", token, "POST");
        c.setDoOutput(true);
        try {
            JSONObject payload = new JSONObject();
            payload.put("name", name);
            payload.put("private", isPrivate);
            payload.put("auto_init", false);
            payload.put("description", "Syrian VCG project — uploaded from VCG Editor");
            byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setFixedLengthStreamingMode(data.length);
            try (OutputStream os = c.getOutputStream()) { os.write(data); }
            String body = bodyOrError(c);
            return new JSONObject(body).getString("full_name");
        } catch (org.json.JSONException e) {
            throw new GithubException("تعذّر إنشاء المستودع");
        } finally {
            c.disconnect();
        }
    }

    private static String getFileSha(String token, String fullRepo, String path) {
        try {
            HttpURLConnection c = open("https://api.github.com/repos/" + fullRepo + "/contents/" + encodePath(path), token, "GET");
            try {
                if (c.getResponseCode() == 200) {
                    String body = readAll(c.getInputStream());
                    return new JSONObject(body).optString("sha", null);
                }
            } finally { c.disconnect(); }
        } catch (Exception ignored) {
            // ملف غير موجود بعد، أو خطأ شبكة عابر — سنحاول إنشاءه دون sha.
        }
        return null;
    }

    private static String encodePath(String path) {
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/")) {
            if (sb.length() > 0) sb.append('/');
            try { sb.append(java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20")); }
            catch (Exception e) { sb.append(seg); }
        }
        return sb.toString();
    }

    /** ينشئ أو يحدّث ملفاً واحداً داخل مستودع، عبر contents API (base64). */
    public static void putFile(String token, String fullRepo, String path, byte[] content, String commitMessage)
            throws IOException, GithubException {
        String sha = getFileSha(token, fullRepo, path);
        HttpURLConnection c = open("https://api.github.com/repos/" + fullRepo + "/contents/" + encodePath(path), token, "PUT");
        c.setDoOutput(true);
        try {
            JSONObject payload = new JSONObject();
            payload.put("message", commitMessage);
            payload.put("content", Base64.encodeToString(content, Base64.NO_WRAP));
            if (sha != null) payload.put("sha", sha);
            byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setFixedLengthStreamingMode(data.length);
            try (OutputStream os = c.getOutputStream()) { os.write(data); }
            bodyOrError(c);
        } catch (org.json.JSONException e) {
            throw new GithubException("تعذّر رفع الملف: " + path);
        } finally {
            c.disconnect();
        }
    }
}
