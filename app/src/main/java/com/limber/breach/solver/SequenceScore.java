package com.limber.breach.solver;

import java.util.List;

public class SequenceScore {
    private final int mMaxProgress;

    private final List<Integer> mSequence;

    private int mScore;

    private final int mRewardLevel;

    private int mBufferSize;

    public SequenceScore(List<Integer> sequence, int bufferSize, int rewardLevel) {
        mMaxProgress = sequence.size();
        mSequence = sequence;
        mScore = 0;
        mRewardLevel = rewardLevel;
        mBufferSize = bufferSize;
    }

    void compute(int compare) {
        if (!completed()) {
            if (mSequence.get(mScore) == compare) {
                increase();
            } else {
                decrease();
            }
        }

    }

    /**
     * When the head of the sequence matches the targeted node, increase the score by 1
     * If the sequence has been completed, set the score depending on the reward level
     */
    void increase() {
        mBufferSize -= 1;
        mScore += 1;
        if (completed()) {
            // Can be adjusted to maximize either:
            //  a) highest quality rewards, possibly lesser quantity
            mScore = (int) Math.pow(10, mRewardLevel + 1);
            //  b) highest amount of rewards, possibly lesser quality
            // self.score = 100 * (self.reward_level + 1)
        }
    }

    /**
     * When an incorrect value is matched against the current head of the sequence, the score is decreased by 1 (can't go below 0)
     * If it's not possible to complete the sequence, set the score to a negative value depending on the reward
     */
    void decrease() {
        mBufferSize -= 1;
        if (mScore > 0) {
            mScore -= 1;
        }
        if (completed()) {
            mScore = -mRewardLevel - 1;
        }
    }

    /**
     * A sequence is considered completed if no further progress is possible or necessary
     */
    boolean completed() {
        return mScore < 0 || mScore >= mMaxProgress || mBufferSize < mMaxProgress - mScore;
    }

    public int score() {
        return mScore;
    }
}
