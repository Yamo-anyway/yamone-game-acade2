package com.yamone.arcade2.core;

import java.util.Random;

/** Pure game rules. Seconds and degrees; no Android, wall clock, or network dependency. */
public final class OrbitEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, HIT, PERFECT, MISS }
    public static final double DURATION = 60.0;
    private final Random random;
    public final long seed;
    private State state = State.READY;
    private State resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, angle = -90, target = 0, ringTime, jumpTime;
    private int lives = 3, jumps, perfects, feedbackId;
    private boolean held;

    public OrbitEngine(long seed) { this.seed = seed; random = new Random(seed); nextTarget(); }
    public void press() {
        if (state == State.READY) state = State.RUNNING;
        if (state == State.RUNNING) held = true;
    }
    public void release() {
        if (state != State.RUNNING || !held) return;
        held = false;
        if (jumpTime > 0) return;
        double error = angularDistance(angle, target);
        if (error <= tolerance()) {
            jumps++;
            feedback = error <= 7 ? Feedback.PERFECT : Feedback.HIT;
            if (feedback == Feedback.PERFECT) perfects++;
            feedbackId++;
            jumpTime = .32;
            ringTime = 0;
            nextTarget();
        } else miss();
    }
    public void cancelInput() { held = false; }
    public void pause() {
        if (state == State.RUNNING || state == State.READY) {
            resumeState = state; state = State.PAUSED; held = false;
        }
    }
    public void resume() { if (state == State.PAUSED) state = resumeState; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || state != State.RUNNING) return;
        double remaining = Math.min(seconds, DURATION - elapsed);
        // Fixed small substeps keep collision/deadline decisions consistent across frame rates.
        while (remaining > 1e-9 && state == State.RUNNING) {
            double dt = Math.min(.01, remaining);
            remaining -= dt; elapsed += dt;
            if (jumpTime > 0) jumpTime = Math.max(0, jumpTime - dt);
            else {
                ringTime += dt;
                if (held) angle = normalize(angle + speed() * dt);
                if (ringTime + 1e-9 >= ringLimit()) miss();
            }
            if (elapsed + 1e-9 >= DURATION) { elapsed = DURATION; finish(); }
        }
    }
    private void nextTarget() { target = normalize(angle + 100 + random.nextDouble() * 160); }
    private void miss() {
        lives--; feedback = Feedback.MISS; feedbackId++; ringTime = 0;
        if (lives <= 0) finish();
        else nextTarget();
    }
    private void finish() { state = State.FINISHED; held = false; }
    public static double angularDistance(double a, double b) { return Math.abs(((a - b) % 360 + 540) % 360 - 180); }
    private static double normalize(double value) { return (value % 360 + 360) % 360; }
    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double remaining() { return Math.max(0, DURATION - elapsed); }
    public double angle() { return angle; }
    public double target() { return target; }
    public double tolerance() { return Math.max(14, 25 - elapsed * .18); }
    public double speed() { return 130 + elapsed * 1.7; }
    public double ringLimit() { return Math.max(2.8, 4.5 - elapsed * .02); }
    public double ringFraction() { return Math.max(0, 1 - ringTime / ringLimit()); }
    public double jumpProgress() { return jumpTime > 0 ? 1 - jumpTime / .32 : -1; }
    public boolean held() { return held; }
    public int lives() { return lives; }
    public int jumps() { return jumps; }
    public int perfects() { return perfects; }
    // A failed/idle ring awards no points. Camping cannot increase the score.
    public int score() { return jumps * 100 + perfects * 50; }
}
