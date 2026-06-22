package com.syrianvcg.editor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView title = findViewById(R.id.splash_title);
        TextView subtitle = findViewById(R.id.splash_subtitle);
        TextView version = findViewById(R.id.splash_version);

        // Animate logo
        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();

        // Animate text
        title.setAlpha(0f);
        title.setTranslationY(30f);
        title.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(400).start();

        subtitle.setAlpha(0f);
        subtitle.animate().alpha(1f).setDuration(600).setStartDelay(600).start();

        version.setAlpha(0f);
        version.animate().alpha(1f).setDuration(600).setStartDelay(800).start();

        // Navigate to projects list after 2.2s
        // ⚠️ إن أغلق المستخدم الشاشة (مثلاً بالضغط على رجوع) خلال هذه الفترة،
        // كان الـ Handler يبقى مجدولاً ويستدعي startActivity على Activity
        // انتهت فعلياً، مما قد يسبب استثناءً أو تسريب سياق. نحفظ الآن مرجع
        // الـ Runnable لإلغائه في onDestroy، ونتحقق من حالة النشاط مرة أخرى
        // كحماية إضافية وقت التنفيذ الفعلي (isDestroyed متوفرة من Activity
        // مباشرة لأن minSdk لهذا المشروع هو 24).
        navigateRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            startActivity(new Intent(this, ProjectsActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };
        splashHandler.postDelayed(navigateRunnable, 2200);
    }

    @Override
    protected void onDestroy() {
        if (navigateRunnable != null) splashHandler.removeCallbacks(navigateRunnable);
        super.onDestroy();
    }
}
