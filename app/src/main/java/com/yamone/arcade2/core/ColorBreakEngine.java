package com.yamone.arcade2.core;

import java.util.Arrays;
import java.util.Random;

/** Rules v2: four shuffled colors, endless active play, five misses. No Android state. */
public final class ColorBreakEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, HIT, MISS }
    public static final int LANES = 4, MAX_LIVES = 5, MAX_SCORE = 1_000_000_000;
    public static final double RECOVERY = .10;
    public final long seed;
    private final Random random;
    private final int[] colors = new int[LANES];
    private State state = State.READY, resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, wallTime, wallDuration, recovery;
    private int lane, targetColor = -1, lives = MAX_LIVES, score, combo, bestCombo, passed, feedbackId;
    private boolean armed;

    public ColorBreakEngine(long seed) { this.seed = seed; random = new Random(seed); nextWall(); }
    public void tapLane(int value) {
        if (value < 0 || value >= LANES || recovery > 0) return;
        if (state == State.READY) state = State.RUNNING;
        if (state == State.RUNNING) { lane = value; armed = true; }
    }
    public void pause() {
        if (state == State.READY || state == State.RUNNING) { resumeState = state; state = State.PAUSED; }
    }
    public void resume() { if (state == State.PAUSED) state = resumeState; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || state != State.RUNNING) return;
        double remaining = seconds;
        // Exact crossings survive delayed frames. Unplayed walls miss, so a huge
        // delta resolves at most one hit and five misses instead of looping forever.
        while (remaining > 1e-9 && state == State.RUNNING) {
            boolean recovering = recovery > 0;
            double untilEvent = recovering ? recovery : wallDuration - wallTime;
            double dt = Math.min(remaining, untilEvent);
            elapsed += dt; remaining -= dt;
            if (recovering) recovery = Math.max(0, recovery - dt);
            else wallTime += dt;
            if (recovering && recovery < 1e-9) { recovery = 0; nextWall(); }
            else if (!recovering && wallTime + 1e-9 >= wallDuration) resolveWall();
        }
    }
    private void resolveWall() {
        feedbackId++;
        if (armed && colors[lane] == targetColor) {
            combo++; passed++; bestCombo = Math.max(bestCombo, combo);
            score = (int)Math.min(MAX_SCORE, (long)score + 100 + Math.min(10, combo - 1) * 10);
            feedback = Feedback.HIT;
        } else {
            lives--; combo = 0; feedback = Feedback.MISS;
        }
        wallTime = wallDuration; recovery = RECOVERY; armed = false;
        if (lives == 0) state = State.FINISHED;
    }
    private void nextWall() {
        int[] previous = colors.clone();
        for (int i = 0; i < LANES; i++) colors[i] = i;
        for (int i = LANES - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), temporary = colors[i]; colors[i] = colors[j]; colors[j] = temporary;
        }
        if (Arrays.equals(previous, colors)) { int first = colors[0]; colors[0] = colors[1]; colors[1] = first; }
        targetColor = targetColor < 0 ? random.nextInt(LANES) : (targetColor + 1 + random.nextInt(LANES - 1)) % LANES;
        wallTime = 0;
        // 1.85s initially, .725s at 90 walls, approaching .35s smoothly.
        wallDuration = .35 + 1.5 / (1 + feedbackId / 30.0);
        armed = false;
    }
    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double wallProgress() { return Math.min(1, wallTime / wallDuration); }
    public double wallDuration() { return wallDuration; }
    public double recoveryRemaining() { return recovery; }
    public double speedMultiplier() { return 1.85 / wallDuration; }
    public int level() { return 1 + feedbackId / 10; }
    public int lane() { return lane; }
    public boolean armed() { return armed; }
    public int targetColor() { return targetColor; }
    public int laneColor(int value) {
        if (value < 0 || value >= LANES) throw new IllegalArgumentException("lane must be 0..3");
        return colors[value];
    }
    public int lives() { return lives; }
    public int score() { return score; }
    public int combo() { return combo; }
    public int bestCombo() { return bestCombo; }
    public int passed() { return passed; }
}
