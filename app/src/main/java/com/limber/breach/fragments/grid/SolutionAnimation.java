package com.limber.breach.fragments.grid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import androidx.fragment.app.Fragment;

import com.limber.breach.DrawUtils;
import com.limber.breach.SoundPlayer;
import com.limber.breach.analyzer.GridNode;
import com.limber.breach.analyzer.Result;
import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.PathScore;

import java.util.ArrayList;
import java.util.List;

public class SolutionAnimation extends AGridAnimation {

    Result mResult;

    PathScore mPathScore;
    List<Coordinate> mAllCoords;
    List<Coordinate> mCoords = new ArrayList<>();

    public SolutionAnimation(Fragment fragment, SurfaceHolder holder, Result result, PathScore pathScore) {
        super(fragment, holder);
        mResult = result;
        mPathScore = pathScore;

        mAllCoords = new ArrayList<>(mPathScore.path().coordinates());
    }

    @Override
    protected Integer onUpdate() {
        if (mAllCoords.size() == mCoords.size()) {
            return null;
        }

        Canvas canvas = getHolder().lockCanvas();

        DrawUtils.drawGrid(mResult.matrix, mResult.bitmap, canvas);
        DrawUtils.scaleFor(canvas, DrawUtils.getRect(mResult.matrix));

        TextPaint textPaint = new TextPaint();

        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setColor(Color.argb(200, 255, 0, 0));

        mCoords.add(mAllCoords.get(mCoords.size()));

        int stepCounter = 0;
        for (Coordinate coord : mCoords) {

            GridNode resNode = mResult.matrix.nodes.get(coord.row).get(coord.column);

            textPaint.setTextSize(60 + (stepCounter == mCoords.size() - 1 ? 30 : 0));

            canvas.drawText("" + stepCounter, resNode.boundingBox.left, resNode.boundingBox.top, textPaint);

            stepCounter++;
        }

        getHolder().unlockCanvasAndPost(canvas);

        SoundPlayer.get().play(SoundPlayer.Effect.beep);
        return 200;
    }
}
