package com.limber.breach.fragments.grid;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import androidx.fragment.app.Fragment;

import com.limber.breach.DrawUtils;
import com.limber.breach.analyzer.Node;
import com.limber.breach.analyzer.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animation to be played while calculations are ongoing
 */
public class WorkingAnimation extends AGridAnimation {
    /**
     * Maximum wait time between steps
     */
    private static final int kMAX_WAIT_TIME_MS = 15;

    /**
     * All grid matrix nodes
     */
    private final List<Node> mAllNodes = new ArrayList<>();

    /**
     * Subset of mAllNodes
     */
    private final List<Node> mHighlightedNodes = new ArrayList<>();

    /**
     * List of nodes left to be popped in
     */
    private List<Node> mPopInNodes = null;

    /**
     * RNG calculator
     */
    private final Random mRandom = new Random();

    /**
     * Indication if animation is poping in or out
     */
    private boolean mDirectionPopIn = true;

    /**
     * Analysis result
     */
    private final Result mResult;

    public WorkingAnimation(Fragment fragment, SurfaceHolder holder, Result result) {
        super(fragment, holder);

        mResult = result;

        for (List<Node> row : mResult.matrix.rows) {
            mAllNodes.addAll(row);
        }
    }

    @Override
    protected Integer onUpdate() {

        // Switch directions
        if ((mDirectionPopIn && mHighlightedNodes.size() == mAllNodes.size()) ||
                (!mDirectionPopIn && mHighlightedNodes.isEmpty())) {
            mDirectionPopIn = !mDirectionPopIn;
            mPopInNodes = null;
            return 0;
        }

        if (mDirectionPopIn) {
            // If we're popping in, add random nodes to our list one by one
            if (mPopInNodes == null) {
                mPopInNodes = new ArrayList<>(mAllNodes);
            }

            int nextIndex = mRandom.nextInt(mPopInNodes.size());

            mHighlightedNodes.add(mPopInNodes.get(nextIndex));
            mPopInNodes.remove(nextIndex);
        } else {
            // If we're popping out, remove random nodes from our list one by one
            mHighlightedNodes.remove(mRandom.nextInt(mHighlightedNodes.size()));
        }

        redraw();

        return mRandom.nextInt(kMAX_WAIT_TIME_MS);
    }

    /**
     * Redraw animation
     */
    private void redraw() {
        Canvas canvas = getHolder().lockCanvas();

        DrawUtils.drawGrid(mResult.matrix, mResult.bitmap, canvas);
        DrawUtils.highlightNodes(mResult.matrix, canvas, mHighlightedNodes);

        getHolder().unlockCanvasAndPost(canvas);
    }
}
