package com.syrianvcg.editor;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * SplashActivity — شاشة بداية مبنية بالكامل بتحريك أصلي (بدون أي مكتبة
 * خارجية كـ Lottie): الشعار يتركّب أمام الناظر طبقة فوق طبقة بدل أن يظهر
 * دفعة واحدة — مربع الخلفية أولاً، ثم النجوم الثلاث، ثم حروف VCG، ثم
 * "بريق" يعبر الشعار، مع توهّج خلفي ينبض باستمرار ونقاط تحميل متموّجة.
 *
 * كل الألوان الديناميكية (التوهّج والنقاط) تُلوَّن وقت التشغيل من سمة
 * الواجهة الحالية (?attr/colorAccentPrimary) عبر TypedValue، بنفس مبدأ
 * بقية الشاشات في التطبيق: لا ألوان ثابتة تكسر تبديل السمات الأربع.
 */
public class SplashActivity extends AppCompatActivity {

    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private Runnable navigateRunnable;
    private final List<Animator> activeAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VcgThemeHelper.apply(this);
        setContentView(R.layout.activity_splash);

        int accentColor = resolveThemeColor(com.syrianvcg.editor.R.attr.colorAccentPrimary);

        ImageView glow = findViewById(R.id.splash_glow);
        ImageView square = findViewById(R.id.layer_square);
        ImageView starSmall = findViewById(R.id.layer_star_small);
        ImageView starMedium = findViewById(R.id.layer_star_medium);
        ImageView starLarge = findViewById(R.id.layer_star_large);
        ImageView letterV = findViewById(R.id.layer_letter_v);
        ImageView letterC = findViewById(R.id.layer_letter_c);
        ImageView letterG = findViewById(R.id.layer_letter_g);
        ImageView shine = findViewById(R.id.layer_shine);
        TextView title = findViewById(R.id.splash_title);
        TextView subtitle = findViewById(R.id.splash_subtitle);
        TextView version = findViewById(R.id.splash_version);
        View loadingDots = findViewById(R.id.loading_dots);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        glow.setColorFilter(accentColor);
        dot1.getBackground().mutate().setTint(accentColor);
        dot2.getBackground().mutate().setTint(accentColor);
        dot3.getBackground().mutate().setTint(accentColor);

        // ── 1) مربع الخلفية: يكبر من 0.4 إلى 1 بانتفاضة خفيفة (overshoot) ──
        prepareForEntrance(square, 0.4f, 24f, false);
        play(entrance(square, 0, 360, new OvershootInterpolator(1.6f)));

        // ── 2) النجوم الثلاث تظهر بالتتابع، كل واحدة بدوران وتكبير خفيفين ──
        prepareForStarEntrance(starSmall);
        prepareForStarEntrance(starMedium);
        prepareForStarEntrance(starLarge);
        play(starEntrance(starSmall, 180));
        play(starEntrance(starMedium, 280));
        play(starEntrance(starLarge, 380));

        // ── 3) حروف VCG تنزلق للأعلى وتتلاشى ظهوراً، حرفاً بعد حرف ──
        prepareForEntrance(letterV, 1f, 22f, true);
        prepareForEntrance(letterC, 1f, 22f, true);
        prepareForEntrance(letterG, 1f, 22f, true);
        play(slideUpEntrance(letterV, 560));
        play(slideUpEntrance(letterC, 630));
        play(slideUpEntrance(letterG, 700));

