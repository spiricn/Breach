package com.limber.breach.fragments.grid;

import android.os.Handler;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

/**
 * Base grid animation class
 */
public abstract class AGridAnimation {
    /**
     * Handler used to execute commands
     */
    private final Handler mHandler;

    /**
     * Target surface holder
     */
    private final SurfaceHolder mHolder;

    /**
     * Indication if animation is running
     */
    private boolean mRunning = false;

    /**
     * Parent fragment
     */
    private Fragment mFragment;

    protected AGridAnimation(Fragment fragment, SurfaceHolder holder) {
        mHandler = new Handler(fragment.requireActivity().getMainLooper());
        mHolder = holder;
        mFragment = fragment;
    }

    protected Fragment getFragment() {
        return mFragment;
    }

    protected SurfaceHolder getHolder() {
        return mHolder;
    }

    /**
     * Stop the animation
     */
    public void stop() {
        if (!mRunning) {
            return;
        }

        mHandler.removeCallbacksAndMessages(null);

        mRunning = false;
    }

    /**
     * Start the animation
     */
    public void start() {
        if (mRunning) {
            throw new IllegalStateException();
        }

        mRunning = true;

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

    /**
     * Update animation step
     *
     * @return Delay between next update in milliseconds, or null if finished
     */
    protected abstract Integer onUpdate();
}
