package com.limber.breach.solver;

import java.util.ArrayList;
import java.util.List;

public class PathScore {
    public PathScore(Path path, List<List<Integer>> sequences, int bufferSize, List<List<Integer>> codeMatrix) {
        mPath = path;

        List<SequenceScore> sequenceScores = new ArrayList<>();

        int rewardLevel = 0;
        for (List<Integer> sequence : sequences) {
            sequenceScores.add(new SequenceScore(sequence, bufferSize, rewardLevel));

            rewardLevel++;
        }

        for (Coordinate coord : mPath.coordinates()) {
            for (SequenceScore seqScore : sequenceScores) {
                seqScore.compute(codeMatrix.get(coord.row).get(coord.column));
            }
        }

        mScore = 0;
        for (SequenceScore seqScore : sequenceScores) {
            mScore += seqScore.score();
        }
    }

    public Path path() {
        return mPath;
    }

    public int score() {
        return mScore;
    }

    private final Path mPath;
    private int mScore;
}
