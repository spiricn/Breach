package com.limber.breach;

import android.content.Context;

public class Vibrator {
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

    static Vibrator sInstance = null;

    android.os.Vibrator mService;

    public Vibrator(Context context) {
        mService = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static void create(Context context) {
        sInstance = new Vibrator(context);
    }

    public static Vibrator get() {
        return sInstance;
    }

    public void play(Effect effect) {

        if(!mEnabled) {
            return;
        }

        mService.vibrate(effect.getPattern(), -1);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    boolean mEnabled = true;
}
