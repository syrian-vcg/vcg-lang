package com.syrianvcg.editor;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import java.util.Random;

/**
 * VcgNotifications — صلاحيات + قناة + رسائل تشجيعية ("هل أنت مستعد؟"، "هيا جهّز نفسك!"..)
 */
public final class VcgNotifications {

    private VcgNotifications() {}

    public static final String CHANNEL_ID = "vcg_reminders";
    public static final int REQ_NOTIFICATIONS = 9001;

    private static final String[] READY_PROMPTS = {
        "هل أنت مستعد؟ 🚀 هيا نكتب كودًا رائعًا اليوم!",
        "جاهز للبرمجة؟ مشروعك بانتظارك.",
        "هيا جهّز نفسك! خطوة صغيرة كل يوم تبني لغة VCG.",
        "وقت الإبداع! هل تريد أن تكمل ما بدأته؟",
        "أكمل من حيث توقفت 👨‍💻 — مشاريعك محفوظة بانتظارك."
    };

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "تذكيرات وتشجيع VCG", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("تذكيرات لطيفة لمتابعة البرمجة بلغة VCG");
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static boolean hasPermission(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    /** يطلب صلاحية الإشعارات إن لزم (Android 13+ فقط). أمن للاستدعاء في كل مرة. */
    public static void requestPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (!hasPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    public static String randomReadyPrompt() {
        return READY_PROMPTS[new Random().nextInt(READY_PROMPTS.length)];
    }

    /** يعرض إشعار نظام حقيقي (إن كانت الصلاحية والإعداد مفعّلين). */
    public static void notify(Context ctx, int id, String title, String text) {
        VcgSettings settings = new VcgSettings(ctx);
        if (!settings.getNotificationsEnabled()) return;
        if (!hasPermission(ctx)) return;

        createChannel(ctx);

        Intent open = new Intent(ctx, ProjectsActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build());
        } catch (SecurityException ignored) {
            // الصلاحية رُفضت بين الفحص والاستدعاء — تجاهل بأمان.
        }
    }

    public static void notifyReadyToCode(Context ctx) {
        notify(ctx, 1, "Syrian VCG Editor", randomReadyPrompt());
    }

    public static void notifyProjectCreated(Context ctx, String projectName) {
        notify(ctx, 2, "مشروع جديد ✓", "تم إنشاء \"" + projectName + "\" — هيا نكتب أول سطر كود!");
    }

    public static void notifyRunSuccess(Context ctx, String filename) {
        notify(ctx, 3, "تم التشغيل بنجاح ✓", filename + " يعمل بدون أخطاء. عمل رائع!");
    }
}
