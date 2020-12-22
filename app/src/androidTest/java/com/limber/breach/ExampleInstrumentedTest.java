package com.limber.breach;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void analyzeAll() throws ExecutionException, InterruptedException {
        CompletableFuture<Analyzer.Result> f = new CompletableFuture<>();

        Analyzer.analyze(BitmapFactory.decodeResource(InstrumentationRegistry.getInstrumentation().getTargetContext().getResources(),
                R.drawable.test1), f::complete, e -> f.complete(null));

        Analyzer.Result result = f.get();
        assertNotNull(result);

        assertEquals(Arrays.asList(
                Arrays.asList(0x1c, 0xe9, 0x1c, 0x55, 0x1c),
                Arrays.asList(0xe9, 0x55, 0x1c, 0x1c, 0xbd),
                Arrays.asList(0x55, 0xbd, 0x1c, 0xbd, 0x55),
                Arrays.asList(0x55, 0x1c, 0x55, 0x55, 0x1c),
                Arrays.asList(0xe9, 0x1c, 0x1c, 0x1c, 0x55)
        ).toArray(), result.matrix.toArray());

        assertEquals(Arrays.asList(
                Arrays.asList(0x55, 0x1c),
                Arrays.asList(0x1c, 0x1c, 0xe9),
                Arrays.asList(0xbd, 0xe9, 0x55)
        ).toArray(), result.sequences.toArray());
    }
}