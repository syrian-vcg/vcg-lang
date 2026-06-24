package com.syrianvcg.editor;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import androidx.core.content.ContextCompat;
import java.util.List;

/**
 * EditorWidgetProvider — تطبيق مصغّر (App Widget) للشاشة الرئيسية يعرض
 * آخر مشروع تم تعديله، ويوفّر اختصارين سريعين: "مشروع جديد" و"Terminal".
 *
 * يقرأ البيانات مباشرة عبر VcgStorage (نفس مصدر بيانات ProjectsActivity)
 * بحيث يبقى التطبيق المصغّر متزامناً مع حالة المشاريع دون حاجة لخدمة إضافية.
 */
public class EditorWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.syrianvcg.editor.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(
                new android.content.ComponentName(context, EditorWidgetProvider.class));
            onUpdate(context, mgr, ids);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_editor);

        VcgStorage storage = new VcgStorage(context);
        List<VcgProject> projects = storage.getAllProjects();

        if (projects.isEmpty()) {
            views.setTextViewText(R.id.widget_project_label, context.getString(R.string.widget_no_projects));
            views.setTextViewText(R.id.widget_project_name, context.getString(R.string.shortcut_new_project_short));
            views.setInt(R.id.widget_project_dot, "setColorFilter",
                ContextCompat.getColor(context, R.color.text_muted));
        } else {
            VcgProject last = projects.get(0);
            views.setTextViewText(R.id.widget_project_label, context.getString(R.string.widget_last_project));
            views.setTextViewText(R.id.widget_project_name, last.getName());
            int dotColor;
            try {
                dotColor = android.graphics.Color.parseColor(last.getColorTag());
            } catch (Exception e) {
                dotColor = ContextCompat.getColor(context, R.color.accent_green);
            }
            views.setInt(R.id.widget_project_dot, "setColorFilter", dotColor);
        }

        // ── فتح آخر مشروع (أو شاشة المشاريع إن لم يوجد) عند الضغط على البطاقة ──
        Intent openLast = new Intent(context, ProjectsActivity.class);
        openLast.setAction("com.syrianvcg.editor.ACTION_LAST_PROJECT");
        openLast.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent lastPI = PendingIntent.getActivity(
            context, widgetId, openLast,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_last_project, lastPI);

        // ── مشروع جديد ──────────────────────────────────────────
        Intent newProject = new Intent(context, ProjectsActivity.class);
        newProject.setAction("com.syrianvcg.editor.ACTION_NEW_PROJECT");
        newProject.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent newPI = PendingIntent.getActivity(
            context, widgetId + 1000, newProject,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_new, newPI);

        // ── Terminal ────────────────────────────────────────────
        Intent terminal = new Intent(context, TerminalActivity.class);
        terminal.setAction("com.syrianvcg.editor.ACTION_TERMINAL");
        terminal.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent terminalPI = PendingIntent.getActivity(
            context, widgetId + 2000, terminal,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_terminal, terminalPI);

        // ── الإعدادات ───────────────────────────────────────────
        Intent settings = new Intent(context, SettingsActivity.class);
        settings.setAction("com.syrianvcg.editor.ACTION_SETTINGS");
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent settingsPI = PendingIntent.getActivity(
            context, widgetId + 4000, settings,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_settings, settingsPI);

        // ── فتح التطبيق عند الضغط على شريط العنوان (الشعار/الاسم) ──
        Intent openApp = new Intent(context, SplashActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPI = PendingIntent.getActivity(
            context, widgetId + 3000, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_header, openAppPI);

        appWidgetManager.updateAppWidget(widgetId, views);
    }
}
