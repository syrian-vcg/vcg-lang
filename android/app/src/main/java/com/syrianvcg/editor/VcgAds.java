package com.syrianvcg.editor;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

/**
 * VcgAds — غلاف حول Google Mobile Ads (AdMob) يدير أربع وحدات إعلانية:
 *
 *  1. Rewarded Interstitial (REWARDED_AD_UNIT_ID)      — يمنح عملات عند المشاهدة الكاملة.
 *  2. Interstitial          (INTERSTITIAL_AD_UNIT_ID)  — إعلان بيني عادي.
 *  3. Banner                (BANNER_UNIT_ID)            — شريط بانر في أسفل الشاشة.
 *  4. Rewarded Interstitial "coi" (REWARDED_INTERSTITIAL_UNIT_ID) — وحدة مُجزية ثانية.
 *
 * أرقام التعريف الحقيقية تُقرأ من BuildConfig (مصدرها local.properties غير المرفوع).
 * عند غيابها تُستخدم أرقام AdMob الرسمية للاختبار حتى يظل المشروع قابلاً للبناء.
 */
public final class VcgAds {

    private static final String TAG = "VcgAds";

    // ── أرقام الوحدات الإعلانية ────────────────────────────────────────────
    
    /** إعلان بيني مقابل مكافأة — "set" (لمنح العملات في SettingsActivity) */
    public static final String REWARDED_AD_UNIT_ID = 
            "ca-app-pub-1525040025806904/5325469129";

    /** إعلان بيني عادي — "john" */
    public static final String INTERSTITIAL_AD_UNIT_ID = 
            "ca-app-pub-1525040025806904/4027633665";

    /** إعلان شاشة فتح التطبيق "oi" (يُستخدم كبانر) */
    public static final String BANNER_UNIT_ID = 
            "ca-app-pub-1525040025806904/3150465990";

    /** إعلان بيني مقابل مكافأة ثاني — "coi" */
    public static final String REWARDED_INTERSTITIAL_UNIT_ID = 
            "ca-app-pub-1525040025806904/2699305783";


    // ── حالة SDK ───────────────────────────────────────────────────────────
    private static boolean initialized = false;

    // ── Rewarded Interstitial (الأولى — لمنح العملات) ─────────────────────
    // ملاحظة: static عمداً — كل Activity كانت تنشئ VcgAds() جديدة فتُصفَّر
    // حالة الإعلان المحمَّل مسبقاً وتظهر رسالة "غير جاهز" حتى لو كان الإعلان
    // قد اكتمل تحميله بالفعل في شاشة سابقة. مشاركة الحالة عبر static تحلّ هذا.
    private static RewardedInterstitialAd rewardedAd;
    private static boolean isLoading = false;

    // ── Interstitial ───────────────────────────────────────────────────────
    private static com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd;
    private static boolean isInterstitialLoading = false;

    // ── Rewarded Interstitial الثانية "coi" ────────────────────────────────
    private static RewardedInterstitialAd rewardedInterstitialAd;
    private static boolean isRewardedInterstitialLoading = false;

    // ── Banner ─────────────────────────────────────────────────────────────
    private AdView bannerAdView;

    // ══════════════════════════════════════════════════════════════════════
    //  Listener
    // ══════════════════════════════════════════════════════════════════════

    public interface RewardListener {
        /** يُستدعى فقط إذا شاهد المستخدم الإعلان كاملاً واستحق المكافأة. */
        void onRewardEarned();
        /** يُستدعى عند فشل التحميل/العرض أو إغلاق الإعلان دون اكتماله. */
        default void onAdUnavailable(String reason) {}
    }

    // ══════════════════════════════════════════════════════════════════════
    //  تهيئة SDK
    // ══════════════════════════════════════════════════════════════════════

