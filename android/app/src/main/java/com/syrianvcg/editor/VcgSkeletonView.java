package com.syrianvcg.editor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * VcgSkeletonView — تحميل هيكلي (Skeleton Loading) بسيط ومتحرك
 * يُعرض بدلاً من شاشة فاضية بينما يتم بناء/تحميل المعاينة الحية.
 */
public class VcgSkeletonView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator pulse;
    private int baseColor = 0xFFE7EBE6;
    private float alphaFraction = 0.45f;

    // نسب عرض كل "سطر" هيكلي، لمحاكاة فقرات/عناصر مخرجات حقيقية
    private static final float[] LINE_WIDTHS = {0.94f, 0.62f, 0.80f, 0.40f, 0.70f, 0.55f, 0.88f, 0.50f};

    public VcgSkeletonView(Context c) { super(c); init(); }
    public VcgSkeletonView(Context c, AttributeSet a) { super(c, a); init(); }
    public VcgSkeletonView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        setDark(true);
        startPulse();
    }

    /** يطبّق لون الهيكل المناسب لسمة التطبيق الحالية (داكنة/فاتحة). */
    public void setDark(boolean dark) {
        baseColor = dark ? 0xFF2A2F32 : 0xFFE7EBE6;
        invalidate();
    }

    private void startPulse() {
        if (pulse != null) pulse.cancel();
        pulse = ValueAnimator.ofFloat(0.35f, 0.9f);
        pulse.setDuration(750);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(new LinearInterpolator());
        pulse.addUpdateListener(a -> {
            alphaFraction = (float) a.getAnimatedValue();
            invalidate();
        });
        pulse.start();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            // ⚠️ كان الشرط isStarted() دائماً true بعد pause() (لأن pause لا
            // توقف الأنيميشن فعلياً، بل تجمّد تحديثاته فقط)، فلم يكن start()
            // يُستدعى أبداً، وتبقى النبضة مجمّدة للأبد بعد أول إخفاء/إظهار.
            // resume() هي الاستدعاء الصحيح لاستئناف أنيميشن متوقف بـ pause().
            if (pulse != null) {
                if (pulse.isPaused()) pulse.resume();
                else if (!pulse.isStarted()) pulse.start();
            }
        } else if (pulse != null) {
            pulse.pause();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        if (w <= 0) return;
        float density = getResources().getDisplayMetrics().density;
        float padding = 16f * density;
        float lineH = 13f * density;
        float gap = 14f * density;
        float radius = 6f * density;

        barPaint.setColor(baseColor);
        barPaint.setAlpha((int) (255 * alphaFraction));

        float y = padding;
        int i = 0;
        RectF r = new RectF();
        while (y + lineH <= getHeight() - padding && i < LINE_WIDTHS.length) {
            float lineW = (w - padding * 2) * LINE_WIDTHS[i % LINE_WIDTHS.length];
            r.set(padding, y, padding + lineW, y + lineH);
            canvas.drawRoundRect(r, radius, radius, barPaint);
            y += lineH + gap;
            i++;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulse != null) pulse.cancel();
    }
}
