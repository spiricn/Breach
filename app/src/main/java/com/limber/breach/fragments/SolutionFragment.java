package com.limber.breach.fragments;

import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.limber.breach.R;
import com.limber.breach.analyzer.Node;
import com.limber.breach.analyzer.Result;
import com.limber.breach.fragments.grid.AGridAnimation;
import com.limber.breach.fragments.grid.SolutionAnimation;
import com.limber.breach.fragments.grid.WorkingAnimation;
import com.limber.breach.solver.PathScore;
import com.limber.breach.solver.Solver;
import com.limber.breach.utils.DrawUtils;
import com.limber.breach.utils.SoundPlayer;
import com.limber.breach.utils.Vibrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calculates and shows the correct solution on screen
 */
public class SolutionFragment extends Fragment {
    public SolutionFragment() {
        super(R.layout.fragment_solution);
    }

    /**
     * Allowed buffer range
     */
    private static final int kMAX_BUFFER_SIZE = 8;
    private static final int kMIN_BUFFER_SIZE = 3;

    /**
     * Start buffer size
     */
    private static final int kSTART_BUFFER_SIZE = 4;

    /**
     * If solution is calculated faster than this, wait a bit
     */
    private static final long kMIN_SOLVE_DURATION_MS = 1500;

    /**
     * Fragment arguments
     */
    private SolutionFragmentArgs mArgs;

    /**
     * Starts solve procedure
     */
    private Button mSolveButton;

    /**
     * Retries capture
     */
    private Button mRetryButton;

    /**
     * Increases buffer size
     */
    private Button mIncreaseBufferButton;

    /**
     * Decreases buffer size
     */
    private Button mDecreaseBufferButton;

    /**
     * System clock timestamp before solve was started
     */
    private long mSolveStartTimestamp;

    /**
     * Used to delay working animation if too fast
     */
    private Handler mDelayHandler;

    /**
     * Current grid animation
     */
    private AGridAnimation mCurrentAnimation = null;

    /**
     * Target surface holder
     */
    private SurfaceHolder mHolder;

    /**
     * Solves the analyzed grids
     */
    private Solver mSolver;

    /**
     * Current buffer size
     */
    private int mCurrentBufferSize = 4;

