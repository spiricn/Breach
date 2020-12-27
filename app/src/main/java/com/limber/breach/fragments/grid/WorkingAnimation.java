package com.limber.breach.fragments.grid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;

import androidx.fragment.app.Fragment;

import com.limber.breach.DrawUtils;
import com.limber.breach.analyzer.GridNode;
import com.limber.breach.analyzer.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorkingAnimation extends AGridAnimation {

    private final List<GridNode> mAllNodes = new ArrayList<>();
    private final List<GridNode> mCurrentNodes = new ArrayList<>();
    private List<GridNode> mPopInNodes = null;
    Random mRandom = new Random();
    boolean mDirectionPopIn = true;
    Result mResult;

    public WorkingAnimation(Fragment fragment, SurfaceHolder holder, Result result) {
        super(fragment, holder);
        mResult = result;

        for (List<GridNode> row : mResult.matrix.nodes) {
            mAllNodes.addAll(row);
        }
    }

    @Override
    protected Integer onUpdate() {

        if ((mDirectionPopIn && mCurrentNodes.size() == mAllNodes.size()) ||
                (!mDirectionPopIn && mCurrentNodes.isEmpty())) {
            mDirectionPopIn = !mDirectionPopIn;
            mPopInNodes = null;
            return 0;
        }

        if (mDirectionPopIn) {
            if (mPopInNodes == null) {
                mPopInNodes = new ArrayList<>(mAllNodes);
            }

            int nextIndex = mRandom.nextInt(mPopInNodes.size());

            mCurrentNodes.add(mPopInNodes.get(nextIndex));
            mPopInNodes.remove(nextIndex);
        } else {
            mCurrentNodes.remove(mRandom.nextInt(mCurrentNodes.size()));
        }

        Canvas canvas = getHolder().lockCanvas();


        DrawUtils.drawGrid(mResult.matrix, mResult.bitmap, canvas);

        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.GREEN);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(6 + mRandom.nextInt(6));

        DrawUtils.highlightNodes(mResult.matrix, canvas, mCurrentNodes, boundaryPaint);

        getHolder().unlockCanvasAndPost(canvas);

        return mRandom.nextInt(15);
    }
}
