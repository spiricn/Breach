package com.limber.breach.fragments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.limber.breach.R;
import com.limber.breach.analyzer.Grid;
import com.limber.breach.analyzer.Node;
import com.limber.breach.analyzer.Result;
import com.limber.breach.utils.DrawUtils;
import com.limber.breach.utils.SoundPlayer;
import com.limber.breach.utils.Vibrator;

import java.util.List;

/**
 * Shows the user analyzed grids
 */
public class VerifyFragment extends Fragment {
    /**
     * Indication if matrices or sequences should be displayed
     */
    public enum Mode {
        /**
         * Matrix confirmation mode
         * <p>
         * If the user confirms it, move on to sequences
         */
        matrix,

        /**
         * Sequences confirmation mode
         * <p>
         * If the user confirms it, move on to the solution
         */
        sequences
    }

    /**
     * Text paint used to display analyzed text
     */
    private static final TextPaint kTEXT_PAINT;

    /**
     * Input arguments
     */
    private VerifyFragmentArgs mArgs;

    /**
     * Target surface holder
     */
    private SurfaceHolder mSurfaceHolder;

    public VerifyFragment() {
        super(R.layout.fragment_verify);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mArgs = VerifyFragmentArgs.fromBundle(requireArguments());

        view.findViewById(R.id.verify_confirm).setOnClickListener(view1 -> onConfirm());
        view.findViewById(R.id.verify_retry).setOnClickListener(view1 -> onRetry());

        // Wait for the surface to become available
        ((SurfaceView) view.findViewById(R.id.surfaceView)).getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if (getView() == null) {
                    // Fragment destroyed
                    return;
                }

                mSurfaceHolder = surfaceHolder;

                redraw();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                if (getView() == null) {
                    // Fragment destroyed
                    return;
                }

                redraw();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });
    }

    /**
     * Retry capture (user is not happy with the scan result
     */
    private void onRetry() {
        SoundPlayer.get().play(getContext(), SoundPlayer.Effect.cancel);

        NavDirections action = VerifyFragmentDirections.actionFragmentVerifyToCaptureFragment();
        Navigation.findNavController(requireView()).navigate(action);
    }

    /**
     * Confirm current grid (user is happy with the scan result)
     */
    private void onConfirm() {
        SoundPlayer.get().play(getContext(), SoundPlayer.Effect.beep);
        Vibrator.get().play(Vibrator.Effect.ok);

        switch (mArgs.getMode()) {
            case matrix: {
                // Matrix verified, so move on to sequences
                NavDirections action = VerifyFragmentDirections.actionFragmentVerifySelf(
                        Mode.sequences,
                        mArgs.getAnalyzeResult()

                );

                Navigation.findNavController(requireView()).navigate(action);
                break;
            }
            case sequences: {
                // Sequences verified so move on to the solution
                NavDirections action = VerifyFragmentDirections.actionFragmentVerifyToSolutionFragment(mArgs.getAnalyzeResult());

                Navigation.findNavController(requireView()).navigate(action);
                break;
            }
        }

    }

    /**
     * Display the grid to the user
     */
    private void redraw() {
        Canvas canvas = mSurfaceHolder.lockCanvas();

        // Pick a grid based on mode
        Result result = mArgs.getAnalyzeResult();
        Grid grid = mArgs.getMode() == Mode.matrix ? result.matrix : result.sequences;

        // Render the grid & highlight the nodes
        DrawUtils.drawGrid(grid, result.bitmap, canvas);
        DrawUtils.highlightNodes(grid, canvas);

        // Draw the analyzed text next to the each node
        for (List<Node> nodeRow : grid.rows) {
            for (Node node : nodeRow) {
                canvas.drawText(node.text, node.boundingBox.left, node.boundingBox.top, kTEXT_PAINT);
            }
        }

        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    static {
        kTEXT_PAINT = new TextPaint();
        kTEXT_PAINT.setTextSize(60);
        kTEXT_PAINT.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        kTEXT_PAINT.setColor(Color.argb(200, 255, 0, 0));
        kTEXT_PAINT.setAntiAlias(true);

    }
}
