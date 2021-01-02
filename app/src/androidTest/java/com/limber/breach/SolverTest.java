package com.limber.breach;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.PathScore;
import com.limber.breach.solver.Solver;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SolverTest {
    @Test
    public void solve5x5() throws ExecutionException, InterruptedException {

        List<List<Integer>> codeMatrix = Arrays.asList(
                Arrays.asList(0x1c, 0xbd, 0x55, 0xe9, 0x55),
                Arrays.asList(0x1c, 0xbd, 0x1c, 0x55, 0xe9),
                Arrays.asList(0x55, 0xe9, 0xe9, 0xbd, 0xbd),
                Arrays.asList(0x55, 0xff, 0xff, 0x1c, 0x1c),
                Arrays.asList(0xff, 0xe9, 0x1c, 0xbd, 0xff)
        );

        List<List<Integer>> sequences = Arrays.asList(
                Arrays.asList(0x1c, 0x1c, 0x55),
                Arrays.asList(0X55, 0Xff, 0X1c),
                Arrays.asList(0xbd, 0xe9, 0xbd, 0x55),
                Arrays.asList(0x55, 0x1c, 0xff, 0xbd)
        );

        int bufferSize = 7;

        HandlerThread thread = new HandlerThread("Test");
        thread.start();

        Handler handler = new Handler(Looper.getMainLooper());

        CompletableFuture<PathScore> f = new CompletableFuture<>();

        Solver solver = new Solver(codeMatrix, sequences, bufferSize, f::complete, f::completeExceptionally,
                handler);

        solver.start();

        PathScore maxScore = f.get();
        assertNotNull(maxScore);

        assertArrayEquals(
                Arrays.asList(Coordinate.from(0, 1),
                        Coordinate.from(2, 1),
                        Coordinate.from(2, 3),
                        Coordinate.from(1, 3),
                        Coordinate.from(1, 0),
                        Coordinate.from(4, 0),
                        Coordinate.from(4, 3)).toArray(),
                maxScore.path().coordinates().toArray());
    }

}