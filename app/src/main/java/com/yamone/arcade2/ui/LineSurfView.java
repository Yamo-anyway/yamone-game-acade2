package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.LineSurfEngine;

public final class LineSurfView extends GameView {
    public interface Listener { void finished(LineSurfEngine engine); }
    private static final int BG = 0xFF090E22, ORANGE = 0xFFFFB96B, CYAN = 0xFF67D9FF;
    private static final int MINT = 0xFF73E7B1, MUTED = 0xFF9AA5C5;
    private final LineSurfEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int pointer = -1, lastFeedback;

    public LineSurfView(Context context, LineSurfEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setClickable(true); setFocusable(true);
        setContentDescription("라인 서프. 화면을 누르면 선을 타고, 손을 떼면 점프해 틈과 장애물을 넘습니다.");
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
        label(canvas, "DISTANCE", 24, 25, 10, MUTED, false);
        label(canvas, engine.distanceMeters() + "m", 24, 61, 31, Color.WHITE, false);
        label(canvas, (int)Math.ceil(engine.remaining()) + "초", 318, 46, 24, Color.WHITE, true);
        for (int i = 0; i < 3; i++) { fill(i < engine.lives() ? ORANGE : 0xFF29304A); canvas.drawCircle(27 + 16 * i, 81, 4, paint); }
        label(canvas, "통과 " + engine.cleared() + " · 최고 연속 " + engine.bestCombo(), 180, 105, 12, MUTED, true);

        // Parallax skyline is tied only to distance, so redraws do not affect gameplay.
        fill(0xFF101A31); canvas.drawRect(0, 250, 360, 390, paint);
        Path hills = new Path(); hills.moveTo(0, 330);
        float shift = (float)(engine.distance() * .08 % 120);
        for (int i = -1; i < 5; i++) {
            float x = i * 120 - shift; hills.lineTo(x + 60, 270 + (i & 1) * 18); hills.lineTo(x + 120, 330);
        }
        hills.lineTo(360, 390); hills.lineTo(0, 390); hills.close(); fill(0xFF172442); canvas.drawPath(hills, paint);

        float lineY = 365, playerX = 88;
        float hazardX = playerX + (float)engine.hazardDistance();
        float hazardEnd = hazardX + (float)engine.hazardWidth();
        stroke(0xFF536487, 3);
        if (engine.hazard() == LineSurfEngine.Hazard.GAP) {
            canvas.drawLine(0, lineY, Math.max(0, hazardX), lineY, paint);
            canvas.drawLine(Math.min(360, hazardEnd), lineY, 360, lineY, paint);
            if (hazardX < 360 && hazardEnd > 0) {
                stroke(0x44FFB96B, 2); canvas.drawLine(Math.max(0, hazardX), lineY + 18, Math.min(360, hazardEnd), lineY + 18, paint);
            }
        } else {
            canvas.drawLine(0, lineY, 360, lineY, paint);
            if (hazardX < 360 && hazardEnd > 0) {
                fill(ORANGE); canvas.drawRoundRect(hazardX, lineY - (float)engine.obstacleHeight(), hazardEnd, lineY, 5, 5, paint);
                stroke(Color.WHITE, 2); canvas.drawRoundRect(hazardX, lineY - (float)engine.obstacleHeight(), hazardEnd, lineY, 5, 5, paint);
            }
        }
        float playerY = lineY - (float)engine.height();
        stroke(0x4467D9FF, 6); canvas.drawLine(35, playerY, playerX - 12, playerY, paint);
        fill(0x4467D9FF); canvas.drawCircle(playerX, playerY, 21, paint);
        fill(CYAN); canvas.drawCircle(playerX, playerY, 12, paint);
        fill(Color.WHITE); canvas.drawCircle(playerX + 3, playerY - 3, 4, paint);

        String feedback = switch (engine.feedback()) {
            case CLEAR -> "CLEAR!  " + engine.combo() + " COMBO";
            case CRASH -> "부딪혔어요 · 다시 균형!";
            default -> engine.airborne() ? "착지할 선을 확인하세요" : "장애물 앞에서 손을 떼세요";
        };
        label(canvas, feedback, 180, 424, 14,
            engine.feedback() == LineSurfEngine.Feedback.CRASH ? 0xFFFF84AF : MINT, true);
        fill(engine.held() ? ORANGE : 0xFF202B46); canvas.drawRoundRect(34, 461, 326, 514, 22, 22, paint);
        label(canvas, engine.held() ? "누르는 중 · 선을 타기" : "누르고 있다가 · 손 떼면 점프", 180, 494, 14,
            engine.held() ? BG : Color.WHITE, true);
        if (engine.state() == LineSurfEngine.State.READY) overlay(canvas, "선을 타볼까요?", "꾹 눌러 시작 · 장애물 앞에서 떼기");
        if (engine.state() == LineSurfEngine.State.PAUSED) overlay(canvas, "잠시 쉬는 중", "탭하면 이어서 플레이");
        canvas.restore();

        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == LineSurfEngine.Feedback.CRASH
                ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == LineSurfEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == LineSurfEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void overlay(Canvas canvas, String title, String hint) {
        fill(0xDD090E22); canvas.drawRect(0, 0, 360, 520, paint);
        label(canvas, title, 180, 236, 25, Color.WHITE, true);
        label(canvas, hint, 180, 273, 13, ORANGE, true);
    }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void stroke(int color, float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); }
    private void label(Canvas canvas, String text, float x, float y, float size, int color, boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); paint.setTextSize(size);
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT); canvas.drawText(text, x, y, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (!foreground) return true;
        if (action == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (engine.state() == LineSurfEngine.State.PAUSED) { resumeGame(); return true; }
            syncTime(); pointer = event.getPointerId(0); engine.press(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_UP && pointer == event.getPointerId(event.getActionIndex())) {
            syncTime(); engine.release(); pointer = -1; performClick(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP && pointer == event.getPointerId(event.getActionIndex())) {
            engine.cancelInput(); pointer = -1; invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) { engine.cancelInput(); pointer = -1; invalidate(); return true; }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
