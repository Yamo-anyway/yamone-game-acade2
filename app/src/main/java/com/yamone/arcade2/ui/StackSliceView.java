package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.StackSliceEngine;

public final class StackSliceView extends GameView {
    public interface Listener { void finished(StackSliceEngine engine); }
    private static final int BG = 0xFF090E22, MINT = 0xFF67E7DB, CYAN = 0xFF67D9FF;
    private static final int PINK = 0xFFFF84AF, GOLD = 0xFFFFB96B, MUTED = 0xFF9AA5C5;
    private final StackSliceEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int pointer = -1, lastFeedback;
    private float downX, downY;

    public StackSliceView(Context context, StackSliceEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setClickable(true); setFocusable(true);
        setContentDescription("스택 슬라이스. 움직이는 블록에서 잘라낼 쪽으로 좌우 스와이프해 탑의 균형을 지키세요.");
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
        label(canvas, "적층 " + engine.placed() + " · 균형 연속 " + engine.balanceStreak(), 180, 91, 12, MUTED, true);

        float towerX = 180, floorY = 418, blockH = 18;
        fill(0xFF202B46); canvas.drawRoundRect(70, floorY + 4, 290, floorY + 17, 6, 6, paint);
        int visible = Math.min(15, engine.layerCount());
        int first = engine.layerCount() - visible;
        for (int i = first; i < engine.layerCount(); i++) {
            float y = floorY - (i - first + 1) * blockH;
            float center = towerX + (float)engine.layerCenter(i);
            float width = (float)engine.layerWidth(i);
            fill(i == engine.layerCount() - 1 ? MINT : (i % 2 == 0 ? 0xFF347A80 : 0xFF295B70));
            canvas.drawRoundRect(center - width / 2, y, center + width / 2, y + blockH - 2, 4, 4, paint);
        }
        if (!engine.waitingNext() && engine.state() != StackSliceEngine.State.FINISHED) {
            float y = floorY - (visible + 1) * blockH - 14;
            float center = towerX + (float)engine.incomingCenter();
            float width = (float)engine.incomingWidth();
            fill(0xFF67D9FF); canvas.drawRoundRect(center - width / 2, y, center + width / 2, y + blockH - 2, 4, 4, paint);
            float halfCut = (float)StackSliceEngine.CUT_WIDTH / 2;
            stroke(0x88FFFFFF, 1.5f); canvas.drawLine(center - halfCut, y, center - halfCut, y + blockH - 2, paint);
            canvas.drawLine(center + halfCut, y, center + halfCut, y + blockH - 2, paint);
        }

        float tilt = (float)Math.max(-1, Math.min(1, engine.tilt()));
        stroke(0xFF29304A, 7); canvas.drawLine(115, 446, 245, 446, paint);
        fill(Math.abs(tilt) < .6 ? MINT : PINK); canvas.drawCircle(180 + tilt * 62, 446, 7, paint);
        label(canvas, "기울기", 180, 469, 10, MUTED, true);

        String feedback = switch (engine.feedback()) {
            case BALANCED -> "BALANCED! 중심 유지";
            case WOBBLE -> "흔들려요 · 반대쪽을 정리하세요";
            case FALL -> "무게중심을 잃었어요";
            case TIMEOUT -> "시간 초과 · 블록이 무너졌어요";
            default -> "튀어나온 쪽으로 스와이프해 잘라내기";
        };
        int feedbackColor = switch (engine.feedback()) {
            case FALL, TIMEOUT -> PINK;
            case WOBBLE -> GOLD;
            default -> MINT;
        };
        label(canvas, feedback, 180, 491, 12, feedbackColor, true);
        label(canvas, engine.waitingNext() ? "다음 블록 준비 중" : "← 왼쪽 절단     오른쪽 절단 →", 180, 513, 11, Color.WHITE, true);
        if (engine.state() == StackSliceEngine.State.READY) overlay(canvas, "잘라내고, 중심을 지켜요", "좌우로 밀어 시작");
        if (engine.state() == StackSliceEngine.State.PAUSED) overlay(canvas, "잠시 쉬는 중", "탭하면 이어서 플레이");
        canvas.restore();

        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == StackSliceEngine.Feedback.BALANCED
                ? HapticFeedbackConstants.CLOCK_TICK : HapticFeedbackConstants.LONG_PRESS);
        }
        if (engine.state() == StackSliceEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == StackSliceEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void overlay(Canvas canvas, String title, String hint) {
        fill(0xDD090E22); canvas.drawRect(0, 0, 360, 520, paint);
        label(canvas, title, 180, 236, 24, Color.WHITE, true);
        label(canvas, hint, 180, 273, 13, MINT, true);
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
            if (engine.state() == StackSliceEngine.State.PAUSED) { resumeGame(); return true; }
            syncTime(); pointer = event.getPointerId(0); downX = event.getX(); downY = event.getY();
            engine.start(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_UP && pointer == event.getPointerId(event.getActionIndex())) {
            syncTime();
            float dx = event.getX(event.getActionIndex()) - downX, dy = event.getY(event.getActionIndex()) - downY;
            if (Math.abs(dx) >= 36 * getResources().getDisplayMetrics().density && Math.abs(dx) > Math.abs(dy) * 1.2f)
                engine.swipe(dx < 0 ? -1 : 1);
            pointer = -1; performClick(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP && pointer == event.getPointerId(event.getActionIndex())) {
            pointer = -1; return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
