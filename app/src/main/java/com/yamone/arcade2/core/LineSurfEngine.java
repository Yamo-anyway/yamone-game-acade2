package com.yamone.arcade2.core;

import java.util.Random;

/** Endless-line survival rules. Coordinates are world pixels and active-play seconds. */
public final class LineSurfEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Hazard { GAP, OBSTACLE }
    public enum Feedback { NONE, CLEAR, CRASH }
    public static final double DURATION = 60;
    private static final double JUMP_SPEED = 285, GRAVITY = 520;
    public final long seed;
    private final Random random;
    private State state = State.READY, resumeState = State.READY;
    private Hazard hazard;
    private Feedback feedback = Feedback.NONE;
    private double elapsed, distance, height, velocity, hazardStart, hazardWidth, obstacleHeight, invulnerable;
    private int lives = 3, jumps, cleared, crashes, combo, bestCombo, hazardSerial, feedbackId;
    private boolean held, airborne;

    public LineSurfEngine(long seed) {
        this.seed = seed; random = new Random(seed); hazardStart = 330; chooseHazard();
    }
    public void press() {
        if (state == State.READY) state = State.RUNNING;
        if (state == State.RUNNING) held = true;
    }
    public void release() {
        if (state != State.RUNNING || !held) return;
        held = false;
        if (!airborne) {
            airborne = true; velocity = JUMP_SPEED; jumps++;
        }
    }
    public void cancelInput() { held = false; }
    public void pause() {
        if (state == State.READY || state == State.RUNNING) {
            resumeState = state; state = State.PAUSED; held = false;
        }
    }
    public void resume() { if (state == State.PAUSED) state = resumeState; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || state != State.RUNNING) return;
        double remaining = Math.min(seconds, DURATION - elapsed);
        // Small deterministic substeps keep jump/collision behavior stable across display refresh rates.
        while (remaining > 1e-9 && state == State.RUNNING) {
            double dt = Math.min(.002, remaining);
            remaining -= dt; elapsed += dt;
            if (airborne) {
                height += velocity * dt; velocity -= GRAVITY * dt;
                if (height <= 0) { height = 0; velocity = 0; airborne = false; }
            }
            distance += speed() * dt;
            invulnerable = Math.max(0, invulnerable - dt);
            // Round completion wins over a collision on the same simulation instant.
            if (elapsed + 1e-9 >= DURATION) { elapsed = DURATION; finish(); break; }
            boolean inside = distance + 1e-9 >= hazardStart && distance <= hazardStart + hazardWidth + 1e-9;
            if (inside && invulnerable <= 0) {
                boolean collision = hazard == Hazard.GAP ? !airborne : height < obstacleHeight;
                if (collision) crash();
            }
            if (state == State.RUNNING && distance > hazardStart + hazardWidth + 1e-9) clearHazard();
        }
    }
    private void crash() {
        lives--; crashes++; combo = 0; feedback = Feedback.CRASH; feedbackId++;
        if (lives <= 0) { finish(); return; }
        // Move the failed feature behind the rider and grant a short safe recovery arc.
        airborne = true; height = 38; velocity = 30; invulnerable = .65;
        scheduleNext(false);
    }
    private void clearHazard() {
        cleared++; combo++; bestCombo = Math.max(bestCombo, combo);
        feedback = Feedback.CLEAR; feedbackId++; scheduleNext(true);
    }
    private void scheduleNext(boolean fromClear) {
        double priorEnd = hazardStart + hazardWidth;
        hazardStart = Math.max(distance + (fromClear ? 250 : 290), priorEnd + 250) + random.nextDouble() * 105;
        hazardSerial++; chooseHazard();
    }
    private void chooseHazard() {
        hazard = random.nextBoolean() ? Hazard.GAP : Hazard.OBSTACLE;
        if (hazard == Hazard.GAP) {
            hazardWidth = 58 + random.nextDouble() * 32; obstacleHeight = 0;
        } else {
            hazardWidth = 34 + random.nextDouble() * 22; obstacleHeight = 35 + random.nextDouble() * 15;
        }
    }
    private void finish() { state = State.FINISHED; held = false; }

    public State state() { return state; }
    public Hazard hazard() { return hazard; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double elapsed() { return elapsed; }
    public double remaining() { return Math.max(0, DURATION - elapsed); }
    public double distance() { return distance; }
    public int distanceMeters() { return (int)Math.floor(distance / 10); }
    public double speed() { return 155 + elapsed * 1.35; }
    public double height() { return height; }
    public double velocity() { return velocity; }
    public boolean held() { return held; }
    public boolean airborne() { return airborne; }
    public double hazardStart() { return hazardStart; }
    public double hazardWidth() { return hazardWidth; }
    public double obstacleHeight() { return obstacleHeight; }
    public double hazardDistance() { return hazardStart - distance; }
    public int hazardSerial() { return hazardSerial; }
    public int lives() { return lives; }
    public int jumps() { return jumps; }
    public int cleared() { return cleared; }
    public int crashes() { return crashes; }
    public int combo() { return combo; }
    public int bestCombo() { return bestCombo; }
    public int score() { return distanceMeters() + cleared * 100; }
}
