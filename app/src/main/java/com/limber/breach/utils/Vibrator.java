package com.limber.breach.utils;

import android.content.Context;

/**
 * Plays application vibration effects
 */
public class Vibrator {

    /**
     * All available effects
     */
    public enum Effect {
        short_beep(new long[]{0, 1}),
        ok(new long[]{0, 5}),
        success(new long[]{0, 20}),
        error(new long[]{0, 40});

        Effect(long[] pattern) {
            mPattern = pattern;
        }

        long[] mPattern;

        public long[] getPattern() {
            return mPattern;
        }
    }

    /**
     * Singleton instance
     */
    static Vibrator sInstance = null;

    /**
     * Vibrator service
     */
    private final android.os.Vibrator mService;

    /**
     * Indication if effects are enabled
     */
    private boolean mEnabled = true;

    public Vibrator(Context context) {
        mService = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Create singleton
     */
    public static void create(Context context) {
        sInstance = new Vibrator(context);
    }

    /**
     * Get singleton
     */
    public static Vibrator get() {
        return sInstance;
    }

    /**
     * Play an effect
     */
    public void play(Effect effect) {
        if (!mEnabled) {
            return;
        }

        mService.vibrate(effect.getPattern(), -1);
    }

    /**
     * Enable or disable effects
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
