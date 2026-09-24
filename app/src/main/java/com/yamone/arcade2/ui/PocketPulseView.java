package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.PocketPulseEngine;

public final class PocketPulseView extends GameView {
    public interface Listener { void finished(PocketPulseEngine engine); }
    private static final int BG = 0xFF090E22, PURPLE = 0xFFBB99FF, CYAN = 0xFF67D9FF;
    private static final int MINT = 0xFF73E7B1, PINK = 0xFFFF84AF, MUTED = 0xFF9AA5C5;
    private final PocketPulseEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int pointer = -1, lastFeedback;

    public PocketPulseView(Context context, PocketPulseEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setClickable(true); setFocusable(true);
        setContentDescription("포켓 펄스. 중심에서 커지는 파동이 보라색 목표 링과 겹칠 때 화면을 탭하세요.");
    }
    @Override public void pauseGame() { engine.pause(); pointer = -1; lastFrame = 0; invalidate(); }
    @Override public void resumeGame() { engine.resume(); pointer = -1; lastFrame = 0; invalidate(); }
    @Override public void setForeground(boolean value) {
        foreground = value;
        if (!value) pauseGame(); else { lastFrame = 0; invalidate(); }
    }
    private void syncTime() {
        long now = SystemClock.elapsedRealtimeNanos();
        if (foreground && lastFrame != 0) engine.advance((now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); syncTime(); canvas.drawColor(BG);
        float scale = Math.min(getWidth() / 360f, getHeight() / 520f);
        if (scale <= 0) return;
        float offsetX = (getWidth() - 360 * scale) / 2, offsetY = (getHeight() - 520 * scale) / 2;
        canvas.save(); canvas.translate(offsetX, offsetY); canvas.scale(scale, scale);
        label(canvas, "SCORE", 24, 25, 10, MUTED, false);
        label(canvas, Integer.toString(engine.score()), 24, 61, 31, Color.WHITE, false);
        label(canvas, (int)Math.ceil(engine.remaining()) + "초", 318, 46, 24, Color.WHITE, true);
        for (int i = 0; i < 4; i++) { fill(i < engine.lives() ? PURPLE : 0xFF29304A); canvas.drawCircle(27 + 15 * i, 81, 3.8f, paint); }
        label(canvas, engine.combo() + " COMBO · 최고 " + engine.bestCombo(), 180, 106, 12, MUTED, true);

        float cx = 180, cy = 285;
        for (int i = 0; i < 5; i++) {
            stroke(0x152F4770, 1); canvas.drawCircle(cx, cy, 34 + i * 27, paint);
        }
        float target = (float)engine.targetRadius();
        stroke(0x22BB99FF, 18); canvas.drawCircle(cx, cy, target, paint);
        stroke(PURPLE, 4); canvas.drawCircle(cx, cy, target, paint);
        float pulse = (float)engine.radius();
        stroke(0x3367D9FF, 12); canvas.drawCircle(cx, cy, pulse, paint);
        stroke(CYAN, 4); canvas.drawCircle(cx, cy, pulse, paint);
        fill(0x4467D9FF); canvas.drawCircle(cx, cy, 18, paint);
        fill(Color.WHITE); canvas.drawCircle(cx, cy, 7, paint);

        String feedback = switch (engine.feedback()) {
            case PERFECT -> "PERFECT! +200";
            case GREAT -> "GREAT! +150";
            case GOOD -> "GOOD! +100";
            case MISS -> "MISS · 링이 겹칠 때 탭";
            default -> "파동과 목표 링이 겹치는 순간";
        };
        int feedbackColor = switch (engine.feedback()) {
            case MISS -> PINK;
            case PERFECT -> PURPLE;
            default -> MINT;
        };
        label(canvas, feedback, 180, 438, 15, feedbackColor, true);
        if (Double.isFinite(engine.lastError()))
            label(canvas, String.format(java.util.Locale.ROOT, "오차 %.1f", engine.lastError()), 180, 457, 10, MUTED, true);
        fill(0xFF202B46); canvas.drawRoundRect(34, 472, 326, 516, 18, 18, paint);
        label(canvas, engine.waitingNext() ? "다음 파동 준비 중" : "화면 어디든 탭", 180, 500, 14, Color.WHITE, true);
        if (engine.state() == PocketPulseEngine.State.READY) overlay(canvas, "크기가 같아지는 순간", "한 번 탭해 시작");
        if (engine.state() == PocketPulseEngine.State.PAUSED) overlay(canvas, "잠시 쉬는 중", "탭하면 이어서 플레이");
        canvas.restore();

        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == PocketPulseEngine.Feedback.MISS
                ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == PocketPulseEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == PocketPulseEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void overlay(Canvas canvas, String title, String hint) {
        fill(0xDD090E22); canvas.drawRect(0, 0, 360, 520, paint);
        label(canvas, title, 180, 236, 25, Color.WHITE, true);
        label(canvas, hint, 180, 273, 13, PURPLE, true);
    }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void stroke(int color, float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); }
    private void label(Canvas canvas, String text, float x, float y, float size, int color, boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(size);
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT); canvas.drawText(text, x, y, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (!foreground) return true;
        if (action == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (engine.state() == PocketPulseEngine.State.PAUSED) { resumeGame(); return true; }
            syncTime(); pointer = event.getPointerId(0); engine.tap(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_UP && pointer == event.getPointerId(event.getActionIndex())) {
            pointer = -1; performClick(); return true;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP && pointer == event.getPointerId(event.getActionIndex())) {
            pointer = -1; return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
