package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.OrbitEngine;
import java.util.Locale;

public final class OrbitView extends GameView {
    public interface Listener { void finished(OrbitEngine engine); }
    private static final int BG = 0xFFFAF8FF, TEXT = 0xFF302A43, PURPLE = 0xFF7754AD, MUTED = 0xFF6D627D;
    private final OrbitEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int lastFeedback;
    private float scale, offsetX, height;

    public OrbitView(Context context, OrbitEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setFocusable(true); setClickable(true); setTag("orbitBoard");
        setContentDescription("오비트 스냅. 점은 자동으로 회전해요. 민트 구간과 겹칠 때 원이나 아래 버튼을 탭하세요. 다섯 번 미스하면 종료됩니다.");
    }
    public OrbitEngine engine() { return engine; }
    public boolean isPaused() { return engine.state() == OrbitEngine.State.PAUSED; }
    public void pauseGame() { engine.pause(); lastFrame = 0; invalidate(); }
    public void resumeGame() { engine.resume(); lastFrame = 0; invalidate(); }
    public void setForeground(boolean value) {
        foreground = value;
        if (!value) pauseGame(); else { lastFrame = 0; invalidate(); }
    }
    private void geometry() {
        scale = Math.min(getWidth() / 360f, getHeight() / 500f);
        if (scale <= 0) return;
        offsetX = (getWidth() - 360 * scale) / 2; height = getHeight() / scale;
    }
    private void syncTime() {
        long now = SystemClock.elapsedRealtimeNanos();
        if (foreground && lastFrame != 0) engine.advance((now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); syncTime(); geometry(); c.drawColor(BG);
        if (scale <= 0) return;
        c.save(); c.translate(offsetX, 0); c.scale(scale, scale);
        label(c, "SCORE", 22, 18, 9, MUTED, false);
        label(c, String.format(Locale.getDefault(), "%,d", engine.score()), 20, 55, engine.score() >= 10000000 ? 25 : 34, TEXT, false);
        label(c, "남은 기회", 254, 17, 9, MUTED, false);
        for (int i = 0; i < 5; i++) heart(c, 258 + i * 17, 38, i < engine.lives() ? 0xFFE67CA6 : 0xFFE8E1EF);
        round(c, 20, 69, 340, 99, 15, Color.WHITE);
        label(c, "LEVEL " + engine.level(), 32, 89, 11, PURPLE, false);
        label(c, "무제한", 180, 89, 11, MUTED, true);
        label(c, String.format(Locale.US, "×%.2f", engine.speed()/120), 310, 89, 11, PURPLE, true);
        label(c, "민트 구간에 들어오면, 톡!", 180, 126, 12, 0xFF286F60, true);

        float cx = 180, cy = height/2 + 7, radius = 108;
        fill(0xFFEFF7FA); c.drawCircle(cx, cy, radius + 32, paint);
        fill(0xFFF5F0FC); c.drawCircle(cx, cy, radius - 22, paint);
        stroke(0xFFE0D7EF, 1); c.drawCircle(cx, cy, radius + 22, paint);
        for (int i = 0; i < 24; i++) {
            double a = Math.toRadians(i * 15);
            fill(0xFFCFC2E2); c.drawCircle(cx+(radius+22)*(float)Math.cos(a), cy+(radius+22)*(float)Math.sin(a), i%3==0 ? 1.5f : .8f, paint);
        }
        stroke(0xFFD9CDEF, 7); c.drawCircle(cx, cy, radius, paint);
        RectF ring = new RectF(cx-radius, cy-radius, cx+radius, cy+radius);
        float start = (float)(engine.target()-engine.tolerance()), sweep = (float)(2*engine.tolerance());
        stroke(0xFFCDF0E4, 22); paint.setStrokeCap(Paint.Cap.BUTT); c.drawArc(ring, start, sweep, false, paint);
        stroke(0xFF459D85, 8); paint.setStrokeCap(Paint.Cap.BUTT); c.drawArc(ring, start, sweep, false, paint);
        stroke(0xFF236F5A, 3); paint.setStrokeCap(Paint.Cap.BUTT);
        c.drawArc(ring, (float)(engine.target()-engine.perfectTolerance()), (float)(engine.perfectTolerance()*2), false, paint);
        float tx=cx+radius*(float)Math.cos(Math.toRadians(engine.target()));
        float ty=cy+radius*(float)Math.sin(Math.toRadians(engine.target()));
        star(c, tx, ty, 5, Color.WHITE);
        // Small fading beads show the direction without obscuring the target window.
        for (int i = 5; i > 0; i--) {
            double a = Math.toRadians(engine.angle()-i*5);
            fill((0x20 + (5-i)*0x14) << 24 | 0xA88BD9);
            c.drawCircle(cx+radius*(float)Math.cos(a), cy+radius*(float)Math.sin(a), 2+(5-i)*.6f, paint);
        }
        float x=cx+radius*(float)Math.cos(Math.toRadians(engine.angle()));
        float y=cy+radius*(float)Math.sin(Math.toRadians(engine.angle()));
        fill(engine.inTarget() ? 0x5576D1BA : 0x33BCA6ED); c.drawCircle(x, y, 23, paint);
        fill(Color.WHITE); c.drawCircle(x, y, 15, paint);
        fill(PURPLE); c.drawCircle(x, y, 11, paint);
        fill(0xFFEBDCFB); c.drawCircle(x-3, y-3, 3, paint);
        star(c, cx-31, cy-49, 6, 0xFFC8AFE4); star(c, cx+31, cy+49, 4, 0xFFF0B7D1);
        label(c, "COMBO", cx, cy-18, 10, MUTED, true);
        label(c, Integer.toString(engine.combo()), cx, cy+22, 38, PURPLE, true);
        label(c, "PERFECT  " + engine.perfects(), cx, cy+42, 9, MUTED, true);
        String feedback = "자동 회전 · 목표 구간에서 한 번 탭";
        int feedbackColor = MUTED;
        if (engine.feedbackRemaining() > 0) {
            feedback = switch (engine.feedback()) {
                case PERFECT -> "✦ PERFECT! +150";
                case HIT -> "NICE! +100";
                case MISS -> "괜찮아요, 다음 타이밍!";
                default -> feedback;
            };
            feedbackColor = engine.feedback() == OrbitEngine.Feedback.MISS ? 0xFFAC416D : 0xFF286F60;
        }
        label(c, feedback, 180, height-92, 12, feedbackColor, true);
        round(c, 28, height-72, 332, height-17, 25, engine.inTarget() ? 0xFFD8F3E9 : 0xFFF0E9FC);
        label(c, engine.inTarget() ? "지금, 톡!  ✦" : "TAP  ·  타이밍을 맞춰요", 180, height-38, 15, engine.inTarget() ? 0xFF236F5A : PURPLE, true);
        c.restore();
        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == OrbitEngine.Feedback.MISS ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == OrbitEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == OrbitEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void heart(Canvas c, float x, float y, int color) {
        fill(color); Path p=new Path(); p.moveTo(x,y+7); p.cubicTo(x-15,y-3,x-4,y-12,x,y-5);
        p.cubicTo(x+4,y-12,x+15,y-3,x,y+7); c.drawPath(p,paint);
    }
    private void star(Canvas c, float x, float y, float r, int color) {
        fill(color); Path p=new Path(); p.moveTo(x,y-r); p.quadTo(x+r*.2f,y-r*.2f,x+r,y);
        p.quadTo(x+r*.2f,y+r*.2f,x,y+r); p.quadTo(x-r*.2f,y+r*.2f,x-r,y); p.quadTo(x-r*.2f,y-r*.2f,x,y-r); c.drawPath(p,paint);
    }
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color) { fill(color); c.drawRoundRect(l,t,r,b,radius,radius,paint); }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void stroke(int color,float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); }
    private void label(Canvas c,String value,float x,float y,float size,int color,boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); paint.setTextSize(size);
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT); c.drawText(value,x,y,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!foreground || engine.state() != OrbitEngine.State.RUNNING) return true;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true); geometry();
            if (scale <= 0) return true;
            float x=(event.getX()-offsetX)/scale, y=event.getY()/scale;
            if (x >= 20 && x <= 340 && y >= 138 && y <= height-12) { syncTime(); engine.tap(); invalidate(); }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        // MOVE, CANCEL, UP and secondary pointers never judge or freeze the rotating dot.
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
