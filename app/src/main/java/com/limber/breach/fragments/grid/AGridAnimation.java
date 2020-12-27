package com.limber.breach.fragments.grid;

import android.os.Handler;
import android.view.SurfaceHolder;

import androidx.fragment.app.Fragment;

public abstract class AGridAnimation {
    private final Handler mHandler;
    private final SurfaceHolder mHolder;

    protected AGridAnimation(Fragment fragment, SurfaceHolder holder) {
        mHandler = new Handler(fragment.requireActivity().getMainLooper());
        mHolder = holder;
    }

    protected SurfaceHolder getHolder() {
        return mHolder;
    }

    public void stop() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public void start() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Integer nextUpdateMs = onUpdate();
                if (nextUpdateMs == null) {
                    return;
                }

                mHandler.postDelayed(this, nextUpdateMs);
            }
        };

        runnable.run();
    }

    protected abstract Integer onUpdate();
}
