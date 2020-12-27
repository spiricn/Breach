package com.limber.breach.solver;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Solver {
    List<List<Integer>> mSequences;
    int mBufferSize;
    List<List<Integer>> mCodeMatrix;
    boolean mRunning = false;
    Thread mThread;
    IListener mListener;
    Handler mCallbackHandler;

    public interface IListener {
        void onAborted();

        void onSolved(PathScore result);
    }

    public Solver(@NonNull List<List<Integer>> codeMatrix, @NonNull List<List<Integer>> sequences,
                  int bufferSize, @NonNull IListener listener, Handler callbackHandler) {
        mCodeMatrix = codeMatrix;
        mSequences = sequences;
        mBufferSize = bufferSize;
        mListener = listener;
        mCallbackHandler = callbackHandler;
    }

    public void start() {
        if (mRunning) {
            throw new RuntimeException("Already running");
        }

        mRunning = true;

        mThread = new Thread(() -> {
            try {
                PathScore result = solve();
                mCallbackHandler.post(() -> mListener.onSolved(result));

            } catch (InterruptedException e) {
                mCallbackHandler.post(() -> mListener.onAborted());
            }
        });
        mThread.start();
    }

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
    }

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

    static List<Coordinate> candidateCoords(List<List<Integer>> codeMatrix) {
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