    /** يُستدعى مرة واحدة (من Application أو أول Activity) لتهيئة Mobile Ads SDK. */
    public static void init(Activity activity) {
        if (initialized) return;
        initialized = true;
        MobileAds.initialize(activity, status -> Log.d(TAG, "AdMob initialized"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  1. Rewarded Interstitial — العملات
    // ══════════════════════════════════════════════════════════════════════

    public interface AdReadyListener { void onAdReady(); }
    private static AdReadyListener adReadyListener;
    public void setAdReadyListener(AdReadyListener l) { adReadyListener = l; }

    public void preload(Activity activity) {
        if (rewardedAd != null || isLoading) return;
        isLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(activity, REWARDED_AD_UNIT_ID, request,
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        rewardedAd = ad;
                        isLoading = false;
                        Log.d(TAG, "Rewarded interstitial (coins) loaded.");
                        if (adReadyListener != null)
                            activity.runOnUiThread(() -> adReadyListener.onAdReady());
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        isLoading = false;
                        Log.w(TAG, "Rewarded interstitial (coins) failed: " + loadAdError.getMessage());
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

    // ══════════════════════════════════════════════════════════════════════
    //  2. Interstitial — إعلان بيني عادي
    // ══════════════════════════════════════════════════════════════════════

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
                        Log.d(TAG, "Interstitial loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isInterstitialLoading = false;
                        Log.w(TAG, "Interstitial failed: " + loadAdError.getMessage());
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

    // ══════════════════════════════════════════════════════════════════════
    //  3. Banner — شريط إعلاني
    // ══════════════════════════════════════════════════════════════════════

    /**
     * يُحمِّل ويعرض بانر AdMob داخل الـ ViewGroup المُمرَّر (عادةً FrameLayout أو
     * LinearLayout في أسفل الشاشة). الوحدة الإعلانية: BANNER_UNIT_ID.
     *
     * استخدام:
     * <pre>
     *   ads.showBanner(this, findViewById(R.id.banner_container));
     * </pre>
     *
     * @param activity        النشاط الحالي (مطلوب لبناء AdRequest).
     * @param bannerContainer الحاوية في التخطيط التي ستستقبل الـ AdView.
     */
    public void showBanner(Activity activity, ViewGroup bannerContainer) {
        if (bannerAdView != null) {
            // أعِد الاستخدام إن كان البانر محمَّلاً مسبقاً
            if (bannerAdView.getParent() == null) {
                bannerContainer.addView(bannerAdView);
            }
            return;
        }

        bannerAdView = new AdView(activity);
        bannerAdView.setAdUnitId(BANNER_UNIT_ID);
        bannerAdView.setAdSize(AdSize.BANNER);

        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner loaded.");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.w(TAG, "Banner failed: " + loadAdError.getMessage());
            }
        });

        bannerContainer.removeAllViews();
        bannerContainer.addView(bannerAdView);

        AdRequest request = new AdRequest.Builder().build();
        bannerAdView.loadAd(request);
    }

    /**
     * يجب استدعاؤها من onPause() في الـ Activity/Fragment الذي يحتوي البانر.
     */
    public void pauseBanner() {
        if (bannerAdView != null) bannerAdView.pause();
    }

    /**
     * يجب استدعاؤها من onResume() في الـ Activity/Fragment الذي يحتوي البانر.
     */
    public void resumeBanner() {
        if (bannerAdView != null) bannerAdView.resume();
    }

    /**
     * يجب استدعاؤها من onDestroy() في الـ Activity/Fragment الذي يحتوي البانر
     * لتحرير الموارد ومنع تسرّب الذاكرة.
     */
    public void destroyBanner() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  4. Rewarded Interstitial "coi" — الوحدة المُجزية الثانية
    // ══════════════════════════════════════════════════════════════════════

    /**
     * يُحمِّل الوحدة البينية مقابل مكافأة "coi" مسبقاً.
     * تُستخدم كوحدة مُجزية إضافية (مثلاً لفتح ميزة بدلاً من العملات).
     */
    public void preloadRewardedInterstitial(Activity activity) {
        if (rewardedInterstitialAd != null || isRewardedInterstitialLoading) return;
        isRewardedInterstitialLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(activity, REWARDED_INTERSTITIAL_UNIT_ID, request,
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        isRewardedInterstitialLoading = false;
                        Log.d(TAG, "Rewarded interstitial (coi) loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedInterstitialAd = null;
                        isRewardedInterstitialLoading = false;
                        Log.w(TAG, "Rewarded interstitial (coi) failed: " + loadAdError.getMessage());
                    }
                });
    }

    public boolean isRewardedInterstitialReady() {
        return rewardedInterstitialAd != null;
    }

    /**
     * يعرض إعلان "coi" البيني المُجزي. يمنح المستخدم المكافأة فقط عند اكتمال المشاهدة.
     *
     * @param activity النشاط الحالي.
     * @param listener مستمع يُستدعى عند الاستحقاق أو الفشل.
     */
    public void showRewardedInterstitial(Activity activity, RewardListener listener) {
        if (rewardedInterstitialAd == null) {
            if (listener != null) listener.onAdUnavailable("الإعلان غير جاهز، حاول لاحقاً");
            preloadRewardedInterstitial(activity);
            return;
        }

        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null;
                preloadRewardedInterstitial(activity);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedInterstitialAd = null;
                if (listener != null) listener.onAdUnavailable(adError.getMessage());
                preloadRewardedInterstitial(activity);
            }
        });

        rewardedInterstitialAd.show(activity,
                rewardItem -> { if (listener != null) listener.onRewardEarned(); });
    }
}
