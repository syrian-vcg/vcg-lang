package com.syrianvcg.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.webkit.JavascriptInterface;

public class VcgJsInterface {
    private final Context ctx;
    VcgJsInterface(Context ctx) { this.ctx = ctx; }

    @JavascriptInterface
    public void showAlert(String msg) {
        new AlertDialog.Builder(ctx)
            .setMessage(msg)
            .setPositiveButton("موافق", null)
            .show();
    }

    @JavascriptInterface
    public String getVersion() { return "1.0.0"; }
}
