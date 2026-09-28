package com.yamone.arcade2.core;

import java.util.Random;

/** Tap Tap v2: four lanes, one/two-finger chords and endless five-miss rhythm. */
public final class TwinTapEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, HIT, PERFECT, MISS }
    public static final int LANES = 4, MAX_LIVES = 5, MAX_SCORE = 1_000_000_000;
    public static final double HIT_WINDOW = .18, PERFECT_WINDOW = .055;
    public final long seed;
    private final Random random;
    private State state = State.READY, resumeState = State.READY;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, targetTime = 1.6, maxTapError, travelDuration = 1.6, feedbackTime;
    private int noteMask, tappedMask, noteSerial, lives = MAX_LIVES;
    private int score, combo, bestCombo, hits, perfects, simultaneousHits, feedbackId;

    public TwinTapEngine(long seed) {
        this.seed = seed; random = new Random(seed); chooseNote();
    }
    public void start() { if (state == State.READY) state = State.RUNNING; }
    public void tapLane(int lane) {
        if (state != State.RUNNING || lane < 0 || lane >= LANES) return;
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
        double remaining = seconds;
        while (remaining > 1e-9 && state == State.RUNNING) {
            double deadline = targetTime + HIT_WINDOW;
            double untilEvent = Math.max(0, deadline - elapsed);
            double dt = Math.min(remaining, untilEvent);
            elapsed += dt; remaining -= dt;
            feedbackTime = Math.max(0, feedbackTime - dt);
            if (elapsed + 1e-9 >= deadline) resolveMiss();
            else if (dt <= 1e-12) break;
        }
    }
    private void resolveHit() {
        boolean perfect = maxTapError <= PERFECT_WINDOW + 1e-9;
        combo++; bestCombo = Math.max(bestCombo, combo); hits++;
        if (Integer.bitCount(noteMask) == 2) simultaneousHits++;
        if (perfect) perfects++;
        long award = Integer.bitCount(noteMask) * (perfect ? 150 : 100) + Math.min(100L, (combo - 1L) * 10);
        score = (int)Math.min(MAX_SCORE, score + award);
        feedback = perfect ? Feedback.PERFECT : Feedback.HIT; feedbackId++; feedbackTime = .32;
        nextNote();
    }
    private void resolveMiss() {
        lives--; combo = 0; feedback = Feedback.MISS; feedbackId++; feedbackTime = .32;
        if (lives <= 0) finish(); else nextNote();
    }
    private void nextNote() {
        tappedMask = 0; maxTapError = 0; noteSerial++;
        travelDuration = .48 + 1.12 / (1 + noteSerial / 50.0);
        targetTime = elapsed + travelDuration;
        chooseNote();
    }
    private void chooseNote() {
        int first = random.nextInt(LANES);
        noteMask = 1 << first;
        // Never require more than two fingers, even though all four lanes are used.
        if (noteSerial >= 4 && random.nextDouble() < .3)
            noteMask |= 1 << ((first + 1 + random.nextInt(LANES - 1)) % LANES);
    }
    public double travelTime() { return travelDuration; }
    private void finish() { state = State.FINISHED; tappedMask = 0; }

    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double feedbackRemaining() { return feedbackTime; }
    public int level() { return 1 + noteSerial / 10; }
    public boolean inHitWindow() { return state == State.RUNNING && Math.abs(targetOffset()) <= HIT_WINDOW; }
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
