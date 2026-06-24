package com.syrianvcg.editor;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * VcgCoins — محفظة عملات بسيطة داخل التطبيق.
 * المستخدم يجمع عملات عبر مشاهدة إعلان مُجزٍ (Rewarded Ad)، ويمكن إنفاقها
 * مستقبلاً على مزايا داخل المحرر (ثيمات إضافية، أيقونات، إلخ).
 * التخزين محلي فقط (SharedPreferences) — لا حاجة لخادم خارجي.
 */
public class VcgCoins {

    private static final String PREFS = "vcg_coins";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_LAST_EARN_AT = "last_earn_at";
    private static final String KEY_EARNED_TODAY = "earned_today";
    private static final String KEY_EARNED_DAY_STAMP = "earned_day_stamp";

    /** عدد العملات الممنوحة عند كل مشاهدة إعلان كاملة بنجاح. */
    public static final int COINS_PER_AD = 10;

    private final SharedPreferences prefs;

    public VcgCoins(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getBalance() {
        return prefs.getInt(KEY_BALANCE, 0);
    }

    /** يُستدعى فقط بعد تأكيد onUserEarnedReward من AdMob. */
    public int grantCoinsForAd() {
        int newBalance = getBalance() + COINS_PER_AD;
        long today = dayStamp();
        int earnedToday = prefs.getLong(KEY_EARNED_DAY_STAMP, -1) == today
            ? prefs.getInt(KEY_EARNED_TODAY, 0) + COINS_PER_AD
            : COINS_PER_AD;

        prefs.edit()
            .putInt(KEY_BALANCE, newBalance)
            .putLong(KEY_LAST_EARN_AT, System.currentTimeMillis())
            .putLong(KEY_EARNED_DAY_STAMP, today)
            .putInt(KEY_EARNED_TODAY, earnedToday)
            .apply();
        return newBalance;
    }

    public boolean spend(int amount) {
        int balance = getBalance();
        if (balance < amount) return false;
        prefs.edit().putInt(KEY_BALANCE, balance - amount).apply();
        return true;
    }

    public int getEarnedToday() {
        long today = dayStamp();
        if (prefs.getLong(KEY_EARNED_DAY_STAMP, -1) != today) return 0;
        return prefs.getInt(KEY_EARNED_TODAY, 0);
    }

    private static long dayStamp() {
        return System.currentTimeMillis() / (24L * 60 * 60 * 1000);
    }
}
