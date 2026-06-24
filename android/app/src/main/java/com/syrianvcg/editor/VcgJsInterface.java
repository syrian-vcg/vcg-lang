package com.syrianvcg.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

/**
 * VcgJsInterface — جسر بين WebView (JavaScript) والتطبيق (Java).
 *
 * ⚠️ تنبيه مهم: Android يستدعي كل دوال @JavascriptInterface من JS thread
 * الخاص بالـ WebView، وليس من UI thread الرئيسي. أي عملية تلمس الواجهة
 * (إنشاء Dialog، تحديث Views..) يجب تمريرها أولاً لـ UI thread عبر Handler،
 * وإلا يرمي Android استثناء CalledFromWrongThreadException ويُسقط التطبيق.
 */
public class VcgJsInterface {
    private final Context ctx;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TerminalListener listener;

    public interface TerminalListener {
        void onLog(String line);
        void onError(String message);
        void onSuccess(int outputCount);
    }

    VcgJsInterface(Context ctx) { this.ctx = ctx; }
    VcgJsInterface(Context ctx, TerminalListener listener) {
        this.ctx = ctx;
        this.listener = listener;
    }

    public void setListener(TerminalListener l) { this.listener = l; }

    @JavascriptInterface
    public void showAlert(String msg) {
        mainHandler.post(() -> {
            new AlertDialog.Builder(ctx)
                .setMessage(msg)
                .setPositiveButton("موافق", null)
                .show();
        });
    }

    @JavascriptInterface
    public void log(String line) {
        mainHandler.post(() -> { if (listener != null) listener.onLog(line); });
    }

    @JavascriptInterface
    public void onRunError(String message) {
        mainHandler.post(() -> { if (listener != null) listener.onError(message); });
    }

    @JavascriptInterface
    public void onRunSuccess(String countStr) {
        int count = 0;
        try { count = Integer.parseInt(countStr); } catch (Exception ignored) {}
        final int finalCount = count;
        mainHandler.post(() -> { if (listener != null) listener.onSuccess(finalCount); });
    }

    @JavascriptInterface
    public String getVersion() { return "2.1.0"; }
}
