package com.limber.breach.solver;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Breach minigame solver based on Nicolas Siplis algorithm
 * <p>
 * For more information see https://nicolas-siplis.com/blog/cyberpwned
 */
public class Solver {
    /**
     * Called on success
     */
    public interface SuccessCallback {
        void onSolved(PathScore result);
    }

    /**
     * Called on failure
     */
    public interface FailedCallback {
        void onFailed(Exception e);
    }

    /**
     * Input sequences
     */
    private final List<List<Integer>> mSequences;

    /**
     * Input buffer size
     */
    private final int mBufferSize;

    /**
     * Input code matrix
     */
    private final List<List<Integer>> mCodeMatrix;

    /**
     * Indication if start() was called
     */
    private boolean mRunning = false;

    /**
     * Execution background thread
     */
    private Thread mThread;

    /**
     * Callback listener
     */
    private final SuccessCallback mSuccessCallback;

    /**
     * Callback listener
     */
    private final FailedCallback mFailedCallback;

    /**
     * Handler used to execute callbacks
     */
    private final Handler mCallbackHandler;


    public Solver(@NonNull List<List<Integer>> codeMatrix, @NonNull List<List<Integer>> sequences,
                  int bufferSize, @NonNull SuccessCallback successCallback, FailedCallback failedCallback, Handler callbackHandler) {
        mCodeMatrix = codeMatrix;
        mSequences = sequences;
        mBufferSize = bufferSize;
        mSuccessCallback = successCallback;
        mFailedCallback = failedCallback;
        mCallbackHandler = callbackHandler;
    }

    /**
     * Start solving in the background
     */
    public void start() {
        if (mRunning) {
            throw new RuntimeException("Already running");
        }

        mRunning = true;

        mThread = new Thread(() -> {
            try {
                PathScore result = solve();
                mCallbackHandler.post(() -> mSuccessCallback.onSolved(result));
            } catch (Exception e) {
                mCallbackHandler.post(() -> mFailedCallback.onFailed(e));
            }
        });

        mThread.start();
    }

    /**
     * Stop solving
     */
    public void stop() {
        if (!mRunning) {
            return;
        }

        mRunning = false;

        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mCallbackHandler.removeCallbacks(null);
    }

    /**
     * Solve matrix
     *
     * @throws InterruptedException In case stop() was called during executio
     */
    private PathScore solve() throws InterruptedException {
        List<Path> paths = generatePaths(mCodeMatrix, mBufferSize);

        PathScore result = null;
        for (Path path : paths) {
            checkRunning();

            PathScore c = new PathScore(path, mSequences, mBufferSize, mCodeMatrix);
            if (result == null || c.score() > result.score()) {
                result = c;
            }
        }

        return result;
    }

    private static List<Coordinate> candidateCoords(List<List<Integer>> codeMatrix) {
        return candidateCoords(codeMatrix, 0, Coordinate.from(0, 0));
    }

    /**
     * Return next available row/column for specified turn and coordinate.
     * If it's the 1st turn the index is 0 so next_line would return the
     * first row. For the second turn, it would return the nth column, with n
     * being the coordinate's row
     */
    static List<Coordinate> candidateCoords(List<List<Integer>> codeMatrix, int turn, Coordinate coordinate) {
        List<Coordinate> coords = new ArrayList<>();

        for (int i = 0; i < codeMatrix.size(); i++) {
            Coordinate coord;
            if (turn % 2 == 0) {
                coord = new Coordinate(coordinate.row, i);
            } else {
                coord = new Coordinate(i, coordinate.column);
            }

            coords.add(coord);
        }

        return coords;
    }

    /**
     * Returns all possible paths with size equal to the buffer
     */
    private List<Path> generatePaths(List<List<Integer>> codeMatrix, int bufferSize) throws InterruptedException {
        Stack<Path> partialPathsStack = new Stack<>();
        partialPathsStack.push(new Path());

        List<Coordinate> candidateCoords = candidateCoords(codeMatrix);

        List<Path> completedPaths = new ArrayList<>();

        walkPaths(codeMatrix, bufferSize, completedPaths, partialPathsStack, 0, candidateCoords);

        return completedPaths;
    }

    void walkPaths(List<List<Integer>> codeMatrix, int bufferSize, List<Path> completedPaths,
                   Stack<Path> partialPathsStack, int turn, List<Coordinate> candidateCoords) throws InterruptedException {
        Path path = partialPathsStack.pop();

        for (Coordinate coord : candidateCoords) {
            checkRunning();

            Path newPath = path.add(new Path(coord));

            if (newPath == null) {
                // Skip coordinate if it has already been visited
                continue;
            }

            // Full path is added to the final return list and removed from the partial paths stack
            if (newPath.coordinates().size() == bufferSize) {
                completedPaths.add(newPath);
            } else {
                // Add new, lengthier partial path back into the stack
                partialPathsStack.push(newPath);

                walkPaths(
                        codeMatrix, bufferSize, completedPaths, partialPathsStack, turn + 1,
                        candidateCoords(codeMatrix, turn + 1, coord)
                );
            }

        }
    }

    private void checkRunning() throws InterruptedException {
        if (!mRunning) {
            throw new InterruptedException();
        }
    }

}
