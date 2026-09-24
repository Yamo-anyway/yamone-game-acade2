package com.yamone.arcade2.core;

import java.util.Random;

/** Two lanes; one rising wall at a time. All timing is in active-play seconds. */
public final class ColorBreakEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, HIT, MISS }
    public static final double DURATION = 60, RECOVERY = .28;
    public final long seed;
    private final Random random;
    private final int[] colors = new int[2];
    private State state = State.READY, resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, wallTime, wallDuration, recovery;
    private int lane, targetColor, lives = 3, score, combo, bestCombo, passed, feedbackId;

    public ColorBreakEngine(long seed) { this.seed = seed; random = new Random(seed); nextWall(); }
    public void tapLane(int value) {
        if (value < 0 || value > 1) return;
        if (state == State.READY) state = State.RUNNING;
        if (state == State.RUNNING) lane = value;
    }
    public void pause() {
        if (state == State.READY || state == State.RUNNING) { resumeState = state; state = State.PAUSED; }
    }
    public void resume() { if (state == State.PAUSED) state = resumeState; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || state != State.RUNNING) return;
        double remaining = Math.min(seconds, DURATION - elapsed);
        // Step to exact events so a delayed frame cannot tunnel through a wall or score twice.
        while (remaining > 1e-9 && state == State.RUNNING) {
            boolean recovering = recovery > 0;
            double untilEvent = recovering ? recovery : wallDuration - wallTime;
            double dt = Math.min(remaining, untilEvent);
            elapsed += dt; remaining -= dt;
            if (recovering) recovery = Math.max(0, recovery - dt);
            else wallTime += dt;
            // At the time limit the round ends before another wall is judged.
            if (elapsed + 1e-9 >= DURATION) { elapsed = DURATION; state = State.FINISHED; break; }
            if (recovering && recovery < 1e-9) { recovery = 0; nextWall(); }
            else if (!recovering && wallTime + 1e-9 >= wallDuration) resolveWall();
        }
    }
    private void resolveWall() {
        feedbackId++;
        if (colors[lane] == targetColor) {
            combo++; passed++; bestCombo = Math.max(bestCombo, combo);
            score += 100 + Math.min(10, combo - 1) * 10;
            feedback = Feedback.HIT;
        } else {
            lives--; combo = 0; feedback = Feedback.MISS;
        }
        wallTime = wallDuration; recovery = RECOVERY;
        if (lives == 0) state = State.FINISHED;
    }
    private void nextWall() {
        targetColor = random.nextInt(3);
        int matchingLane = random.nextInt(2);
        colors[matchingLane] = targetColor;
        colors[1 - matchingLane] = (targetColor + 1 + random.nextInt(2)) % 3;
        wallTime = 0; wallDuration = Math.max(1.1, 2.4 - elapsed * .022);
    }
    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double remaining() { return Math.max(0, DURATION - elapsed); }
    public double wallProgress() { return Math.min(1, wallTime / wallDuration); }
    public double wallDuration() { return wallDuration; }
    public double recoveryRemaining() { return recovery; }
    public int lane() { return lane; }
    public int targetColor() { return targetColor; }
    public int laneColor(int value) {
        if (value < 0 || value > 1) throw new IllegalArgumentException("lane must be 0 or 1");
        return colors[value];
    }
    public int lives() { return lives; }
    public int score() { return score; }
    public int combo() { return combo; }
    public int bestCombo() { return bestCombo; }
    public int passed() { return passed; }
}
