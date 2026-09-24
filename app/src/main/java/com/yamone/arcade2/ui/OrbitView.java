package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import com.yamone.arcade2.core.OrbitEngine;
import java.util.Locale;
import java.util.Random;

public final class OrbitView extends View {
    public interface Listener { void finished(OrbitEngine engine); }
    private final OrbitEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] stars = new float[100];
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int pointer = -1, lastFeedback;
    private static final int INK = 0xFF090E22, CYAN = 0xFF67D9FF, MINT = 0xFF67E7DB, MUTED = 0xFF97A2C8;

    public OrbitView(Context context, OrbitEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setFocusable(true); setClickable(true);
        setContentDescription("오비트 스냅. 화면을 누르면 점이 돌고, 민트 구간에서 손을 떼면 점프합니다.");
        Random random = new Random(42);
        for (int i = 0; i < stars.length; i++) stars[i] = random.nextFloat();
    }
    public OrbitEngine engine() { return engine; }
    public void pauseGame() { engine.pause(); pointer = -1; lastFrame = 0; invalidate(); }
    public void resumeGame() { engine.resume(); lastFrame = 0; invalidate(); }
    public void setForeground(boolean value) {
        foreground = value;
        if (!value) pauseGame();
        else { lastFrame = 0; invalidate(); }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.elapsedRealtimeNanos();
        if (foreground && lastFrame != 0) engine.advance((now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        float scale = getWidth() / 360f;
        if (scale <= 0) return;
        canvas.save(); canvas.scale(scale, scale);
        float height = getHeight() / scale;
        canvas.drawColor(INK);
        for (int i = 0; i < stars.length; i += 2) {
            fill(0xFF334468); canvas.drawCircle(stars[i] * 360, stars[i + 1] * height, i % 3 == 0 ? 1.3f : .7f, paint);
        }
        label(canvas, "SCORE", 24, 29, 10, MUTED, false);
        label(canvas, Integer.toString(engine.score()), 24, 63, 30, Color.WHITE, false);
        label(canvas, String.format(Locale.ROOT, "%02d", (int)Math.ceil(engine.remaining())), 320, 52, 28, Color.WHITE, true);
        label(canvas, "SECONDS", 320, 69, 8, MUTED, true);
        for (int i = 0; i < 3; i++) { fill(i < engine.lives() ? 0xFFFF84AF : 0xFF2D304F); canvas.drawCircle(28 + i * 16, 84, 4, paint); }

        float cx = 180, cy = Math.max(160, height * .48f), radius = Math.min(115, Math.max(60, height * .24f));
        float progress = (float)engine.jumpProgress();
        float stretch = progress < 0 ? 0 : 18 * (float)Math.sin(progress * Math.PI);
        stroke(0xFF182748, 1); canvas.drawCircle(cx, cy, radius + 40, paint);
        stroke(0xFF213B5B, 1); canvas.drawCircle(cx, cy, radius - 30, paint);
        stroke(0xFF304768, 3); canvas.drawCircle(cx, cy, radius + stretch, paint);
        RectF ring = new RectF(cx - radius - stretch, cy - radius - stretch, cx + radius + stretch, cy + radius + stretch);
        stroke(0x334EE6D3, 18); canvas.drawArc(ring, (float)(engine.target() - engine.tolerance()), (float)(engine.tolerance() * 2), false, paint);
        stroke(MINT, 6); canvas.drawArc(ring, (float)(engine.target() - engine.tolerance()), (float)(engine.tolerance() * 2), false, paint);
        float tx = cx + (radius + stretch) * (float)Math.cos(Math.toRadians(engine.target()));
        float ty = cy + (radius + stretch) * (float)Math.sin(Math.toRadians(engine.target()));
        fill(Color.WHITE); canvas.drawCircle(tx, ty, 3, paint);
        float x = cx + (radius + stretch) * (float)Math.cos(Math.toRadians(engine.angle()));
        float y = cy + (radius + stretch) * (float)Math.sin(Math.toRadians(engine.angle()));
        fill(0x2267D9FF); canvas.drawCircle(x, y, 23, paint);
        fill(0x5567D9FF); canvas.drawCircle(x, y, 15, paint);
        fill(Color.WHITE); canvas.drawCircle(x, y, 8, paint);
        label(canvas, "ORBIT", cx, cy - 11, 10, MUTED, true);
        label(canvas, String.format(Locale.ROOT, "%02d", engine.jumps() + 1), cx, cy + 22, 36, Color.WHITE, true);
        float gaugeY = cy + radius + 30;
        stroke(0xFF20304A, 4); canvas.drawLine(125, gaugeY, 235, gaugeY, paint);
        stroke(engine.ringFraction() < .3 ? 0xFFFF84AF : CYAN, 4);
        canvas.drawLine(125, gaugeY, 125 + 110 * (float)engine.ringFraction(), gaugeY, paint);
        String feedback = switch (engine.feedback()) {
            case PERFECT -> "PERFECT! +150";
            case HIT -> "NICE! +100";
            case MISS -> "괜찮아요, 다음 타이밍!";
            default -> "민트 구간에서 손을 떼세요";
        };
        label(canvas, feedback, cx, Math.max(gaugeY + 30, height - 89), 15,
            engine.feedback() == OrbitEngine.Feedback.MISS ? 0xFFFF84AF : MINT, true);
        fill(engine.held() ? 0xFF193B4A : 0xFF152139);
        canvas.drawRoundRect(35, height - 63, 325, height - 13, 25, 25, paint);
        label(canvas, engine.held() ? "타이밍에 맞춰 손 떼기" : "화면 어디든 길게 누르기", cx, height - 33, 14, Color.WHITE, true);

        if (engine.state() == OrbitEngine.State.READY) overlay(canvas, height, "준비됐나요?", "꾹 누르고 돌다가 · 민트에서 떼세요");
        if (engine.state() == OrbitEngine.State.PAUSED) overlay(canvas, height, "잠시 쉬는 중", "화면을 탭하면 이어서 플레이");
        canvas.restore();
        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == OrbitEngine.Feedback.MISS ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == OrbitEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == OrbitEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void overlay(Canvas canvas, float height, String title, String hint) {
        fill(0xCF090E22); canvas.drawRect(0, 0, 360, height, paint);
        label(canvas, title, 180, height * .44f, 25, Color.WHITE, true);
        label(canvas, hint, 180, height * .44f + 37, 12, CYAN, true);
    }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void stroke(int color, float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); }
    private void label(Canvas c, String text, float x, float y, float size, int color, boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextSize(size); paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT);
        c.drawText(text, x, y, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (engine.state() == OrbitEngine.State.PAUSED) { resumeGame(); return true; }
                syncTime(); pointer = event.getPointerId(0); engine.press(); invalidate(); return true;
            case MotionEvent.ACTION_UP:
                if (pointer == event.getPointerId(event.getActionIndex())) {
                    syncTime(); engine.release(); pointer = -1; performClick(); invalidate();
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (pointer == event.getPointerId(event.getActionIndex())) { engine.cancelInput(); pointer = -1; }
                return true;
            case MotionEvent.ACTION_CANCEL:
                engine.cancelInput(); pointer = -1; invalidate(); return true;
            default: return true;
        }
    }
    private void syncTime() {
        long now = SystemClock.elapsedRealtimeNanos();
        if (foreground && lastFrame != 0) engine.advance((now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
