package com.limber.breach.fragments.grid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import androidx.fragment.app.Fragment;

import com.limber.breach.analyzer.Node;
import com.limber.breach.analyzer.Result;
import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.PathScore;
import com.limber.breach.utils.DrawUtils;
import com.limber.breach.utils.SoundPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Animation which shows the solution on screen
 */
public class SolutionAnimation extends AGridAnimation {

    /**
     * Sleep time between step renders
     */
    private static final int kSTEP_TIME_MS = 200;

    /**
     * Used to show step text
     */
    private static final TextPaint kTEXT_PAINT;

    /**
     * Minimum text size
     */
    private static final int kMIN_TEXT_SIZE = 60;

    /**
     * Maximum text size
     */
    private static final int kMAX_TEXT_SIZE = kMIN_TEXT_SIZE + 30;

    /**
     * Analysis result
     */
    private final Result mResult;

    /**
     * All solution coordinates
     */
    private final List<Coordinate> mAllCoords;

    /**
     * Highlighted subset of mAllCoords
     */
    private final List<Coordinate> mHighlightedCoords = new ArrayList<>();

    public SolutionAnimation(Fragment fragment, SurfaceHolder holder, Result result, PathScore pathScore) {
        super(fragment, holder);

        mResult = result;

        mAllCoords = new ArrayList<>(pathScore.path().coordinates());
    }

    @Override
    protected Integer onUpdate() {
        if (mAllCoords.size() == mHighlightedCoords.size()) {
            return null;
        }

        mHighlightedCoords.add(mAllCoords.get(mHighlightedCoords.size()));
        redraw();

        SoundPlayer.get().play(getFragment().getContext(), SoundPlayer.Effect.beep);

        return kSTEP_TIME_MS;
    }

    /**
     * Redraw animation
     */
    private void redraw() {
        Canvas canvas = getHolder().lockCanvas();

        // Draw background grid
        DrawUtils.drawGrid(mResult.matrix, mResult.bitmap, canvas);
        DrawUtils.scaleFor(canvas, DrawUtils.getRect(mResult.matrix));

        int stepCounter = 1;

        // Highlight all the steps so far
        for (Coordinate coord : mHighlightedCoords) {
            Node node = mResult.matrix.rows.get(coord.row).get(coord.column);

            // Scale the text size based on step number (from smallest to largest)
            int scaledTextSize = (int) (kMIN_TEXT_SIZE + (kMAX_TEXT_SIZE - kMIN_TEXT_SIZE) * ((float) stepCounter / (float) mAllCoords.size()));
            kTEXT_PAINT.setTextSize(scaledTextSize);

            canvas.drawText("" + stepCounter, node.boundingBox.left, node.boundingBox.top, kTEXT_PAINT);

            stepCounter++;
        }

        getHolder().unlockCanvasAndPost(canvas);
    }

    static {
        kTEXT_PAINT = new TextPaint();

        kTEXT_PAINT.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        kTEXT_PAINT.setColor(Color.argb(200, 255, 0, 0));
        kTEXT_PAINT.setAntiAlias(true);
    }
}
