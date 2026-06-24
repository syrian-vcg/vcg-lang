package com.syrianvcg.editor;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * TerminalQuickTile — اختصار في لوحة Quick Settings (الإشعارات السريعة)
 * يفتح شاشة Terminal مباشرة بضغطة واحدة.
 *
 * المستخدم يضيفه يدوياً من زر "تعديل" (القلم) في لوحة الإشعارات السريعة،
 * مثل باقي الأزرار (البلوتوث، الفلاش، إلخ). هذا سلوك معياري في Android
 * ولا يمكن لأي تطبيق إضافة Tile تلقائياً بدون موافقة المستخدم.
 */
public class TerminalQuickTile extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.terminal));
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        Intent terminal = new Intent(this, TerminalActivity.class);
        terminal.setAction("com.syrianvcg.editor.ACTION_TERMINAL");
        terminal.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // ── استخدام PendingIntent دائماً (وليس Intent مباشرة) ──
        // بما أن targetSdk = 34، فإن TileService#startActivityAndCollapse(Intent)
        // متوقفة (deprecated) وتُطلق استثناءً فوراً عند الاستدعاء على أجهزة
        // Android 14+. التوقيع المعتمد بـ PendingIntent متوافق فعلياً من
        // API 24 فما فوق، لذا نستخدمه دائماً بدون فرع شرطي على إصدار النظام.
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
            this, 0, terminal,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                | android.app.PendingIntent.FLAG_IMMUTABLE);
        startActivityAndCollapse(pi);
    }
}
