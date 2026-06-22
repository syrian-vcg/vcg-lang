package com.syrianvcg.editor;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

/**
 * VcgAds — غلاف بسيط حول Google Mobile Ads (AdMob) لعرض إعلان "بيني مقابل
 * مكافأة" (Rewarded Interstitial) يمنح المستخدم عملات داخل المحرر عند
 * مشاهدته كاملاً.
 *
 * ⚠️ هذه الوحدة الإعلانية أُنشئت في AdMob كنوع "Rewarded Interstitial"
 * تحديداً (هذا ما تؤكده صفحة الإنشاء التي توجّه لدليل "تنفيذ الإعلانات
 * البينية مقابل مكافأة")، وهو تنسيق مختلف عن "Rewarded" العادي رغم
 * تشابه الاسم: يُحمَّل ويُعرض عبر فئة RewardedInterstitialAd في الحزمة
 * com.google.android.gms.ads.rewardedinterstitial، وليس عبر RewardedAd.
 * استخدام الفئة الخاطئة مع رقم تعريف وحدة من نوع مختلف يؤدي غالباً إلى
 * فشل تحميل الإعلان (No Fill) لأن خادم AdMob يطابق التنسيق بدقة.
 *
 * رقم تعريف التطبيق (AdMob App ID) موضوع في AndroidManifest.xml كـ meta-data،
 * ورقم تعريف الوحدة الإعلانية هنا أدناه (AD_UNIT_ID).
 */
public final class VcgAds {

    private static final String TAG = "VcgAds";

    /**
     * أرقام تعريف الوحدات الإعلانية الحقيقية لا تُكتب هنا مباشرة — تُقرأ من
     * BuildConfig الذي يأخذها من local.properties (غير مرفوع على GitHub).
     * إن لم تُضبط، تُستخدم أرقام AdMob الرسمية للاختبار تلقائياً، فيبقى
     * المشروع قابلاً للبناء بأمان لأي شخص يستنسخه من المستودع العام.
     */
    public static final String REWARDED_AD_UNIT_ID = BuildConfig.ADMOB_REWARDED_UNIT_ID;
    public static final String INTERSTITIAL_AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID;

    private static boolean initialized = false;

    private RewardedInterstitialAd rewardedAd;
    private boolean isLoading = false;

    private com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd;
    private boolean isInterstitialLoading = false;

    public interface RewardListener {
        /** يُستدعى فقط إذا شاهد المستخدم الإعلان كاملاً واستحق المكافأة. */
        void onRewardEarned();
        /** يُستدعى عند فشل التحميل/العرض أو إغلاق الإعلان دون اكتماله. */
        default void onAdUnavailable(String reason) {}
    }

    /** يُستدعى مرة واحدة (مثلاً من Application أو أول Activity) لتهيئة SDK. */
    public static void init(Activity activity) {
        if (initialized) return;
        initialized = true;
        MobileAds.initialize(activity, status -> Log.d(TAG, "AdMob initialized"));
    }

    public void preload(Activity activity) {
        if (rewardedAd != null || isLoading) return;
        isLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(activity, REWARDED_AD_UNIT_ID, request, new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                rewardedAd = ad;
                isLoading = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                isLoading = false;
                Log.w(TAG, "Rewarded interstitial ad failed to load: " + loadAdError.getMessage());
            }
        });
    }

    public boolean isReady() {
        return rewardedAd != null;
    }

    public void show(Activity activity, RewardListener listener) {
        if (rewardedAd == null) {
            listener.onAdUnavailable("الإعلان غير جاهز بعد، حاول بعد لحظات");
            preload(activity);
            return;
        }

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                preload(activity);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                listener.onAdUnavailable(adError.getMessage());
                preload(activity);
            }
        });

        rewardedAd.show(activity, rewardItem -> listener.onRewardEarned());
    }

    // ═══════════════ إعلان بيني (Interstitial) — الوحدة الإعلانية الثانية ═══════════════

    public void preloadInterstitial(Activity activity) {
        if (interstitialAd != null || isInterstitialLoading) return;
        isInterstitialLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        com.google.android.gms.ads.interstitial.InterstitialAd.load(
            activity, INTERSTITIAL_AD_UNIT_ID, request,
            new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd ad) {
                    interstitialAd = ad;
                    isInterstitialLoading = false;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    interstitialAd = null;
                    isInterstitialLoading = false;
                    Log.w(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                }
            });
    }

    public boolean isInterstitialReady() {
        return interstitialAd != null;
    }

    /** يعرض الإعلان البيني إن كان جاهزاً، وإلا يبدأ تحميله بصمت للمرة القادمة. */
    public void showInterstitial(Activity activity, Runnable onClosed) {
        if (interstitialAd == null) {
            preloadInterstitial(activity);
            if (onClosed != null) onClosed.run();
            return;
        }

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                preloadInterstitial(activity);
                if (onClosed != null) onClosed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                interstitialAd = null;
                preloadInterstitial(activity);
                if (onClosed != null) onClosed.run();
            }
        });
        interstitialAd.show(activity);
    }
}
