package com.yamone.arcade2.core;

import java.util.Random;

/** Pure two-lane rhythm rules. Pointer tracking remains in the Android View. */
public final class TwinTapEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, HIT, PERFECT, MISS }
    public static final double DURATION = 60, HIT_WINDOW = .18, PERFECT_WINDOW = .055;
    public final long seed;
    private final Random random;
    private State state = State.READY, resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, targetTime = 1.6, maxTapError;
    private int noteMask, tappedMask, noteSerial, lives = 5;
    private int score, combo, bestCombo, hits, perfects, simultaneousHits, feedbackId;

    public TwinTapEngine(long seed) {
        this.seed = seed; random = new Random(seed); chooseNote();
    }
    public void start() { if (state == State.READY) state = State.RUNNING; }
    public void tapLane(int lane) {
        if (state != State.RUNNING || lane < 0 || lane > 1) return;
        double error = Math.abs(elapsed - targetTime);
        if (error > HIT_WINDOW + 1e-9) return;
        int bit = 1 << lane;
        if ((tappedMask & bit) != 0) return;
        if ((noteMask & bit) == 0) { resolveMiss(); return; }
        tappedMask |= bit; maxTapError = Math.max(maxTapError, error);
        if (tappedMask == noteMask) resolveHit();
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
            double deadline = targetTime + HIT_WINDOW;
            double untilEvent = Math.max(0, deadline - elapsed);
            double untilEnd = DURATION - elapsed;
            double dt = Math.min(remaining, Math.min(untilEvent, untilEnd));
            elapsed += dt; remaining -= dt;
            // The 60-second boundary wins over a note deadline at the same instant.
            if (elapsed + 1e-9 >= DURATION) { elapsed = DURATION; finish(); break; }
            if (elapsed + 1e-9 >= deadline) resolveMiss();
            else if (dt <= 1e-12) break;
        }
    }
    private void resolveHit() {
        boolean perfect = maxTapError <= PERFECT_WINDOW + 1e-9;
        combo++; bestCombo = Math.max(bestCombo, combo); hits++;
        if (noteMask == 3) simultaneousHits++;
        if (perfect) perfects++;
        score += Integer.bitCount(noteMask) * (perfect ? 150 : 100) + Math.min(100, (combo - 1) * 10);
        feedback = perfect ? Feedback.PERFECT : Feedback.HIT; feedbackId++;
        nextNote();
    }
    private void resolveMiss() {
        lives--; combo = 0; feedback = Feedback.MISS; feedbackId++;
        if (lives <= 0) finish(); else nextNote();
    }
    private void nextNote() {
        double priorTarget = targetTime;
        targetTime = Math.max(priorTarget + interval(), elapsed + .5);
        tappedMask = 0; maxTapError = 0; noteSerial++; chooseNote();
    }
    private void chooseNote() {
        // Chords are common enough to exercise two-finger play, but never dominate the rhythm.
        noteMask = noteSerial >= 2 && random.nextDouble() < .3 ? 3 : (random.nextBoolean() ? 1 : 2);
    }
    private double interval() { return Math.max(.72, 1.15 - elapsed * .0065); }
    public double travelTime() { return Math.max(.95, 1.6 - elapsed * .01); }
    private void finish() { state = State.FINISHED; tappedMask = 0; }

    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double remaining() { return Math.max(0, DURATION - elapsed); }
    public double targetOffset() { return targetTime - elapsed; }
    public double noteProgress() {
        return Math.max(0, Math.min(1, 1 - (targetTime - elapsed) / travelTime()));
    }
    public int noteMask() { return noteMask; }
    public int tappedMask() { return tappedMask; }
    public int noteSerial() { return noteSerial; }
    public int lives() { return lives; }
    public int score() { return score; }
    public int combo() { return combo; }
    public int bestCombo() { return bestCombo; }
    public int hits() { return hits; }
    public int perfects() { return perfects; }
    public int simultaneousHits() { return simultaneousHits; }
}
