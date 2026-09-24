package com.yamone.arcade2.core;

import java.util.Random;

/** Swipe-cut stacking and center-of-mass stability rules, independent from Android. */
public final class StackSliceEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, BALANCED, WOBBLE, FALL, TIMEOUT }
    public static final double DURATION = 60, BASE_WIDTH = 150, CUT_WIDTH = 26, MIN_WIDTH = 32, RECOVERY = .24;
    private static final double MAX_BLOCK_WIDTH = 130, STABILITY_PADDING = 3;
    private static final int MAX_LAYERS = 256;
    public final long seed;
    private final Random random;
    private final double[] centers = new double[MAX_LAYERS], widths = new double[MAX_LAYERS];
    private State state = State.READY, resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, phase, turnElapsed, recovery, tilt, maxTilt;
    private int layerCount = 1, placed, balanced, balanceStreak, bestBalanceStreak, blockSerial, feedbackId;
    private boolean waitingNext;

    public StackSliceEngine(long seed) {
        this.seed = seed; random = new Random(seed);
        centers[0] = 0; widths[0] = BASE_WIDTH;
        prepareBlock();
    }
    public void start() { if (state == State.READY) state = State.RUNNING; }
    /** Direction -1 cuts the left edge; +1 cuts the right edge. */
    public void swipe(int direction) {
        if (state != State.RUNNING || waitingNext || (direction != -1 && direction != 1)) return;
        double supportCenter = topCenter(), supportWidth = topWidth();
        double incomingWidth = incomingWidth();
        double trimmedWidth = incomingWidth - CUT_WIDTH;
        double trimmedCenter = incomingCenter() - direction * CUT_WIDTH / 2;
        double left = Math.max(trimmedCenter - trimmedWidth / 2, supportCenter - supportWidth / 2);
        double right = Math.min(trimmedCenter + trimmedWidth / 2, supportCenter + supportWidth / 2);
        double overlap = right - left;
        if (overlap < MIN_WIDTH - 1e-9 || layerCount >= MAX_LAYERS) { collapse(Feedback.FALL); return; }

        centers[layerCount] = (left + right) / 2;
        widths[layerCount] = overlap;
        layerCount++;
        double nextTilt = calculateTilt();
        if (Math.abs(nextTilt) >= 1 - 1e-9) {
            layerCount--;
            collapse(Feedback.FALL);
            return;
        }
        tilt = nextTilt; maxTilt = Math.max(maxTilt, Math.abs(tilt));
        placed++;
        int bonus = (int)Math.round(Math.max(0, 1 - Math.abs(tilt)) * 100);
        score += 100 + bonus;
        if (Math.abs(tilt) <= .25 + 1e-9) {
            balanced++; balanceStreak++; bestBalanceStreak = Math.max(bestBalanceStreak, balanceStreak);
            feedback = Feedback.BALANCED;
        } else {
            balanceStreak = 0; feedback = Feedback.WOBBLE;
        }
        feedbackId++; waitingNext = true; recovery = RECOVERY;
    }
    public void pause() {
        if (state == State.READY || state == State.RUNNING) { resumeState = state; state = State.PAUSED; }
    }
    public void resume() { if (state == State.PAUSED) state = resumeState; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || state != State.RUNNING) return;
        double remaining = Math.min(seconds, DURATION - elapsed);
        while (remaining > 1e-9 && state == State.RUNNING) {
            double dt = Math.min(.002, remaining);
            remaining -= dt; elapsed += dt;
            if (waitingNext) {
                recovery = Math.max(0, recovery - dt);
                if (recovery <= 1e-9) prepareBlock();
            } else {
                phase += angularSpeed() * dt;
                turnElapsed += dt;
            }
            // A completed minute wins over a block timeout at the same simulation instant.
            if (elapsed + 1e-9 >= DURATION) { elapsed = DURATION; finish(); break; }
            if (!waitingNext && turnElapsed + 1e-9 >= turnLimit()) { collapse(Feedback.TIMEOUT); break; }
        }
    }
    private void prepareBlock() {
        waitingNext = false; recovery = 0; turnElapsed = 0;
        phase = random.nextBoolean() ? 0 : Math.PI;
        blockSerial++;
    }
    private double calculateTilt() {
        double worst = 0;
        for (int support = 0; support < layerCount - 1; support++) {
            double mass = 0, moment = 0;
            for (int above = support + 1; above < layerCount; above++) {
                mass += widths[above]; moment += widths[above] * centers[above];
            }
            double centerOfMass = moment / mass;
            double halfSupport = Math.max(1, widths[support] / 2 - STABILITY_PADDING);
            double ratio = (centerOfMass - centers[support]) / halfSupport;
            if (Math.abs(ratio) > Math.abs(worst)) worst = ratio;
        }
        return worst;
    }
    private void collapse(Feedback cause) {
        feedback = cause; feedbackId++; balanceStreak = 0; finish();
    }
    private void finish() { state = State.FINISHED; waitingNext = false; }

    private int score;
    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public double elapsed() { return elapsed; }
    public double remaining() { return Math.max(0, DURATION - elapsed); }
    public int score() { return score; }
    public int placed() { return placed; }
    public int balanced() { return balanced; }
    public int balanceStreak() { return balanceStreak; }
    public int bestBalanceStreak() { return bestBalanceStreak; }
    public int feedbackId() { return feedbackId; }
    public int blockSerial() { return blockSerial; }
    public boolean waitingNext() { return waitingNext; }
    public double recoveryRemaining() { return recovery; }
    public double turnElapsed() { return turnElapsed; }
    public double turnLimit() { return Math.max(2.7, 3.6 - elapsed * .015); }
    public double angularSpeed() { return Math.min(2.62, 1.9 + elapsed * .012); }
    public double tilt() { return tilt; }
    public double maxTilt() { return maxTilt; }
    public int layerCount() { return layerCount; }
    public double layerCenter(int index) { return centers[index]; }
    public double layerWidth(int index) { return widths[index]; }
    public double topCenter() { return centers[layerCount - 1]; }
    public double topWidth() { return widths[layerCount - 1]; }
    public double incomingWidth() { return Math.min(MAX_BLOCK_WIDTH, topWidth() + CUT_WIDTH); }
    public double travelAmplitude() { return Math.min(58, Math.max(34, topWidth() * .38)); }
    public double incomingCenter() { return topCenter() + travelAmplitude() * Math.sin(phase); }
}