        // ── 4) توهّج خلفي ينبض بهدوء طوال مدة الشاشة (يبدأ بعد ظهور المربع) ──
        glow.setAlpha(0f);
        ObjectAnimator glowIn = ObjectAnimator.ofFloat(glow, View.ALPHA, 0f, 0.85f);
        glowIn.setDuration(420);
        glowIn.setStartDelay(60);
        play(glowIn);
        splashHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(glow,
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.14f),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.14f),
                android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0.85f, 0.55f));
            pulse.setDuration(1300);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            play(pulse);
        }, 480);

        // ── 5) بريق يعبر الشعار بعد اكتمال تركيبه ──
        float shineSweep = dp(170);
        shine.setTranslationX(-shineSweep);
        splashHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            AnimatorSet shineSet = new AnimatorSet();
            ObjectAnimator move = ObjectAnimator.ofFloat(shine, View.TRANSLATION_X, -shineSweep, shineSweep);
            move.setDuration(650);
            ObjectAnimator fade = ObjectAnimator.ofFloat(shine, View.ALPHA, 0f, 1f, 1f, 0f);
            fade.setDuration(650);
            shineSet.playTogether(move, fade);
            shineSet.setInterpolator(new AccelerateDecelerateInterpolator());
            play(shineSet);
        }, 950);

        // ── 6) عنوان التطبيق ووصفه ورقم إصداره ──
        title.setAlpha(0f);
        title.setTranslationY(30f);
        ObjectAnimator titleIn = ObjectAnimator.ofPropertyValuesHolder(title,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 30f, 0f));
        titleIn.setDuration(560);
        titleIn.setStartDelay(780);
        titleIn.setInterpolator(new AccelerateDecelerateInterpolator());
        play(titleIn);

        subtitle.setAlpha(0f);
        ObjectAnimator subtitleIn = ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f);
        subtitleIn.setDuration(520);
        subtitleIn.setStartDelay(920);
        play(subtitleIn);

        version.setAlpha(0f);
        ObjectAnimator versionIn = ObjectAnimator.ofFloat(version, View.ALPHA, 0f, 1f);
        versionIn.setDuration(520);
        versionIn.setStartDelay(1040);
        play(versionIn);

        // ── 7) نقاط التحميل: تظهر ثم تتموّج بلا توقف حتى الانتقال ──
        ObjectAnimator dotsIn = ObjectAnimator.ofFloat(loadingDots, View.ALPHA, 0f, 1f);
        dotsIn.setDuration(380);
        dotsIn.setStartDelay(1200);
        play(dotsIn);
        splashHandler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            startDotWave(dot1, 0);
            startDotWave(dot2, 140);
            startDotWave(dot3, 280);
        }, 1200);

        // ── الانتقال إلى قائمة المشاريع بعد اكتمال كل التحريك ──
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
        splashHandler.postDelayed(navigateRunnable, 2600);
    }

    /** يهيّئ عرضاً عاماً (حروف/مربع) ليبدأ من حالة مخفية قبل تشغيل دخوله. */
    private void prepareForEntrance(View v, float fromScale, float fromTranslationY, boolean isSlide) {
        v.setAlpha(0f);
        if (isSlide) {
            v.setTranslationY(fromTranslationY);
        } else {
            v.setScaleX(fromScale);
            v.setScaleY(fromScale);
        }
    }

    private void prepareForStarEntrance(View v) {
        v.setAlpha(0f);
        v.setScaleX(0f);
        v.setScaleY(0f);
        v.setRotation(-20f);
    }

    private Animator entrance(View v, long delay, long duration, android.view.animation.Interpolator interpolator) {
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(v,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, v.getScaleX(), 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, v.getScaleY(), 1f));
        anim.setStartDelay(delay);
        anim.setDuration(duration);
        anim.setInterpolator(interpolator);
        return anim;
    }

    private Animator starEntrance(View v, long delay) {
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(v,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1.25f, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1.25f, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.ROTATION, -20f, 0f));
        anim.setStartDelay(delay);
        anim.setDuration(420);
        anim.setInterpolator(new OvershootInterpolator(1.4f));
        return anim;
    }

    private Animator slideUpEntrance(View v, long delay) {
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(v,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 22f, 0f));
        anim.setStartDelay(delay);
        anim.setDuration(340);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        return anim;
    }

    /** نقطة تحميل تتموّج للأعلى ثم تعود، بتأخير بدء مختلف عن جاراتها لتظهر كموجة. */
    private void startDotWave(View dot, long startDelay) {
        ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, -7f, 0f);
        bounce.setDuration(620);
        bounce.setStartDelay(startDelay);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
        play(bounce);
    }

    private void play(Animator animator) {
        activeAnimators.add(animator);
        animator.start();
    }

    private int resolveThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        if (navigateRunnable != null) splashHandler.removeCallbacks(navigateRunnable);
        splashHandler.removeCallbacksAndMessages(null);
        for (Animator animator : activeAnimators) {
            animator.cancel();
        }
        activeAnimators.clear();
        super.onDestroy();
    }
}
