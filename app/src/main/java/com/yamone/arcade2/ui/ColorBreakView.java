package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.ColorBreakEngine;

public final class ColorBreakView extends GameView {
    public interface Listener { void finished(ColorBreakEngine engine); }
    private static final int BG = 0xFF090E22, MUTED = 0xFF9AA5C5;
    private static final int[] COLORS = {0xFF67D9FF, 0xFFFF84AF, 0xFFFFC875};
    private final ColorBreakEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int lastFeedback;
    private float scale, offsetX, offsetY;

    public ColorBreakView(Context context, ColorBreakEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setClickable(true); setFocusable(true);
        setContentDescription("컬러 브레이크. 왼쪽 또는 오른쪽을 탭해 같은 색과 숫자의 벽을 통과하세요.");
    }
    public void pauseGame() { engine.pause(); lastFrame = 0; invalidate(); }
    public void resumeGame() { engine.resume(); lastFrame = 0; invalidate(); }
    public void setForeground(boolean value) {
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
        // Fit the complete board in both dimensions; letterbox instead of clipping controls.
        scale = Math.min(getWidth() / 360f, getHeight() / 520f);
        if (scale <= 0) return;
        offsetX = (getWidth() - 360 * scale) / 2; offsetY = (getHeight() - 520 * scale) / 2;
        canvas.save(); canvas.translate(offsetX, offsetY); canvas.scale(scale, scale);
        label(canvas, "SCORE", 24, 25, 10, MUTED, false);
        label(canvas, Integer.toString(engine.score()), 24, 61, 31, Color.WHITE, false);
        label(canvas, (int)Math.ceil(engine.remaining()) + "초", 318, 46, 24, Color.WHITE, true);
        for (int i = 0; i < 3; i++) { fill(i < engine.lives() ? COLORS[1] : 0xFF29304A); canvas.drawCircle(29 + 16 * i, 80, 4, paint); }
        label(canvas, "같은 색 + 숫자로 이동", 180, 111, 13, MUTED, true);
        for (int lane = 0; lane < 2; lane++) {
            fill(engine.lane() == lane ? 0xFF1C2945 : 0xFF121A30);
            canvas.drawRoundRect(22 + lane * 162, 130, 176 + lane * 162, 432, 16, 16, paint);
        }
        fill(0xFF546382); canvas.drawRect(25, 204, 335, 206, paint);
        float wallY = 418 - 213 * (float)engine.wallProgress();
        if (engine.recoveryRemaining() > 0) wallY -= 64 * (float)(1 - engine.recoveryRemaining() / ColorBreakEngine.RECOVERY);
        for (int lane = 0; lane < 2; lane++) {
            int color = engine.laneColor(lane);
            fill(COLORS[color]); canvas.drawRoundRect(27 + lane * 162, wallY - 14, 171 + lane * 162, wallY + 14, 8, 8, paint);
            label(canvas, Integer.toString(color + 1), 99 + lane * 162, wallY + 6, 17, BG, true);
        }
        float playerX = 99 + engine.lane() * 162;
        fill(Color.WHITE); canvas.drawCircle(playerX, 205, 21, paint);
        fill(COLORS[engine.targetColor()]); canvas.drawCircle(playerX, 205, 17, paint);
        label(canvas, Integer.toString(engine.targetColor() + 1), playerX, 211, 18, BG, true);
        String feedback = engine.recoveryRemaining() > 0
            ? (engine.feedback() == ColorBreakEngine.Feedback.MISS ? "색이 달라요 · 다시 도전!" : engine.combo() + " COMBO!")
            : "연속 " + engine.combo() + "회 · 최고 " + engine.bestCombo() + "회";
        label(canvas, feedback, 180, 460, 15, COLORS[1], true);
        for (int lane = 0; lane < 2; lane++) {
            fill(0xFF202B46); canvas.drawRoundRect(25 + lane * 162, 479, 173 + lane * 162, 516, 14, 14, paint);
            label(canvas, lane == 0 ? "← 왼쪽 탭" : "오른쪽 탭 →", 99 + lane * 162, 503, 13, Color.WHITE, true);
        }
        if (engine.state() == ColorBreakEngine.State.READY) overlay(canvas, "같은 색을 찾아요", "왼쪽 / 오른쪽 탭으로 시작");
        if (engine.state() == ColorBreakEngine.State.PAUSED) overlay(canvas, "잠시 쉬는 중", "탭하면 이어서 플레이");
        canvas.restore();
        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == ColorBreakEngine.Feedback.MISS
                ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == ColorBreakEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == ColorBreakEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void overlay(Canvas canvas, String title, String hint) {
        fill(0xDD090E22); canvas.drawRect(0, 0, 360, 520, paint);
        label(canvas, title, 180, 236, 25, Color.WHITE, true);
        label(canvas, hint, 180, 273, 13, COLORS[0], true);
    }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void label(Canvas canvas, String text, float x, float y, float size, int color, boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(size);
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT); canvas.drawText(text, x, y, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!foreground) return true;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (engine.state() == ColorBreakEngine.State.PAUSED) { resumeGame(); return true; }
            // Only the initial pointer chooses a lane. Holding, moving and extra fingers do not repeat taps.
            syncTime(); engine.tapLane(event.getX() < getWidth() / 2f ? 0 : 1); invalidate();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
