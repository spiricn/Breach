package com.limber.breach.fragments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.limber.breach.DrawUtils;
import com.limber.breach.R;
import com.limber.breach.SoundPlayer;
import com.limber.breach.analyzer.GridNode;
import com.limber.breach.analyzer.Result;
import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.Path;
import com.limber.breach.solver.PathScore;
import com.limber.breach.solver.Solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SolutionFragment extends Fragment {
    public SolutionFragment() {
        super(R.layout.fragment_solution);
    }

    private static final int kMAX_BUFFER_SIZE = 8;
    private static final int kMIN_BUFFER_SIZE = 3;
    private static final int kSTART_BUFFER_SIZE = 4;

    private static final long kMIN_SOLVE_DURATION_MS = 1500;

    SolutionFragmentArgs mArgs;
    Button mSolveButton;
    Button mRetryButton;
    long mSolveStartTimestamp;
    Handler mDelayHandler;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stop();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mDelayHandler = new Handler(Looper.getMainLooper());

        mArgs = SolutionFragmentArgs.fromBundle(requireArguments());

        mRetryButton = view.findViewById(R.id.btnRetry);

        mRetryButton.setOnClickListener(view1 -> {
            boolean abort = mSolver != null;

            stop();

            SoundPlayer.get().play(SoundPlayer.Effect.cancel);

            if (abort) {
                return;
            }

            NavDirections action = SolutionFragmentDirections.actionSolutionFragmentToCaptureFragment();
            Navigation.findNavController(requireView()).navigate(action);
        });

        setBufferSize(kSTART_BUFFER_SIZE);

        view.findViewById(R.id.buttonDecreaseBuffer).setOnClickListener(view12 -> changeBufferSize(-1));
        view.findViewById(R.id.buttonIncreaseBuffer).setOnClickListener(view12 -> changeBufferSize(1));

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
            mSolveStartTimestamp = System.currentTimeMillis();

            stop();

            mSolveButton.setEnabled(false);
            ProgressBar progressBar = requireView().findViewById(R.id.progressBar);
            mRetryButton.setText(R.string.btnCancel);

            progressBar.setVisibility(View.VISIBLE);

            SoundPlayer.get().play(SoundPlayer.Effect.working, true);

            draw();


            Result result = mArgs.getResult();

            List<List<Integer>> matrix = convertRows(result.matrix.nodes);
            List<List<Integer>> sequences = convertRows(result.sequences.nodes);

            mSolver = new Solver(matrix, sequences, mBufferSize, new Solver.IListener() {
                @Override
                public void onAborted() {
                    stop();
                }

                @Override
                public void onSolved(PathScore result) {
                    long solveDuration = System.currentTimeMillis() - mSolveStartTimestamp;

                    Runnable resultRunnable = () -> {
                        stop();
                        showResult(result);
                    };

                    if (solveDuration >= kMIN_SOLVE_DURATION_MS) {
                        resultRunnable.run();
                    } else {
                        mDelayHandler.postDelayed(resultRunnable, kMIN_SOLVE_DURATION_MS - solveDuration);
                    }
                }
            }, new Handler(Looper.getMainLooper()));

            mSolver.start();
        });
    }

    SurfaceHolder mHolder;
    Solver mSolver;
    int mBufferSize = 4;

    void changeBufferSize(int delta) {
        SoundPlayer.get().play(SoundPlayer.Effect.short_beep);
        setBufferSize(mBufferSize + delta);
    }

    void setBufferSize(int bufferSize) {
        mBufferSize = bufferSize;
        if (mBufferSize > kMAX_BUFFER_SIZE) {
            mBufferSize = kMAX_BUFFER_SIZE;
        }
        if (mBufferSize < kMIN_BUFFER_SIZE) {
            mBufferSize = kMIN_BUFFER_SIZE;
        }

        ((TextView) requireView().findViewById(R.id.bufferSize)).setText(
                String.format(Locale.ENGLISH, "%d", mBufferSize)
        );
    }

    void draw() {
        Objects.requireNonNull(mHolder);
        draw(mHolder);
    }

    void stop() {
        mDelayHandler.removeCallbacksAndMessages(null);

        if (mSolver != null) {
            mSolver.stop();
            mSolver = null;
            SoundPlayer.get().stop();
        }

        requireView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        mSolveButton.setEnabled(true);
        mRetryButton.setText(R.string.btnRetry);
    }


    void draw(SurfaceHolder holder) {
        mHolder = holder;

        Canvas canvas = holder.lockCanvas();

        Result result = mArgs.getResult();

        DrawUtils.drawGrid(result.matrix, result.bitmap, canvas);

        holder.unlockCanvasAndPost(canvas);
    }

    void showResult(PathScore pathScore) {
        SoundPlayer.get().play(SoundPlayer.Effect.success);

        Path path = pathScore.path();

        Canvas canvas = mHolder.lockCanvas();

        DrawUtils.drawGrid(mArgs.getResult().matrix, mArgs.getResult().bitmap, canvas);

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(60);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setColor(Color.argb(200, 255, 0, 0));

        int stepCounter = 0;
        for (Coordinate coord : path.coordinates()) {

            GridNode resNode = mArgs.getResult().matrix.nodes.get(coord.row).get(coord.column);

            canvas.drawText("" + stepCounter, resNode.boundingBox.left, resNode.boundingBox.top, textPaint);

            stepCounter++;
        }

        mHolder.unlockCanvasAndPost(canvas);
    }


    static List<List<Integer>> convertRows(List<List<GridNode>> inRows) {
        List<List<Integer>> rows = new ArrayList<>();

        for (List<GridNode> row : inRows) {
            List<Integer> irow = new ArrayList<>();
            for (GridNode node : row) {
                irow.add(Integer.parseInt(node.text, 16));
            }

            rows.add(irow);
        }

        return rows;
    }

}
