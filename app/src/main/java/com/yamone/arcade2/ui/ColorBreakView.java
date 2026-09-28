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
import com.yamone.arcade2.core.ColorBreakEngine;
import java.util.Locale;

/** Tall pastel playfield. Only the four color pads accept lane input. */
public final class ColorBreakView extends GameView {
    public interface Listener { void finished(ColorBreakEngine engine); }
    private static final int BG = 0xFFFAF8FF, TEXT = 0xFF302A43, MUTED = 0xFF6D627D, PURPLE = 0xFF7754AD;
    private static final int[] COLORS = {0xFFF197BB, 0xFFF6C66E, 0xFF76D1BA, 0xFFA999E9};
    private static final int[] INKS = {0xFF843452, 0xFF76521A, 0xFF205F52, 0xFF533D8B};
    private static final String[] NAMES = {"핑크", "옐로", "민트", "라일락"};
    private final ColorBreakEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long lastFrame;
    private boolean foreground = true, delivered;
    private int lastFeedback;
    private float scale, offsetX, boardHeight, shownLane;

    public ColorBreakView(Context context, ColorBreakEngine engine, boolean haptics, Listener listener) {
        super(context); this.engine = engine; this.haptics = haptics; this.listener = listener;
        setClickable(true); setFocusable(true); setTag("colorBreakBoard");
        setContentDescription("컬러 브레이크. 내 공과 같은 색의 아래 버튼을 탭하세요. 색마다 모양이 달라요. 다섯 번 미스하면 종료됩니다.");
    }
    public void pauseGame() { engine.pause(); lastFrame = 0; invalidate(); }
    public void resumeGame() { engine.resume(); lastFrame = 0; invalidate(); }
    public boolean isPaused() { return engine.state() == ColorBreakEngine.State.PAUSED; }
    public void setForeground(boolean value) {
        foreground = value;
        if (!value) pauseGame(); else { lastFrame = 0; invalidate(); }
    }
    private void syncTime() {
        long now = SystemClock.elapsedRealtimeNanos();
        if (foreground && lastFrame != 0) engine.advance((now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
    }
    private void geometry() {
        scale = Math.min(getWidth() / 360f, getHeight() / 480f);
        if (scale <= 0) return;
        offsetX = (getWidth() - 360 * scale) / 2;
        boardHeight = getHeight() / scale; // Use the full portrait height, not a 520px letterbox.
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); syncTime(); geometry(); c.drawColor(BG);
        if (scale <= 0) return;
        c.save(); c.translate(offsetX, 0); c.scale(scale, scale);
        label(c, "SCORE", 22, 18, 9, MUTED, false);
        label(c, String.format(Locale.getDefault(), "%,d", engine.score()), 20, 55, 34, TEXT, false);
        label(c, "남은 기회", 254, 17, 9, MUTED, false);
        for (int i = 0; i < 5; i++) heart(c, 258 + i * 17, 38, i < engine.lives() ? 0xFFE67CA6 : 0xFFE8E1EF);
        round(c, 20, 68, 340, 98, 15, Color.WHITE);
        label(c, "LEVEL " + engine.level(), 32, 88, 11, PURPLE, false);
        label(c, engine.combo() + " COMBO", 180, 88, 11, TEXT, true);
        label(c, String.format(Locale.US, "×%.2f", engine.speedMultiplier()), 308, 88, 11, PURPLE, true);
        round(c, 92, 108, 268, 138, 15, 0xFFF0E9FC);
        symbol(c, engine.targetColor(), 110, 123, 6, INKS[engine.targetColor()]);
        label(c, "내 색  " + NAMES[engine.targetColor()], 185, 128, 12, INKS[engine.targetColor()], true);
        float hitY = 177, bottom = boardHeight - 106, padY = boardHeight - 86;
        for (int lane = 0; lane < 4; lane++) {
            float x = 20 + lane * 80;
            round(c, x, 148, x + 74, bottom, 18, lane == engine.lane() && engine.armed() ? 0xFFEDE5FA : 0xFFF0ECF6);
            // Quiet dotted guides emphasize vertical travel.
            for (float y = 217; y < bottom - 15; y += 32) {
                fill(0xFFDFD5EC); c.drawCircle(x + 37, y, 1.5f, paint);
            }
        }
        fill(0xFFD6C8E7); c.drawRect(22, hitY - 1, 338, hitY + 1, paint);
        float progress = (float)engine.wallProgress();
        float wallY = bottom - 24 - (bottom - 24 - hitY) * progress;
        if (engine.recoveryRemaining() > 0) wallY -= 34 * (float)(1 - engine.recoveryRemaining() / ColorBreakEngine.RECOVERY);
        for (int lane = 0; lane < 4; lane++) {
            int color = engine.laneColor(lane); float x = 24 + lane * 80;
            round(c, x, wallY - 15, x + 66, wallY + 15, 11, COLORS[color]);
            symbol(c, color, x + 33, wallY, 7, INKS[color]);
            // Color pads track this wall's shuffled lane colors.
            boolean selected = engine.armed() && engine.lane() == lane;
            round(c, x - 2, padY + 3, x + 68, padY + 63, 19, 0xFFDDD3E9);
            round(c, x - 2, padY, x + 68, padY + 58, 19, COLORS[color]);
            if (selected) {
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f); paint.setColor(INKS[color]);
                c.drawRoundRect(x + 1, padY + 3, x + 65, padY + 55, 16, 16, paint);
            }
            symbol(c, color, x + 33, padY + 19, 7, INKS[color]);
            label(c, NAMES[color], x + 33, padY + 45, 11, INKS[color], true);
        }
        // Visual easing only; never affects collision rules.
        shownLane += (engine.lane() - shownLane) * .65f;
        float playerX = 57 + shownLane * 80;
        fill(0x227754AD); c.drawCircle(playerX, hitY + 3, 23, paint);
        fill(Color.WHITE); c.drawCircle(playerX, hitY, 22, paint);
        fill(COLORS[engine.targetColor()]); c.drawCircle(playerX, hitY, 17, paint);
        symbol(c, engine.targetColor(), playerX, hitY, 8, INKS[engine.targetColor()]);
        String hint = engine.state() == ColorBreakEngine.State.READY ? "내 색과 같은 버튼을 탭하면 시작!"
            : engine.recoveryRemaining() > 0 ? engine.feedback() == ColorBreakEngine.Feedback.HIT ? "✦  " + engine.combo() + " COMBO!" : "앗!  남은 기회 " + engine.lives()
            : "같은 색을 찾아, 톡!";
        if (engine.recoveryRemaining() > 0) {
            round(c, 104, hitY + 32, 256, hitY + 65, 16, Color.WHITE);
            label(c, hint, 180, hitY + 54, 12, engine.feedback() == ColorBreakEngine.Feedback.MISS ? 0xFFAC416D : PURPLE, true);
        }
        label(c, engine.state() == ColorBreakEngine.State.PAUSED ? "잠시 쉬어가는 중" : hint, 180, boardHeight - 8, 10, MUTED, true);
        if (engine.state() == ColorBreakEngine.State.READY) {
            star(c, 161, (hitY + bottom) / 2 - 23, 8, 0xFFCAB3EA);
            star(c, 201, (hitY + bottom) / 2 + 1, 5, 0xFFE9AFCA);
        }
        c.restore();
        if (engine.feedbackId() != lastFeedback) {
            lastFeedback = engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback() == ColorBreakEngine.Feedback.MISS
                ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state() == ColorBreakEngine.State.FINISHED && !delivered) {
            delivered = true; post(() -> listener.finished(engine));
        } else if (foreground && engine.state() == ColorBreakEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void round(Canvas c, float l, float t, float r, float b, float radius, int color) {
        fill(color); c.drawRoundRect(l, t, r, b, radius, radius, paint);
    }
    private void heart(Canvas c, float x, float y, int color) {
        fill(color); Path p = new Path(); p.moveTo(x, y + 7);
        p.cubicTo(x - 15, y - 3, x - 4, y - 12, x, y - 5);
        p.cubicTo(x + 4, y - 12, x + 15, y - 3, x, y + 7); c.drawPath(p, paint);
    }
    private void symbol(Canvas c, int color, float x, float y, float r, int ink) {
        fill(ink);
        if (color == 0) c.drawCircle(x, y, r, paint);
        else if (color == 1) c.drawRoundRect(x-r, y-r, x+r, y+r, 2, 2, paint);
        else {
            Path p = new Path(); p.moveTo(x, y-r);
            if (color == 2) { p.lineTo(x+r, y); p.lineTo(x, y+r); p.lineTo(x-r, y); }
            else { p.lineTo(x+r, y+r); p.lineTo(x-r, y+r); }
            p.close(); c.drawPath(p, paint);
        }
    }
    private void star(Canvas c, float x, float y, float r, int color) {
        fill(color); Path p = new Path(); p.moveTo(x, y-r); p.lineTo(x+r*.3f, y-r*.3f);
        p.lineTo(x+r, y); p.lineTo(x+r*.3f, y+r*.3f); p.lineTo(x, y+r);
        p.lineTo(x-r*.3f, y+r*.3f); p.lineTo(x-r, y); p.lineTo(x-r*.3f, y-r*.3f); p.close(); c.drawPath(p, paint);
    }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void label(Canvas c, String value, float x, float y, float size, int color, boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); paint.setTextSize(size);
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT); c.drawText(value, x, y, paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!foreground || engine.state() == ColorBreakEngine.State.PAUSED) return true;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true); geometry();
            if (scale <= 0) return true;
            int pointer = event.getActionIndex();
            float x = (event.getX(pointer) - offsetX) / scale, y = event.getY(pointer) / scale;
            // No accidental lane changes from the score, runway, blank margins, or ad strip.
            int lane = (int)Math.floor((x - 20) / 80);
            if (lane >= 0 && lane < 4 && x < 94 + lane * 80 && y >= boardHeight - 86 && y <= boardHeight - 28) {
                syncTime(); engine.tapLane(lane); invalidate();
            }
        } else if (action == MotionEvent.ACTION_UP) performClick();
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
