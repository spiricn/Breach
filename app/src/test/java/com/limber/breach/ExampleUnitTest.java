package com.limber.breach;

import com.limber.breach.solver.Coordinate;
import com.limber.breach.solver.PathScore;
import com.limber.breach.solver.Solver;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {

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

        PathScore maxScore = Solver.solve(codeMatrix, sequences, bufferSize);
        assertNotNull(maxScore);

        assertEquals(
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