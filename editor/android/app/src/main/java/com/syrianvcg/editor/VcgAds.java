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
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

/**
 * VcgAds — غلاف حول Google Mobile Ads (AdMob) يدير وحدات الإعلانات
 * الحقيقية لتطبيق "Syrian VCG Editor" (applicationId: com.syrianvcg.editor).
 *
 * ⚠️ المعرّفات هنا حقيقية ومُضمَّنة مباشرة في الكود بناءً على طلب المطوّر،
 * بدل قراءتها من local.properties/BuildConfig. يُفضَّل عدم رفعها لريبو عام
 * إن أمكن، لكن AdMob لا يسمح بعرض إعلانات إلا على تطبيقك المسجَّل بنفس
 * applicationId، فهي ليست بحساسية مفاتيح API الخاصة.
 */
public final class VcgAds {

    private static final String TAG = "VcgAds";

    // ═══════════════════════════════════════════════════════════════════════
    //  معرّفات AdMob الحقيقية — ca-app-pub-1525040025806904
    // ═══════════════════════════════════════════════════════════════════════

    /** معرّف التطبيق — يُستخدم في AndroidManifest.xml (meta-data APPLICATION_ID). */
    public static final String APP_ID = "ca-app-pub-1525040025806904~6185155034";

    /** "set" — إعلان بيني مقابل مكافأة (منح عملات في SettingsActivity). */
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-1525040025806904/5325469129";

    /** "john" — إعلان بيني عادي (Interstitial). */
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1525040025806904/4027633665";

    /** "oi" — إعلان شاشة فتح التطبيق (App Open Ad) — نوعه الصحيح، لا يُستخدم كبانر. */
    public static final String APP_OPEN_AD_UNIT_ID = "ca-app-pub-1525040025806904/3150465990";

    /** "coi" — إعلان بيني مقابل مكافأة ثاني. */
    public static final String REWARDED_INTERSTITIAL_UNIT_ID = "ca-app-pub-1525040025806904/2699305783";

    /** "uio" — متوفّر للاستخدام المستقبلي (Rewarded). */
    public static final String UIO_REWARDED_UNIT_ID = "ca-app-pub-1525040025806904/5259086172";

    /** "mop" — متوفّر للاستخدام المستقبلي (Native / مدمج مع المحتوى). */
    public static final String MOP_NATIVE_AD_UNIT_ID = "ca-app-pub-1525040025806904/9153601713";

    /**
     * ⚠️ لا توجد حتى الآن وحدة Banner حقيقية بالحساب.
     * هذا معرّف اختبار رسمي من Google (Test Banner) — استبدله بمعرّف Banner
     * حقيقي من لوحة AdMob (Ad units → Add ad unit → Banner) فور إنشائه،
     * وإلا سيستمر ظهور إعلانات اختبارية فقط في شريط البانر.
     */
    public static final String BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";

    // ── حالة SDK ───────────────────────────────────────────────────────────
    private static boolean initialized = false;

    // ── Rewarded Interstitial "set" (الأولى — لمنح العملات) ────────────────
    private static RewardedInterstitialAd rewardedAd;
    private static boolean isLoading = false;

    // ── Interstitial "john" ──────────────────────────────────────────────
    private static InterstitialAd interstitialAd;
    private static boolean isInterstitialLoading = false;

    // ── Rewarded Interstitial "coi" ─────────────────────────────────────
    private static RewardedInterstitialAd rewardedInterstitialAd;
    private static boolean isRewardedInterstitialLoading = false;

    // ── App Open "oi" ─────────────────────────────────────────────────────
    private static AppOpenAd appOpenAd;
    private static boolean isAppOpenLoading = false;
    private static long appOpenLoadTime = 0L;

    // ── Banner ─────────────────────────────────────────────────────────────
    private AdView bannerAdView;

    // ══════════════════════════════════════════════════════════════════════
    //  Listeners
    // ══════════════════════════════════════════════════════════════════════

    public interface RewardListener {
        /** يُستدعى فقط إذا شاهد المستخدم الإعلان كاملاً واستحق المكافأة. */
        void onRewardEarned();
        /** يُستدعى عند فشل التحميل/العرض أو إغلاق الإعلان دون اكتماله. */
        default void onAdUnavailable(String reason) {}
    }

    public interface AdReadyListener { void onAdReady(); }
    private static AdReadyListener adReadyListener;
    public void setAdReadyListener(AdReadyListener l) { adReadyListener = l; }

    // ══════════════════════════════════════════════════════════════════════
    //  تهيئة SDK
    // ══════════════════════════════════════════════════════════════════════

