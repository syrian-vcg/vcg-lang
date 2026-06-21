package com.syrianvcg.editor;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * VcgIconSwitcher — يبدّل أيقونة التطبيق على الشاشة الرئيسية بين 4 أشكال
 * (أخضر/زيتوني، أسود، أزرق، أبيض) عبر activity-alias في AndroidManifest.
 */
public final class VcgIconSwitcher {

    private VcgIconSwitcher() {}

    public static final String[] ICONS = {"olive", "black", "blue", "white"};

    private static String aliasFor(String key) {
        switch (key) {
            case "black": return "com.syrianvcg.editor.LauncherBlack";
            case "blue":  return "com.syrianvcg.editor.LauncherBlue";
            case "white": return "com.syrianvcg.editor.LauncherWhite";
            default:      return "com.syrianvcg.editor.LauncherOlive";
        }
    }

    public static String displayName(String key) {
        switch (key) {
            case "black": return "أسود";
            case "blue":  return "أزرق";
            case "white": return "أبيض";
            default:      return "أخضر زيتوني (الافتراضي)";
        }
    }

    /** يفعّل الأيقونة المختارة ويعطّل البقية. آمن للاستدعاء عدة مرات. */
    public static void applyIcon(Context ctx, String key) {
        PackageManager pm = ctx.getPackageManager();
        for (String icon : ICONS) {
            ComponentName cn = new ComponentName(ctx, aliasFor(icon));
            int desired = icon.equals(key)
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            try {
                pm.setComponentEnabledSetting(cn, desired, PackageManager.DONT_KILL_APP);
            } catch (Exception ignored) {
                // إن لم يكن الـ alias موجوداً لأي سبب، نتجاهل بأمان بدل تعطيل التطبيق.
            }
        }
    }
}
