package com.syrianvcg.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.webkit.JavascriptInterface;

public class VcgJsInterface {
    private final Context ctx;
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
        new AlertDialog.Builder(ctx)
            .setMessage(msg)
            .setPositiveButton("موافق", null)
            .show();
    }

    @JavascriptInterface
    public void log(String line) {
        if (listener != null) listener.onLog(line);
    }

    @JavascriptInterface
    public void onRunError(String message) {
        if (listener != null) listener.onError(message);
    }

    @JavascriptInterface
    public void onRunSuccess(String countStr) {
        int count = 0;
        try { count = Integer.parseInt(countStr); } catch (Exception ignored) {}
        if (listener != null) listener.onSuccess(count);
    }

    @JavascriptInterface
    public String getVersion() { return "2.1.0"; }
}