    /** يُستدعى مرة واحدة (من أول Activity) لتهيئة Mobile Ads SDK. */
    public static void init(Activity activity) {
        if (initialized) return;
        initialized = true;
        MobileAds.initialize(activity, status -> Log.d(TAG, "AdMob initialized"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  1. Rewarded Interstitial "set" — العملات
    // ══════════════════════════════════════════════════════════════════════

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
                        Log.d(TAG, "Rewarded interstitial 'set' (coins) loaded.");
                        if (adReadyListener != null)
                            activity.runOnUiThread(() -> adReadyListener.onAdReady());
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        isLoading = false;
                        Log.w(TAG, "Rewarded interstitial 'set' failed: code="
                                + loadAdError.getCode() + " msg=" + loadAdError.getMessage());
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
    //  2. Interstitial "john" — إعلان بيني عادي
    // ══════════════════════════════════════════════════════════════════════

    public void preloadInterstitial(Activity activity) {
        if (interstitialAd != null || isInterstitialLoading) return;
        isInterstitialLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, request,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        isInterstitialLoading = false;
                        Log.d(TAG, "Interstitial 'john' loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isInterstitialLoading = false;
                        Log.w(TAG, "Interstitial 'john' failed: code="
                                + loadAdError.getCode() + " msg=" + loadAdError.getMessage());
                    }
                });
    }

    /** يعرض الإعلان البيني إن كان جاهزًا، وفي كل الحالات ينفّذ onProceed بعد إغلاقه (أو فورًا لو غير جاهز). */
    public void showInterstitial(Activity activity, Runnable onProceed) {
        if (interstitialAd == null) {
            preloadInterstitial(activity);
            onProceed.run();
            return;
        }
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                preloadInterstitial(activity);
                onProceed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                interstitialAd = null;
                preloadInterstitial(activity);
                onProceed.run();
            }
        });
        interstitialAd.show(activity);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  3. Rewarded Interstitial "coi" — وحدة مُجزية ثانية
    // ══════════════════════════════════════════════════════════════════════

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
                        Log.d(TAG, "Rewarded interstitial 'coi' loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedInterstitialAd = null;
                        isRewardedInterstitialLoading = false;
                        Log.w(TAG, "Rewarded interstitial 'coi' failed: code="
                                + loadAdError.getCode() + " msg=" + loadAdError.getMessage());
                    }
                });
    }

    public boolean isRewardedInterstitialReady() {
        return rewardedInterstitialAd != null;
    }

    public void showRewardedInterstitial(Activity activity, RewardListener listener) {
        if (rewardedInterstitialAd == null) {
            listener.onAdUnavailable("الإعلان غير جاهز بعد، حاول بعد لحظات");
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
                listener.onAdUnavailable(adError.getMessage());
                preloadRewardedInterstitial(activity);
            }
        });
        rewardedInterstitialAd.show(activity, rewardItem -> listener.onRewardEarned());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  4. App Open Ad "oi" — إعلان شاشة فتح التطبيق (النوع الصحيح لهذه الوحدة)
    // ══════════════════════════════════════════════════════════════════════

    /** يحمّل إعلان شاشة الفتح مسبقًا. يُفضَّل استدعاؤه من Application.onCreate أو SplashActivity. */
    public static void preloadAppOpenAd(Activity activity) {
        if (appOpenAd != null && isAppOpenAdFresh()) return;
        if (isAppOpenLoading) return;
        isAppOpenLoading = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, APP_OPEN_AD_UNIT_ID, request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        isAppOpenLoading = false;
                        appOpenLoadTime = System.currentTimeMillis();
                        Log.d(TAG, "App Open 'oi' loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        appOpenAd = null;
                        isAppOpenLoading = false;
                        Log.w(TAG, "App Open 'oi' failed: code="
                                + loadAdError.getCode() + " msg=" + loadAdError.getMessage());
                    }
                });
    }

    /** إعلانات App Open تنتهي صلاحيتها بعد 4 ساعات تقريبًا حسب توصية Google. */
    private static boolean isAppOpenAdFresh() {
        long fourHoursMs = 4L * 60 * 60 * 1000;
        return System.currentTimeMillis() - appOpenLoadTime < fourHoursMs;
    }

    public static boolean isAppOpenAdReady() {
        return appOpenAd != null && isAppOpenAdFresh();
    }

    /** يعرض إعلان شاشة الفتح إن كان جاهزًا (نموذجي: عند رجوع المستخدم للتطبيق من الخلفية). */
    public static void showAppOpenAdIfAvailable(Activity activity, Runnable onDone) {
        if (!isAppOpenAdReady()) {
            preloadAppOpenAd(activity);
            onDone.run();
            return;
        }
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAd = null;
                preloadAppOpenAd(activity);
                onDone.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                appOpenAd = null;
                preloadAppOpenAd(activity);
                onDone.run();
            }
        });
        appOpenAd.show(activity);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  5. Banner — شريط إعلاني (يستخدم حاليًا معرّف اختبار، راجع التحذير أعلاه)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * يعرض بانر داخل الحاوية المعطاة. مثال استخدام:
     *   FrameLayout bannerContainer = findViewById(R.id.banner_container);
     *   ads.showBanner(this, bannerContainer);
     */
    public void showBanner(Activity activity, ViewGroup bannerContainer) {
        if (bannerAdView != null) {
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
                Log.w(TAG, "Banner failed: code="
                        + loadAdError.getCode() + " msg=" + loadAdError.getMessage());
            }
        });

        bannerContainer.removeAllViews();
        bannerContainer.addView(bannerAdView);

        AdRequest request = new AdRequest.Builder().build();
        bannerAdView.loadAd(request);
    }

    public void pauseBanner() {
        if (bannerAdView != null) bannerAdView.pause();
    }

    public void resumeBanner() {
        if (bannerAdView != null) bannerAdView.resume();
    }

    public void destroyBanner() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
    }
}
