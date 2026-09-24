package com.yamone.arcade2.core;

import java.util.Random;

/** Expanding-wave timing rules; independent from Android drawing and wall clocks. */
public final class PocketPulseEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, GOOD, GREAT, PERFECT, MISS }
    public static final double DURATION = 60, MIN_RADIUS = 24, GOOD_WINDOW = 18, RECOVERY = .28;
    private static final double PERFECT_WINDOW = 4, GREAT_WINDOW = 10;
    public final long seed;
    private final Random random;
    private State state = State.READY, resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, radius = MIN_RADIUS, targetRadius, recovery, lastError = Double.NaN;
    private int lives = 4, score, combo, bestCombo, hits, goods, greats, perfects, misses, pulseSerial, feedbackId;
    private boolean waitingNext;

    public PocketPulseEngine(long seed) {
        this.seed = seed; random = new Random(seed); chooseTarget();
    }
    public void tap() {
        if (state == State.READY) { state = State.RUNNING; return; }
        if (state != State.RUNNING || waitingNext) return;
        lastError = Math.abs(radius - targetRadius);
        if (lastError <= GOOD_WINDOW + 1e-9) resolveHit(); else resolveMiss();
    }
    public void pause() {
        if (state == State.READY || state == State.RUNNING) {
            resumeState = state; state = State.PAUSED;
        }
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
                if (recovery <= 1e-9) nextPulse();
            } else {
                radius += speed() * dt;
            }
            // Completion wins over a missed pulse at the same simulation instant.
            if (elapsed + 1e-9 >= DURATION) { elapsed = DURATION; finish(); break; }
            if (!waitingNext && radius > targetRadius + GOOD_WINDOW + 1e-9) {
                lastError = radius - targetRadius; resolveMiss();
            }
        }
    }
    private void resolveHit() {
        combo++; bestCombo = Math.max(bestCombo, combo); hits++;
        int base;
        if (lastError <= PERFECT_WINDOW + 1e-9) { feedback = Feedback.PERFECT; perfects++; base = 200; }
        else if (lastError <= GREAT_WINDOW + 1e-9) { feedback = Feedback.GREAT; greats++; base = 150; }
        else { feedback = Feedback.GOOD; goods++; base = 100; }
        score += base + Math.min(100, (combo - 1) * 10);
        feedbackId++; beginRecovery();
    }
    private void resolveMiss() {
        lives--; misses++; combo = 0; feedback = Feedback.MISS; feedbackId++;
        if (lives <= 0) finish(); else beginRecovery();
    }
    private void beginRecovery() { waitingNext = true; recovery = RECOVERY; }
    private void nextPulse() {
        waitingNext = false; recovery = 0; radius = MIN_RADIUS; lastError = Double.NaN; pulseSerial++; chooseTarget();
    }
    private void chooseTarget() { targetRadius = 72 + random.nextDouble() * 58; }
    private void finish() { state = State.FINISHED; waitingNext = false; recovery = 0; }

    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double remaining() { return Math.max(0, DURATION - elapsed); }
    public double radius() { return radius; }
    public double targetRadius() { return targetRadius; }
    public double error() { return Math.abs(radius - targetRadius); }
    public double lastError() { return lastError; }
    public double speed() { return 72 + elapsed * .9; }
    public boolean waitingNext() { return waitingNext; }
    public double recoveryRemaining() { return recovery; }
    public int pulseSerial() { return pulseSerial; }
    public int lives() { return lives; }
    public int score() { return score; }
    public int combo() { return combo; }
    public int bestCombo() { return bestCombo; }
    public int hits() { return hits; }
    public int goods() { return goods; }
    public int greats() { return greats; }
    public int perfects() { return perfects; }
    public int misses() { return misses; }
}
