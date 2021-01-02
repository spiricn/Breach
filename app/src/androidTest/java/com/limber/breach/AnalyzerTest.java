package com.limber.breach;

import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.limber.breach.analyzer.Analyzer;
import com.limber.breach.analyzer.Result;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AnalyzerTest {

    @Test
    public void test5x5_3() throws ExecutionException, InterruptedException {
        verify(R.drawable.test_5x5_3_01,
                Arrays.asList(
                        Arrays.asList(0x1c, 0xe9, 0x1c, 0x55, 0x1c),
                        Arrays.asList(0xe9, 0x55, 0x1c, 0x1c, 0xbd),
                        Arrays.asList(0x55, 0xbd, 0x1c, 0xbd, 0x55),
                        Arrays.asList(0x55, 0x1c, 0x55, 0x55, 0x1c),
                        Arrays.asList(0xe9, 0x1c, 0x1c, 0x1c, 0x55)
                ),
                Arrays.asList(
                        Arrays.asList(0x55, 0x1c),
                        Arrays.asList(0x1c, 0x1c, 0xe9),
                        Arrays.asList(0xbd, 0xe9, 0x55)
                )
        );
    }

    @Test
    public void test6x6_1() throws ExecutionException, InterruptedException {
        verify(R.drawable.test_6x6_1_01,
                Arrays.asList(
                        Arrays.asList(0x1c, 0x1c, 0xbd, 0xbd, 0xbd, 0x1c),
                        Arrays.asList(0x1c, 0x55, 0x55, 0x55, 0xe9, 0x1c),
                        Arrays.asList(0xe9, 0x1c, 0xbd, 0xe9, 0xbd, 0xe9),
                        Arrays.asList(0xbd, 0x55, 0x1c, 0xe9, 0x1c, 0x1c),
                        Arrays.asList(0x55, 0x1c, 0x55, 0x1c, 0xbd, 0x7a),
                        Arrays.asList(0x55, 0xbd, 0x7a, 0xe9, 0x55, 0x1c)
                ),
                Collections.singletonList(
                        Arrays.asList(0xe9, 0x1c, 0xe9)
                )
        );
    }

    private void verify(int drawable, List<List<Integer>> expectedMatrix, List<List<Integer>> expectedSequences) throws ExecutionException, InterruptedException {
        CompletableFuture<Result> f = new CompletableFuture<>();

        Handler handler = new Handler(Looper.getMainLooper());

        Analyzer.analyze(BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getTargetContext().getResources(),
                drawable),
                f::complete,
                f::completeExceptionally,
                handler);

        Result result = f.get();
        assertNotNull(result);

        assertArrayEquals(expectedMatrix.toArray(), result.matrix.getValues().toArray());
        assertArrayEquals(expectedSequences.toArray(), result.sequences.getValues().toArray());

    }
}