    @Override
    public void onDestroyView() {
        stopSolving();

        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mDelayHandler = new Handler(Looper.getMainLooper());

        mArgs = SolutionFragmentArgs.fromBundle(requireArguments());

        mRetryButton = view.findViewById(R.id.btnRetry);
        mRetryButton.setOnClickListener(view1 -> cancelOrRetry());

        setBufferSize(kSTART_BUFFER_SIZE);

        mDecreaseBufferButton = view.findViewById(R.id.buttonDecreaseBuffer);
        mDecreaseBufferButton.setOnClickListener(view12 -> changeBufferSize(-1));

        mIncreaseBufferButton = view.findViewById(R.id.buttonIncreaseBuffer);
        mIncreaseBufferButton.setOnClickListener(view12 -> changeBufferSize(1));

        ((SurfaceView) view.findViewById(R.id.solutionSurface)).getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mHolder = surfaceHolder;
                redraw();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                redraw();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });

        mSolveButton = view.findViewById(R.id.btnSolve);
        view.findViewById(R.id.btnSolve).setOnClickListener(v -> solve());
    }

    /**
     * Start solving
     */
    private void solve() {
        // Start measuring time
        mSolveStartTimestamp = System.currentTimeMillis();

        stopSolving();

        // Show the working animation
        mCurrentAnimation = new WorkingAnimation(this, mHolder, mArgs.getResult());
        mCurrentAnimation.start();

        Vibrator.get().play(Vibrator.Effect.ok);
        SoundPlayer.get().play(getContext(), SoundPlayer.Effect.working, true);

        // Disable user input & show loading spinner
        setInputEnabled(false);
        ProgressBar progressBar = requireView().findViewById(R.id.progressBar);
        mRetryButton.setText(R.string.btnCancel);
        progressBar.setVisibility(View.VISIBLE);

        Result result = mArgs.getResult();

        // Start solving
        List<List<Integer>> matrix = getRowValues(result.matrix.rows);
        List<List<Integer>> sequences = getRowValues(result.sequences.rows);
        mSolver = new Solver(matrix, sequences, mCurrentBufferSize, new Solver.IListener() {
            @Override
            public void onAborted() {
                stopSolving();
            }

            @Override
            public void onSolved(PathScore result) {
                if (getView() == null) {
                    return;
                }

                // If we finished too fast, delay the animation a bit (better UX)
                long solveDuration = System.currentTimeMillis() - mSolveStartTimestamp;

                Runnable resultRunnable = () -> {
                    stopSolving();

                    mCurrentAnimation = new SolutionAnimation(SolutionFragment.this, mHolder, mArgs.getResult(), result);
                    mCurrentAnimation.start();

                    Vibrator.get().play(Vibrator.Effect.success);
                };

                if (solveDuration >= kMIN_SOLVE_DURATION_MS) {
                    // Show solution right away
                    resultRunnable.run();
                } else {
                    // Delay
                    mDelayHandler.postDelayed(resultRunnable, kMIN_SOLVE_DURATION_MS - solveDuration);
                }
            }
        }, new Handler(Looper.getMainLooper()));

        mSolver.start();
    }

    /**
     * Cancel ongoing solves or retry from the top
     */
    private void cancelOrRetry() {
        boolean abort = mSolver != null;

        stopSolving();

        SoundPlayer.get().play(getContext(), SoundPlayer.Effect.cancel);
        Vibrator.get().play(Vibrator.Effect.ok);

        if (abort) {
            redraw();
            return;
        }

        NavDirections action = SolutionFragmentDirections.actionSolutionFragmentToCaptureFragment();
        Navigation.findNavController(requireView()).navigate(action);
    }

    /**
     * Enable or disable user input
     */
    private void setInputEnabled(boolean enabled) {
        mSolveButton.setEnabled(enabled);

        if (enabled) {
            refreshBufferButtons();
        } else {
            mIncreaseBufferButton.setEnabled(false);
            mDecreaseBufferButton.setEnabled(false);
        }
    }

    /**
     * Change the buffer size by given delta
     */
    private void changeBufferSize(int delta) {
        SoundPlayer.get().play(getContext(), SoundPlayer.Effect.short_beep);
        Vibrator.get().play(Vibrator.Effect.short_beep);
        setBufferSize(mCurrentBufferSize + delta);

        refreshBufferButtons();
    }

    /**
     * Refresh buffer button states
     */
    private void refreshBufferButtons() {
        mIncreaseBufferButton.setEnabled(mCurrentBufferSize < kMAX_BUFFER_SIZE);
        mDecreaseBufferButton.setEnabled(mCurrentBufferSize > kMIN_BUFFER_SIZE);
    }

    /**
     * Set a buffer size
     */
    private void setBufferSize(int bufferSize) {
        mCurrentBufferSize = bufferSize;
        if (mCurrentBufferSize > kMAX_BUFFER_SIZE) {
            mCurrentBufferSize = kMAX_BUFFER_SIZE;
        }
        if (mCurrentBufferSize < kMIN_BUFFER_SIZE) {
            mCurrentBufferSize = kMIN_BUFFER_SIZE;
        }

        ((TextView) requireView().findViewById(R.id.bufferSize)).setText(
                String.format(Locale.ENGLISH, "%d", mCurrentBufferSize)
        );
    }

    /**
     * Stop ongoing solve calculations (if any)
     */
    private void stopSolving() {
        mDelayHandler.removeCallbacksAndMessages(null);

        if (mSolver != null) {
            mSolver.stop();
            mSolver = null;
            SoundPlayer.get().stop();
        }

        requireView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        setInputEnabled(true);
        mRetryButton.setText(R.string.btnNewScan);

        if (mCurrentAnimation != null) {
            mCurrentAnimation.stop();
            mCurrentAnimation = null;
        }
    }

    /**
     * Redraw the grid
     */
    private void redraw() {
        Canvas canvas = mHolder.lockCanvas();

        Result result = mArgs.getResult();

        DrawUtils.drawGrid(result.matrix, result.bitmap, canvas);

        mHolder.unlockCanvasAndPost(canvas);
    }

    /**
     * Extract row values from nodes
     */
    private static List<List<Integer>> getRowValues(List<List<Node>> inRows) {
        List<List<Integer>> rows = new ArrayList<>();

        for (List<Node> row : inRows) {
            List<Integer> irow = new ArrayList<>();
            for (Node node : row) {
                irow.add(Integer.parseInt(node.text, 16));
            }

            rows.add(irow);
        }

        return rows;
    }
}
