package com.yamone.arcade2.core;

import java.util.ArrayList;
import java.util.Random;

/** Endless, staggered concentric pulses, independent from Android drawing and clocks. */
public final class PocketPulseEngine {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    public enum Feedback { NONE, GOOD, GREAT, PERFECT, MISS }
    public static final double MIN_RADIUS=12, TARGET_RADIUS=108, GOOD_WINDOW=8, RECOVERY=.07;
    public static final int MAX_SCORE=1_000_000_000;
    private static final double PERFECT_WINDOW=2, GREAT_WINDOW=5;
    private static final class Pulse {
        double radius=MIN_RADIUS;
        final int color;
        Pulse(int color) { this.color=color; }
    }
    public final long seed;
    private final Random random;
    private final ArrayList<Pulse> pulses=new ArrayList<>();
    private State state=State.READY, resumeState=State.READY;
    private Feedback feedback=Feedback.NONE;
    private double elapsed, spawnIn, recovery, feedbackRemaining, lastError=Double.NaN;
    private int lives=5, score, combo, bestCombo, hits, goods, greats, perfects, misses, pulseSerial, feedbackId, lastColor=-1;

    public PocketPulseEngine(long seed) { this.seed=seed; random=new Random(seed); spawn(); }
    public void start() { if (state==State.READY) state=State.RUNNING; }
    public void tap() {
        if (state==State.READY) { start(); return; }
        if (state!=State.RUNNING || recovery>1e-9 || pulses.isEmpty()) return;
        lastError=Math.abs(radius()-TARGET_RADIUS);
        resolve(lastError<=GOOD_WINDOW+1e-9);
    }
    public void pause() {
        if (state==State.READY || state==State.RUNNING) { resumeState=state; state=State.PAUSED; }
    }
    public void resume() { if (state==State.PAUSED) state=resumeState; }
    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds<=0 || state!=State.RUNNING) return;
        double remaining=seconds;
        while (remaining>1e-9 && state==State.RUNNING) {
            double dt=Math.min(.002,remaining); remaining-=dt; elapsed+=dt;
            recovery=Math.max(0,recovery-dt); feedbackRemaining=Math.max(0,feedbackRemaining-dt);
            double movement=speed()*dt;
            for (Pulse pulse:pulses) pulse.radius+=movement;
            if (!pulses.isEmpty() && radius()>TARGET_RADIUS+GOOD_WINDOW+1e-9) {
                lastError=radius()-TARGET_RADIUS; resolve(false);
            }
            if (state!=State.RUNNING) break;
            spawnIn-=dt;
            if (spawnIn<=1e-9 && pulses.size()<maxCircles()) spawn();
        }
    }
    private void resolve(boolean hit) {
        if (hit) {
            combo++; bestCombo=Math.max(bestCombo,combo); hits++;
            int base;
            if (lastError<=PERFECT_WINDOW+1e-9) { feedback=Feedback.PERFECT; perfects++; base=200; }
            else if (lastError<=GREAT_WINDOW+1e-9) { feedback=Feedback.GREAT; greats++; base=150; }
            else { feedback=Feedback.GOOD; goods++; base=100; }
            score=(int)Math.min(MAX_SCORE,(long)score+base+Math.min(100L,(long)(combo-1)*10));
        } else { lives--; misses++; combo=0; feedback=Feedback.MISS; }
        pulses.remove(0); pulseSerial++; feedbackId++; feedbackRemaining=.32; recovery=RECOVERY;
        if (lives==0) { state=State.FINISHED; recovery=0; return; }
        if (pulses.isEmpty()) spawnIn=RECOVERY;
        else spawnIn=Math.min(spawnIn,spawnInterval());
    }
    private void spawn() {
        int blocked=lastColor<0?0:1<<lastColor;
        for (Pulse pulse:pulses) blocked|=1<<pulse.color;
        int choice=random.nextInt(5-Integer.bitCount(blocked)), color=0;
        for (;color<5;color++) if ((blocked&(1<<color))==0 && choice--==0) break;
        pulses.add(new Pulse(color)); lastColor=color; spawnIn=spawnInterval();
    }
    public State state() { return state; }
    public Feedback feedback() { return feedback; }
    public int feedbackId() { return feedbackId; }
    public double feedbackRemaining() { return feedbackRemaining; }
    public double elapsed() { return elapsed; }
    public double radius() { return pulses.isEmpty()?MIN_RADIUS:pulses.get(0).radius; }
    public double radius(int index) { return pulses.get(index).radius; }
    public int color(int index) { return pulses.get(index).color; }
    public int pulseCount() { return pulses.size(); }
    public double targetRadius() { return TARGET_RADIUS; }
    public double error() { return Math.abs(radius()-TARGET_RADIUS); }
    public double lastError() { return lastError; }
    public double speed() { return 60+72.0*pulseSerial/(pulseSerial+80.0); }
    public double spawnInterval() { return (TARGET_RADIUS-MIN_RADIUS)/speed()/maxCircles(); }
    public double timeToTarget() { return pulses.isEmpty()?0:Math.max(0,(TARGET_RADIUS-radius())/speed()); }
    public boolean inWindow() { return !pulses.isEmpty() && error()<=GOOD_WINDOW+1e-9; }
    public boolean waitingNext() { return pulses.isEmpty() || recovery>1e-9; }
    public double recoveryRemaining() { return recovery; }
    public int maxCircles() { return pulseSerial<12?1:pulseSerial<28?2:pulseSerial<48?3:pulseSerial<72?4:5; }
    public int level() { return 1+pulseSerial/12; }
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
