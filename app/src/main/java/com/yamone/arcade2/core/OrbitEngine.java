package com.yamone.arcade2.core;

import java.util.Random;

/** Orbit rules v2: automatic rotation, tap timing, endless play and five misses. */
public final class OrbitEngine {
    public enum State { RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, HIT, PERFECT, MISS }
    public static final int MAX_LIVES = 5, MAX_SCORE = 1_000_000_000;
    public static final double FEEDBACK_SECONDS = .16;
    private final Random random;
    public final long seed;
    private State state = State.RUNNING;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, angle = 270, target, travel, targetDistance, feedbackTime;
    private int lives = MAX_LIVES, jumps, perfects, score, combo, bestCombo, feedbackId;

    public OrbitEngine(long seed) { this.seed = seed; random = new Random(seed); nextTarget(); }
    /** A new DOWN event judges immediately; release/hold is not part of this rule set. */
    public void tap() {
        if (state != State.RUNNING || feedbackTime > 0) return;
        double error = Math.abs(travel - targetDistance);
        if (error <= tolerance() + 1e-9) {
            jumps++; combo++; bestCombo = Math.max(bestCombo, combo);
            feedback = error <= perfectTolerance() + 1e-9 ? Feedback.PERFECT : Feedback.HIT;
            if (feedback == Feedback.PERFECT) perfects++;
            score = (int)Math.min(MAX_SCORE, (long)score + (feedback == Feedback.PERFECT ? 150 : 100));
            resolved();
        } else miss();
    }
    public void pause() { if (state == State.RUNNING) state = State.PAUSED; }
    public void resume() { if (state == State.PAUSED) state = State.RUNNING; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || state != State.RUNNING) return;
        double remaining = seconds;
        // One exact event at the target exit. Rotation also continues during feedback.
        // No input can resolve at most five misses, so huge deltas cannot busy-loop.
        while (remaining > 1e-9 && state == State.RUNNING) {
            double angularSpeed = speed();
            double toExit = Math.max(0, (targetDistance + tolerance() - travel) / angularSpeed);
            double dt = Math.min(remaining, toExit);
            double rotation = angularSpeed * dt;
            elapsed += dt; remaining -= dt; travel += rotation;
            angle = normalize(angle + rotation);
            feedbackTime = Math.max(0, feedbackTime - dt);
            if (toExit <= dt + 1e-9) miss();
        }
    }
    private void nextTarget() {
        targetDistance = 100 + random.nextDouble() * 140;
        target = normalize(angle + targetDistance); travel = 0;
    }
    private void miss() {
        lives--; combo = 0; feedback = Feedback.MISS;
        if (lives == 0) {
            feedbackId++; feedbackTime = FEEDBACK_SECONDS; state = State.FINISHED;
        } else resolved();
    }
    private void resolved() {
        feedbackId++; feedbackTime = FEEDBACK_SECONDS; nextTarget();
    }
    public static double angularDistance(double a, double b) { return Math.abs(((a - b) % 360 + 540) % 360 - 180); }
    private static double normalize(double value) { return (value % 360 + 360) % 360; }
    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double feedbackRemaining() { return feedbackTime; }
    public double elapsed() { return elapsed; }
    public double angle() { return angle; }
    public double target() { return target; }
    public double tolerance() { return 10 + 20 / (1 + feedbackId / 40.0); }
    public double perfectTolerance() { return tolerance() * .28; }
    public double speed() { return 120 + 240 * feedbackId / (feedbackId + 60.0); }
    public boolean inTarget() { return state == State.RUNNING && Math.abs(travel - targetDistance) <= tolerance(); }
    public int level() { return 1 + feedbackId / 10; }
    public int lives() { return lives; }
    public int jumps() { return jumps; }
    public int perfects() { return perfects; }
    public int combo() { return combo; }
    public int bestCombo() { return bestCombo; }
    public int score() { return score; }
}
