package com.syrianvcg.editor;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * VcgFeedback — صندوق الاقتراحات: يفتح تطبيق البريد على الجهاز برسالة
 * جاهزة موجّهة لمطوّر VCG Editor، ليكتب المستخدم اقتراحه ويرسله مباشرة.
 * لا حاجة لخادم أو مفاتيح API — فقط Intent قياسي (mailto:).
 */
public final class VcgFeedback {

    public static final String DEVELOPER_EMAIL = "majdsaadi10096@gmail.com";

    private VcgFeedback() {}

    public static void openSuggestionEmail(AppCompatActivity activity, String suggestionText) {
        String subject = "اقتراح على تطبيق VCG Editor";
        StringBuilder body = new StringBuilder();
        body.append("مرحباً فريق VCG Editor،\n\n");
        if (suggestionText != null && !suggestionText.trim().isEmpty()) {
            body.append(suggestionText.trim()).append("\n\n");
        } else {
            body.append("اقتراحي هو:\n\n\n");
        }
        body.append("—\nتم الإرسال من داخل تطبيق VCG Editor (الإصدار 2.1.0)");

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{DEVELOPER_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body.toString());

        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity,
                "لا يوجد تطبيق بريد مثبت على هذا الجهاز", Toast.LENGTH_LONG).show();
        }
    }
}
