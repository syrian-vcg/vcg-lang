package com.syrianvcg.editor;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.util.Random;

/**
 * VcgNotifications — إشعارات احترافية لتطبيق Syrian VCG Editor.
 *
 * القنوات:
 *  • CHANNEL_GENERAL  — تذكيرات وتشجيع (نغمة vcg_notify)
 *  • CHANNEL_SUCCESS  — نجاح تشغيل/رفع (نغمة vcg_notify)
 *  • CHANNEL_SILENT   — صامتة للإشعارات الخلفية
 *
 * أيقونة الإشعار: ic_notification (chevron + VCG بأبيض/شفاف)
 * نغمة مخصصة: raw/vcg_notify.wav (مولّدة برمجياً)
 */
public final class VcgNotifications {

    private VcgNotifications() {}

    // ── معرّفات القنوات ────────────────────────────────────────────────────
    public static final String CHANNEL_GENERAL = "vcg_general";
    public static final String CHANNEL_SUCCESS  = "vcg_success";
    public static final String CHANNEL_SILENT   = "vcg_silent";

    /** للتوافق مع الكود القديم الذي يستخدم CHANNEL_ID */
    public static final String CHANNEL_ID = CHANNEL_GENERAL;

    public static final int REQ_NOTIFICATIONS = 9001;

    // ── رسائل تحفيزية ─────────────────────────────────────────────────────
    private static final String[] READY_PROMPTS = {
        "يوم جديد، كود جديد ✨\nهيا نكمل ما بدأناه في VCG!",
        "لغة VCG تنتظرك 🚀\nمشاريعك محفوظة وجاهزة للإكمال.",
        "خطوة واحدة يومياً تبني لغات برمجة 💪\nأنت على الطريق الصحيح!",
        "المبرمج الحقيقي لا يتوقف 👨‍💻\nتعال نكتب سطراً واحداً على الأقل اليوم.",
        "إبداعك في انتظارك 🎯\nافتح مشروعك وفاجئ نفسك بما ستنجزه."
    };

    // ── ألوان الإشعارات ────────────────────────────────────────────────────
    private static final int COLOR_GREEN  = 0xFF4DC95A;   // أخضر VCG
    private static final int COLOR_GOLD   = 0xFFFFD700;   // ذهبي للنجاح

    // ══════════════════════════════════════════════════════════════════════
    //  إنشاء القنوات
    // ══════════════════════════════════════════════════════════════════════

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;

        Uri soundUri = Uri.parse(
            "android.resource://" + ctx.getPackageName() + "/" + R.raw.vcg_notify);

