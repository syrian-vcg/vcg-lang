package com.syrianvcg.editor;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * SplashActivity — شاشة بداية محسّنة تعرض الأيقونة الرسمية الجديدة لـ VCG
 * (chevrons + نص VCG على خلفية سوداء) مع انيميشن متدرّج:
 *
 *  1. الأيقونة تظهر بتكبير من 0.3 مع bounce خفيف (overshoot) وfade-in
 *  2. توهّج أخضر ينبض خلفها
 *  3. بريق يعبر الأيقونة بعد اكتمال ظهورها
 *  4. العنوان والوصف والإصدار يصعدان الواحد تلو الآخر
 *  5. نقاط التحميل تتموّج بلا توقف حتى الانتقال
 *
 * الخلفية ثابتة سوداء (#000000) لتناسب ألوان الأيقونة الجديدة
 * (أسود + أخضر) بغض النظر عن ثيم التطبيق المختار.
 */
public class SplashActivity extends AppCompatActivity {

    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;
    private final List<Animator> activeAnimators = new ArrayList<>();

    private static final int GREEN_ACCENT = 0xFF4DC95A;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // لا نستدعي VcgThemeHelper هنا — الخلفية مقيّدة بالأسود لتناسب الأيقونة
        setContentView(R.layout.activity_splash);

        // ── مراجع العناصر ─────────────────────────────────────────────────
        ImageView logo      = findViewById(R.id.splash_logo);
        ImageView glow      = findViewById(R.id.splash_glow);
        ImageView shine     = findViewById(R.id.layer_shine);
        TextView  title     = findViewById(R.id.splash_title);
        TextView  subtitle  = findViewById(R.id.splash_subtitle);
        TextView  version   = findViewById(R.id.splash_version);
        View      loadingDots = findViewById(R.id.loading_dots);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        // لوّن نقاط التحميل باللون الأخضر
        dot1.getBackground().mutate().setTint(GREEN_ACCENT);
        dot2.getBackground().mutate().setTint(GREEN_ACCENT);
        dot3.getBackground().mutate().setTint(GREEN_ACCENT);
        glow.setColorFilter(GREEN_ACCENT);

        // ── 1. الأيقونة: تكبر من 0.3 → 1 مع bounce + fade-in ────────────
        // تبدأ صغيرة من الأسفل قليلاً
        logo.setScaleX(0.3f);
        logo.setScaleY(0.3f);
        logo.setAlpha(0f);
        logo.setTranslationY(30f);

        ObjectAnimator logoAnim = ObjectAnimator.ofPropertyValuesHolder(logo,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.3f, 1.08f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.3f, 1.08f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 30f, -6f, 0f));
        logoAnim.setDuration(700);
        logoAnim.setStartDelay(80);
        logoAnim.setInterpolator(new OvershootInterpolator(1.5f));
        play(logoAnim);

        // ── 2. توهّج أخضر ينبض خلف الأيقونة ────────────────────────────
        glow.setAlpha(0f);
        ObjectAnimator glowIn = ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 0.7f);
        glowIn.setDuration(500);
        glowIn.setStartDelay(120);
        play(glowIn);

        splashHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(glow,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0.35f));
            pulse.setDuration(1400);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            play(pulse);
        }, 620);

        // ── 3. بريق يعبر الأيقونة ────────────────────────────────────────
        float shineSweep = dp(180);
        shine.setTranslationX(-shineSweep);
        shine.setAlpha(0f);
        splashHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            AnimatorSet shineSet = new AnimatorSet();
            ObjectAnimator move = ObjectAnimator.ofFloat(shine, View.TRANSLATION_X, -shineSweep, shineSweep);
            move.setDuration(700);
            ObjectAnimator fade = ObjectAnimator.ofFloat(shine, View.ALPHA, 0f, 0.9f, 0.9f, 0f);
            fade.setDuration(700);
            shineSet.playTogether(move, fade);
            shineSet.setInterpolator(new DecelerateInterpolator());
            play(shineSet);
        }, 900);

        // ── 4. العنوان يصعد ───────────────────────────────────────────────
        title.setAlpha(0f);
        title.setTranslationY(28f);
        ObjectAnimator titleIn = ObjectAnimator.ofPropertyValuesHolder(title,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 28f, 0f));
        titleIn.setDuration(500);
        titleIn.setStartDelay(820);
        titleIn.setInterpolator(new AccelerateDecelerateInterpolator());
        play(titleIn);

        // ── 5. الوصف ─────────────────────────────────────────────────────
        subtitle.setAlpha(0f);
        ObjectAnimator subtitleIn = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f);
        subtitleIn.setDuration(420);
        subtitleIn.setStartDelay(960);
        play(subtitleIn);

        // ── 6. رقم الإصدار ───────────────────────────────────────────────
        version.setAlpha(0f);
        ObjectAnimator versionIn = ObjectAnimator.ofFloat(version, View.ALPHA, 0f, 1f);
        versionIn.setDuration(380);
        versionIn.setStartDelay(1060);
        play(versionIn);

        // ── 7. نقاط التحميل تظهر ثم تتموّج ─────────────────────────────
        ObjectAnimator dotsIn = ObjectAnimator.ofFloat(loadingDots, View.ALPHA, 0f, 1f);
        dotsIn.setDuration(350);
        dotsIn.setStartDelay(1200);
        play(dotsIn);
        splashHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            startDotWave(dot1, 0);
            startDotWave(dot2, 130);
            startDotWave(dot3, 260);
        }, 1200);

        // ── الانتقال إلى قائمة المشاريع ──────────────────────────────────
        // نحرص على تمرير action الـ shortcut (إن وُجد) عبر Intent Stack:
        // عندما يضغط المستخدم على shortcut مثل "مشروع جديد" أو "آخر مشروع"،
        // يصل الـ ACTION إلى SplashActivity عبر سلسلة Intents في shortcuts.xml،
        // لذا نعيد تمريره يدوياً إلى ProjectsActivity لضمان معالجته حتى بعد Splash.
        navigateRunnable = () -> {
            if (isFinishing() || isDestroyed()) return;
            Intent dest = new Intent(this, ProjectsActivity.class);
            String incomingAction = getIntent() != null ? getIntent().getAction() : null;
            if (incomingAction != null
                    && (incomingAction.equals("com.syrianvcg.editor.ACTION_NEW_PROJECT")
                     || incomingAction.equals("com.syrianvcg.editor.ACTION_LAST_PROJECT"))) {
                dest.setAction(incomingAction);
            }
            startActivity(dest);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };
        splashHandler.postDelayed(navigateRunnable, 2700);
    }

    private void startDotWave(View dot, long startDelay) {
        ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, -8f, 0f);
        bounce.setDuration(600);
        bounce.setStartDelay(startDelay);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
        play(bounce);
    }

    private void play(Animator animator) {
        activeAnimators.add(animator);
        animator.start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        if (navigateRunnable != null) splashHandler.removeCallbacks(navigateRunnable);
        splashHandler.removeCallbacksAndMessages(null);
        for (Animator a : activeAnimators) a.cancel();
        activeAnimators.clear();
        super.onDestroy();
    }
}
