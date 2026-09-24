package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.SparseIntArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.TwinTapEngine;

public final class TwinTapView extends GameView {
    public interface Listener { void finished(TwinTapEngine engine); }
    private static final int BG = 0xFF090E22, LEFT = 0xFF67D9FF, RIGHT = 0xFFFF84AF;
    private static final int MINT = 0xFF73E7B1, MUTED = 0xFF9AA5C5;
    private final TwinTapEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseIntArray pointerLanes = new SparseIntArray();
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int lastFeedback;
    private float scale, offsetX, offsetY;

    public TwinTapView(Context context, TwinTapEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setClickable(true); setFocusable(true);
        setContentDescription("트윈 탭. 두 레인에서 내려오는 점을 타이밍에 맞춰 한 손가락 또는 두 손가락으로 탭하세요.");
    }
    @Override public void pauseGame() { engine.pause(); pointerLanes.clear(); lastFrame = 0; invalidate(); }
    @Override public void resumeGame() { engine.resume(); pointerLanes.clear(); lastFrame = 0; invalidate(); }
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
        scale = Math.min(getWidth() / 360f, getHeight() / 520f);
        if (scale <= 0) return;
        offsetX = (getWidth() - 360 * scale) / 2; offsetY = (getHeight() - 520 * scale) / 2;
        canvas.save(); canvas.translate(offsetX, offsetY); canvas.scale(scale, scale);
        label(canvas, "SCORE", 24, 25, 10, MUTED, false);
        label(canvas, Integer.toString(engine.score()), 24, 61, 31, Color.WHITE, false);
        label(canvas, (int)Math.ceil(engine.remaining()) + "초", 318, 46, 24, Color.WHITE, true);
        for (int i = 0; i < 5; i++) { fill(i < engine.lives() ? MINT : 0xFF29304A); canvas.drawCircle(26 + 14 * i, 81, 3.7f, paint); }
        label(canvas, "내려오는 점이 선에 닿을 때 탭", 180, 109, 13, MUTED, true);

        for (int lane = 0; lane < 2; lane++) {
            float left = 24 + lane * 162;
            fill(lanePressed(lane) ? 0xFF203552 : 0xFF121A30);
            canvas.drawRoundRect(left, 125, left + 150, 474, 17, 17, paint);
            stroke(lane == 0 ? 0x5567D9FF : 0x55FF84AF, 2);
            canvas.drawLine(left + 75, 135, left + 75, 421, paint);
        }
        stroke(0xFF617190, 3); canvas.drawLine(29, 397, 331, 397, paint);
        fill(0x2267D9FF); canvas.drawCircle(99, 397, 29, paint);
        fill(0x22FF84AF); canvas.drawCircle(261, 397, 29, paint);
        stroke(LEFT, 3); canvas.drawCircle(99, 397, 22, paint);
        stroke(RIGHT, 3); canvas.drawCircle(261, 397, 22, paint);

        float noteY = 143 + 254 * (float)engine.noteProgress();
        drawNote(canvas, 0, noteY, LEFT);
        drawNote(canvas, 1, noteY, RIGHT);
        String feedback = switch (engine.feedback()) {
            case PERFECT -> "PERFECT!  " + engine.combo() + " COMBO";
            case HIT -> "NICE!  " + engine.combo() + " COMBO";
            case MISS -> "MISS · 다시 리듬을 잡아요";
            default -> "한 점은 한 손가락 · 두 점은 동시에";
        };
        label(canvas, feedback, 180, 447, 14,
            engine.feedback() == TwinTapEngine.Feedback.MISS ? RIGHT : MINT, true);
        for (int lane = 0; lane < 2; lane++) {
            int color = lane == 0 ? LEFT : RIGHT;
            fill(lanePressed(lane) ? color : 0xFF202B46);
            canvas.drawRoundRect(27 + lane * 162, 477, 171 + lane * 162, 515, 14, 14, paint);
            label(canvas, lane == 0 ? "왼쪽 탭" : "오른쪽 탭", 99 + lane * 162, 502, 13,
                lanePressed(lane) ? BG : Color.WHITE, true);
        }
        if (engine.state() == TwinTapEngine.State.READY)
            overlay(canvas, "두 리듬을 잡아요", "한 손가락으로 탭해 시작");
        if (engine.state() == TwinTapEngine.State.PAUSED)
            overlay(canvas, "잠시 쉬는 중", "탭하면 이어서 플레이");
        canvas.restore();

        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == TwinTapEngine.Feedback.MISS
                ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == TwinTapEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == TwinTapEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void drawNote(Canvas canvas, int lane, float y, int color) {
        int bit = 1 << lane;
        if ((engine.noteMask() & bit) == 0) return;
        float x = 99 + lane * 162;
        boolean tapped = (engine.tappedMask() & bit) != 0;
        fill(tapped ? 0x5573E7B1 : color); canvas.drawCircle(x, y, tapped ? 13 : 17, paint);
        stroke(Color.WHITE, 2); canvas.drawCircle(x, y, tapped ? 13 : 17, paint);
        if (engine.noteMask() == 3) label(canvas, "2", x, y + 5, 12, BG, true);
    }
    private boolean lanePressed(int lane) {
        for (int i = 0; i < pointerLanes.size(); i++) if (pointerLanes.valueAt(i) == lane) return true;
        return false;
    }
    private void overlay(Canvas canvas, String title, String hint) {
        fill(0xDD090E22); canvas.drawRect(0, 0, 360, 520, paint);
        label(canvas, title, 180, 236, 25, Color.WHITE, true);
        label(canvas, hint, 180, 273, 13, LEFT, true);
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
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int index = event.getActionIndex(), pointerId = event.getPointerId(index);
            if (engine.state() == TwinTapEngine.State.PAUSED) { resumeGame(); return true; }
            if (engine.state() == TwinTapEngine.State.READY) {
                engine.start(); pointerLanes.put(pointerId, event.getX(index) < getWidth() / 2f ? 0 : 1);
                lastFrame = SystemClock.elapsedRealtimeNanos(); invalidate(); return true;
            }
            if (pointerLanes.indexOfKey(pointerId) < 0) {
                syncTime(); int lane = event.getX(index) < getWidth() / 2f ? 0 : 1;
                pointerLanes.put(pointerId, lane); engine.tapLane(lane); invalidate();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            pointerLanes.delete(event.getPointerId(event.getActionIndex()));
            if (action == MotionEvent.ACTION_UP) performClick();
            invalidate();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            pointerLanes.clear(); invalidate();
        }
        // MOVE never creates another rhythm hit; each physical pointer-down is consumed once.
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