        AudioAttributes audioAttr = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        // قناة عامة — تذكيرات وتشجيع
        if (nm.getNotificationChannel(CHANNEL_GENERAL) == null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_GENERAL,
                "تذكيرات VCG",
                NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("تشجيع ومتابعة مشاريع لغة VCG");
            ch.setSound(soundUri, audioAttr);
            ch.enableLights(true);
            ch.setLightColor(COLOR_GREEN);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 80, 60, 80, 60, 120});
            nm.createNotificationChannel(ch);
        }

        // قناة النجاح — تشغيل/رفع ناجح
        if (nm.getNotificationChannel(CHANNEL_SUCCESS) == null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_SUCCESS,
                "إنجازات VCG",
                NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("إشعارات النجاح: تشغيل ناجح، رفع ناجح، وغيرها");
            ch.setSound(soundUri, audioAttr);
            ch.enableLights(true);
            ch.setLightColor(COLOR_GOLD);
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 60, 40, 60, 40, 60, 40, 200});
            nm.createNotificationChannel(ch);
        }

        // قناة صامتة — إشعارات خلفية بلا صوت
        if (nm.getNotificationChannel(CHANNEL_SILENT) == null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_SILENT,
                "إشعارات VCG الصامتة",
                NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("إشعارات خلفية بلا صوت أو اهتزاز");
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  صلاحيات
    // ══════════════════════════════════════════════════════════════════════

    public static boolean hasPermission(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (!hasPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  المُولِّد الأساسي
    // ══════════════════════════════════════════════════════════════════════

    /**
     * يبني ويرسل إشعاراً.
     *
     * @param ctx       السياق
     * @param id        رقم الإشعار (لتجنب التعارض)
     * @param channelId معرّف القناة (CHANNEL_GENERAL / CHANNEL_SUCCESS / CHANNEL_SILENT)
     * @param title     عنوان الإشعار
     * @param text      نص الإشعار المختصر
     * @param bigText   نص موسّع (BigTextStyle) — يُعرض عند توسيع الإشعار
     * @param emoji     إيموجي يُضاف قبل العنوان (أو "" للتجاهل)
     * @param color     لون accent (0 لاستخدام الأخضر الافتراضي)
     * @param openClass Activity يُفتح عند الضغط
     */
    private static void send(Context ctx, int id, String channelId,
                             String title, String text, String bigText,
                             int color, Class<?> openClass) {
        VcgSettings settings = new VcgSettings(ctx);
        if (!settings.getNotificationsEnabled()) return;
        if (!hasPermission(ctx)) return;

        createChannel(ctx);

        Intent open = new Intent(ctx, openClass);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // FLAG_IMMUTABLE إلزامي على Android 12+ (API 31+) لكل PendingIntent لا يحتاج تعديلاً.
        // نحدده دائماً لأن minSdk=24 فمضمون أن Build.VERSION.SDK_INT >= 31
        // لأي جهاز سيشغّل targetSdk=34.
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int accentColor = (color != 0) ? color : COLOR_GREEN;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accentColor)
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(bigText != null && !bigText.isEmpty() ? bigText : text)
                .setBigContentTitle(title))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(CHANNEL_SUCCESS.equals(channelId)
                ? NotificationCompat.PRIORITY_HIGH
                : NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        // صوت مخصص للإصدارات القديمة (أسفل Oreo)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse(
                "android.resource://" + ctx.getPackageName() + "/" + R.raw.vcg_notify);
            builder.setSound(soundUri);
            builder.setVibrate(new long[]{0, 80, 60, 80});
        }

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
        } catch (SecurityException ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════
    //  الإشعارات المُسمّاة
    // ══════════════════════════════════════════════════════════════════════

    /** إشعار تشجيعي عام "هل أنت مستعد" */
    public static void notifyReadyToCode(Context ctx) {
        String msg = randomReadyPrompt();
        send(ctx, 1, CHANNEL_GENERAL,
            "⚡ Syrian VCG Editor",
            "وقت الكود! افتح التطبيق ▸",
            msg,
            COLOR_GREEN, ProjectsActivity.class);
    }

    /** إشعار إنشاء مشروع جديد */
    public static void notifyProjectCreated(Context ctx, String projectName) {
        send(ctx, 2, CHANNEL_SUCCESS,
            "مشروع جديد ✓",
            "\"" + projectName + "\" جاهز — ابدأ كودك الأول!",
            "تم إنشاء مشروع «" + projectName + "» بنجاح 🎉\n"
            + "هيا نكتب أول سطر بلغة VCG — كل رحلة تبدأ بخطوة.",
            COLOR_GREEN, ProjectsActivity.class);
    }

    /** إشعار نجاح التشغيل */
    public static void notifyRunSuccess(Context ctx, String filename) {
        send(ctx, 3, CHANNEL_SUCCESS,
            "✅ تم التشغيل بنجاح",
            filename + " — يعمل بدون أخطاء!",
            "الملف «" + filename + "» شُغِّل بنجاح كامل ✅\n"
            + "بلغة VCG كتبت كوداً يعمل — عمل رائع!",
            COLOR_GOLD, ProjectsActivity.class);
    }

    /** إشعار رفع GitHub ناجح */
    public static void notifyGitHubPushed(Context ctx, String projectName, String repo) {
        send(ctx, 4, CHANNEL_SUCCESS,
            "GitHub ✓ تم الرفع",
            projectName + " → " + repo,
            "تم رفع مشروع «" + projectName + "» بنجاح إلى GitHub 🚀\n"
            + "الريبو: " + repo + "\n"
            + "شاركه مع العالم!",
            COLOR_GREEN, ProjectsActivity.class);
    }

    /** إشعار عام (للتوافق مع الكود القديم) */
    public static void notify(Context ctx, int id, String title, String text) {
        send(ctx, id, CHANNEL_GENERAL, title, text, text, COLOR_GREEN, ProjectsActivity.class);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  أدوات مساعدة
    // ══════════════════════════════════════════════════════════════════════

    public static String randomReadyPrompt() {
        return READY_PROMPTS[new Random().nextInt(READY_PROMPTS.length)];
    }
}
