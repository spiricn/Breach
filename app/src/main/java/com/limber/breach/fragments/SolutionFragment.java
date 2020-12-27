package com.limber.breach.fragments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.limber.breach.Analyzer;
import com.limber.breach.DrawUtils;
import com.limber.breach.R;
import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.Path;
import com.limber.breach.solver.PathScore;
import com.limber.breach.solver.Solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SolutionFragment extends Fragment {
    public SolutionFragment() {
        super(R.layout.fragment_solution);
    }

    SolutionFragmentArgs mArgs;
    NumberPicker mNumberPicker;
    Button mSolveButton;
    Button mRetryButton;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        mArgs = SolutionFragmentArgs.fromBundle(requireArguments());

        mRetryButton = view.findViewById(R.id.btnRetry);

        mRetryButton.setOnClickListener(view1 -> {
            if (mSolver != null) {
                stop();
                return;
            }

            NavDirections action = SolutionFragmentDirections.actionSolutionFragmentToCaptureFragment();
            Navigation.findNavController(requireView()).navigate(action);
        });

        mNumberPicker = view.findViewById(R.id.bufferSize);
        mNumberPicker.setMaxValue(8);
        mNumberPicker.setMinValue(4);

        ((SurfaceView) view.findViewById(R.id.solutionSurface)).getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                draw(surfaceHolder);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });

        mSolveButton = view.findViewById(R.id.btnSolve);

        view.findViewById(R.id.btnSolve).setOnClickListener(v -> {
            Log.e("@#", "solve clicked");
            stop();

            mSolveButton.setEnabled(false);
            ProgressBar progressBar = requireView().findViewById(R.id.progressBar);
            mRetryButton.setText(R.string.cancel);

            progressBar.setVisibility(View.VISIBLE);

            draw();

            Analyzer.Result result = mArgs.getResult();

            List<List<Integer>> matrix = convertRows(result.matrix.nodes);
            List<List<Integer>> sequences = convertRows(result.sequences.nodes);

            mSolver = new Solver(matrix, sequences, mNumberPicker.getValue(), new Solver.IListener() {
                @Override
                public void onAborted() {
                    Log.e("@#", "aborted");
                }

                @Override
                public void onSolved(PathScore result) {
                    Log.e("@#", "solved");
                    stop();
                    showResult(result);
                }
            }, new Handler(Looper.getMainLooper()));

            mSolver.start();
        });
    }

    SurfaceHolder mHolder;
    Solver mSolver;

    void draw() {
        Objects.requireNonNull(mHolder);
        draw(mHolder);
    }

    void stop() {
        if (mSolver != null) {
            mSolver.stop();
            mSolver = null;
        }

        requireView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        mSolveButton.setEnabled(true);
        mRetryButton.setText(R.string.retry);
    }


    void draw(SurfaceHolder holder) {
        mHolder = holder;

        Canvas canvas = holder.lockCanvas();

        Analyzer.Result result = mArgs.getResult();

        DrawUtils.drawGrid(result.matrix, result.bitmap, canvas);

        holder.unlockCanvasAndPost(canvas);
    }

    void showResult(PathScore pathScore) {

        Path path = pathScore.path();

        Canvas canvas = mHolder.lockCanvas();

        DrawUtils.drawGrid(mArgs.getResult().matrix, mArgs.getResult().bitmap, canvas);

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(60);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setColor(Color.argb(200, 255, 0, 0));

        int stepCounter = 0;
        for (Coordinate coord : path.coordinates()) {

            Analyzer.GridNode resNode = mArgs.getResult().matrix.nodes.get(coord.row).get(coord.column);

            canvas.drawText("" + stepCounter, resNode.boundingBox.left, resNode.boundingBox.top, textPaint);

            stepCounter++;
        }

        mHolder.unlockCanvasAndPost(canvas);
    }


    static List<List<Integer>> convertRows(List<List<Analyzer.GridNode>> inRows) {
        List<List<Integer>> rows = new ArrayList<>();

        for (List<Analyzer.GridNode> row : inRows) {
            List<Integer> irow = new ArrayList<>();
            for (Analyzer.GridNode node : row) {
                irow.add(Integer.parseInt(node.text, 16));
            }

            rows.add(irow);
        }

        return rows;
    }

}
