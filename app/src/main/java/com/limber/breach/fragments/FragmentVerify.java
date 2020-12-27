package com.limber.breach.fragments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.limber.breach.Analyzer;
import com.limber.breach.DrawUtils;
import com.limber.breach.R;
import com.limber.breach.SoundPlayer;

import java.util.List;

public class FragmentVerify extends Fragment {

    FragmentVerifyArgs mArgs;

    public FragmentVerify() {
        super(R.layout.fragment_verify);
    }

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mArgs = FragmentVerifyArgs.fromBundle(getArguments());

        mSurfaceView = view.findViewById(R.id.surfaceView);

        view.findViewById(R.id.verify_confirm).setOnClickListener(view1 -> {
            SoundPlayer.get().play(SoundPlayer.Effect.beep);

            if (!mArgs.getVerifyMatrix()) {
                NavDirections action = FragmentVerifyDirections.actionFragmentVerifyToSolutionFragment(mArgs.getAnalyzeResult());

                Navigation.findNavController(getView()).navigate(action);
            } else {
                NavDirections action = FragmentVerifyDirections.actionFragmentVerifySelf(
                        mArgs.getAnalyzeResult()
                ).setVerifyMatrix(false);

                Navigation.findNavController(getView()).navigate(action);
            }
        });

        view.findViewById(R.id.verify_retry).setOnClickListener(view1 -> retry());

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mSurfaceHolder = surfaceHolder;

                draw();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                draw();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });
    }

    void retry() {
        SoundPlayer.get().play(SoundPlayer.Effect.cancel);

        NavDirections action = FragmentVerifyDirections.actionFragmentVerifyToCaptureFragment();
        Navigation.findNavController(getView()).navigate(action);
    }

    void draw() {
        Canvas canvas = mSurfaceHolder.lockCanvas();

        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.GREEN);
        boundaryPaint.setStyle(Paint.Style.STROKE);
        boundaryPaint.setStrokeWidth(2);

        Analyzer.Result result = mArgs.getAnalyzeResult();

        Analyzer.Grid grid = mArgs.getVerifyMatrix() ? result.matrix : result.sequences;

        DrawUtils.drawGrid(grid, result.bitmap, canvas);

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(60);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setColor(Color.argb(200, 255, 0, 0));

        for (List<Analyzer.GridNode> nodeRow : grid.nodes) {
            for (Analyzer.GridNode node : nodeRow) {
                canvas.drawText(node.text, node.boundingBox.left, node.boundingBox.top, textPaint);
            }
        }

        mSurfaceHolder.unlockCanvasAndPost(canvas);

        StringBuilder b = new StringBuilder();

        b.append("Confirm " + (mArgs.getVerifyMatrix() ? "matrix" : "sequences") + ":\n\n");

        for (List<Analyzer.GridNode> line : grid.nodes) {
            for (Analyzer.GridNode node : line) {
                b.append("\t" + node.text + "  ");
            }

            b.append("\n");
        }

//        ((TextView) getView().findViewById(R.id.scannedResults)).setText(b.toString());
    }


}
