package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import com.yamone.arcade2.core.GameId;

/** Resolution-independent, decorative art. Game rules and touch handling live elsewhere. */
public final class ArcadeArt extends View {
    public enum Symbol { HOME, TROPHY, SETTINGS, AVATAR, SPARKLE }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GameId game;
    private final Symbol symbol;
    private final int ink;
    private final boolean scene;
    public ArcadeArt(Context context, GameId game, boolean scene) {
        super(context); this.game = game; this.scene = scene; this.symbol = null;
        ink = accent(game); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    public ArcadeArt(Context context, Symbol symbol, int ink) {
        super(context); this.symbol = symbol; this.ink = ink; game = null; scene = false;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    public static int accent(GameId game) {
        return switch (game) {
            case ORBIT_SNAP -> 0xFF3976AC;
            case COLOR_BREAK -> 0xFFBC4770;
            case TWIN_TAP -> 0xFF247C69;
            case LINE_SURF -> 0xFF9D612C;
            case POCKET_PULSE -> 0xFF7958B3;
            case STACK_SLICE -> 0xFF237C86;
        };
    }
    public static int tint(GameId game) {
        return switch (game) {
            case ORBIT_SNAP -> 0xFFE1F1FF;
            case COLOR_BREAK -> 0xFFFFE6EE;
            case TWIN_TAP -> 0xFFDEF5EA;
            case LINE_SURF -> 0xFFFFEEDC;
            case POCKET_PULSE -> 0xFFEDE4FF;
            case STACK_SLICE -> 0xFFDEF4F3;
        };
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float scale = Math.min(getWidth(), getHeight()) / 100f;
        c.save(); c.translate((getWidth() - 100 * scale) / 2, (getHeight() - 100 * scale) / 2); c.scale(scale, scale);
        if (game != null) {
            if (scene) {
                fill(0x80FFFFFF); c.drawCircle(50, 50, 43, paint);
                star(c, 12, 22, 5, 0xFFFFFFFF); star(c, 88, 61, 6, 0xFFFFFFFF);
                fill(0xFFF4C75E); c.drawCircle(80, 15, 2.5f, paint);
                c.save(); c.translate(10, 10); c.scale(.8f, .8f);
            }
            drawGame(c);
            if (scene) c.restore();
        } else drawSymbol(c);
        c.restore();
    }
    private void drawGame(Canvas c) {
        switch (game) {
            case ORBIT_SNAP -> {
                stroke(ink, 3); c.drawCircle(50, 50, 25, paint);
                c.save(); c.rotate(-30, 50, 50); c.drawOval(new RectF(10, 36, 90, 64), paint); c.restore();
                fill(0xFFAACFFF); c.drawCircle(50, 50, 13, paint);
                fill(0xFFFFFFFF); c.drawCircle(46, 45, 4, paint);
                fill(0xFFF6B2C9); c.drawCircle(77, 36, 8, paint);
                star(c, 21, 18, 5, ink);
            }
            case COLOR_BREAK -> {
                block(c, 18, 57, 46, 80, 0xFFF191B4); block(c, 51, 57, 80, 80, 0xFFBCA6ED);
                block(c, 18, 29, 46, 51, 0xFFBCA6ED); block(c, 51, 29, 80, 51, 0xFFF8C988);
                fill(0xFFFFFFFF); c.drawCircle(65, 25, 13, paint);
                fill(ink); c.drawCircle(65, 25, 8, paint);
                star(c, 23, 15, 5, ink);
            }
            case TWIN_TAP -> {
                block(c, 23, 14, 43, 85, 0xFFA9DBD0); block(c, 57, 14, 77, 85, 0xFFB9D9F1);
                fill(0xFFFFFFFF); c.drawCircle(33, 59, 13, paint); c.drawCircle(67, 36, 13, paint);
                fill(ink); c.drawCircle(33, 59, 8, paint); fill(0xFF6F91C8); c.drawCircle(67, 36, 8, paint);
                stroke(ink, 3); c.drawLine(15, 76, 85, 76, paint);
                star(c, 83, 17, 5, ink);
            }
            case LINE_SURF -> {
                Path wave = new Path(); wave.moveTo(8, 70); wave.cubicTo(24, 90, 43, 46, 58, 63); wave.cubicTo(70, 78, 80, 61, 93, 63);
                stroke(0xFF68ADA9, 5); c.drawPath(wave, paint);
                stroke(ink, 4); c.drawLine(37, 49, 63, 49, paint);
                fill(0xFFF5BF77); c.drawCircle(50, 32, 12, paint);
                stroke(ink, 2); c.drawArc(new RectF(24, 13, 76, 61), 205, 115, false, paint);
                star(c, 81, 27, 6, ink);
            }
            case POCKET_PULSE -> {
                stroke(0xFFCCB9F0, 6); c.drawCircle(50, 50, 32, paint);
                stroke(ink, 3); c.drawCircle(50, 50, 22, paint);
                fill(0xFFF2ABCA); c.drawCircle(50, 50, 11, paint);
                star(c, 79, 23, 7, ink); fill(0xFFFFFFFF); c.drawCircle(47, 47, 3, paint);
            }
            case STACK_SLICE -> {
                block(c, 19, 65, 81, 83, 0xFF8BD3CF); block(c, 24, 43, 76, 61, 0xFFBCA8E8);
                block(c, 36, 21, 79, 39, 0xFFF4B1C9);
                stroke(ink, 2); c.drawLine(27, 17, 27, 38, paint); c.drawLine(18, 28, 36, 28, paint);
                star(c, 84, 13, 5, ink);
            }
        }
    }
    private void drawSymbol(Canvas c) {
        switch (symbol) {
            case TROPHY -> {
                stroke(ink, 5); c.drawArc(new RectF(12, 21, 47, 58), 75, 240, false, paint);
                c.drawArc(new RectF(53, 21, 88, 58), -135, 240, false, paint);
                fill(ink); Path cup = new Path(); cup.moveTo(27, 20); cup.lineTo(73, 20); cup.lineTo(68, 48);
                cup.quadTo(50, 73, 32, 48); cup.close(); c.drawPath(cup, paint);
                block(c, 46, 53, 54, 77, ink); block(c, 30, 77, 70, 84, ink);
                star(c, 50, 37, 9, 0xFFFFFFFF);
            }
            case AVATAR -> {
                fill(0xFFDFD0FA); c.drawCircle(50, 52, 40, paint);
                fill(0xFFFFFFFF); c.drawOval(new RectF(27, 8, 43, 47), paint); c.drawOval(new RectF(56, 5, 72, 47), paint);
                c.drawOval(new RectF(19, 28, 81, 84), paint);
                fill(0xFFF8C5D9); c.drawOval(new RectF(32, 16, 38, 40), paint); c.drawOval(new RectF(61, 13, 67, 39), paint);
                c.drawCircle(32, 65, 6, paint); c.drawCircle(68, 65, 6, paint);
                fill(0xFF51456C); c.drawCircle(39, 56, 2.5f, paint); c.drawCircle(61, 56, 2.5f, paint);
                stroke(0xFF51456C, 2); c.drawArc(new RectF(44, 59, 56, 68), 0, 180, false, paint);
                star(c, 84, 21, 6, 0xFFB080D0);
            }
            case HOME -> {
                stroke(ink, 6); Path home = new Path(); home.moveTo(15, 46); home.lineTo(50, 17); home.lineTo(85, 46); c.drawPath(home, paint);
                c.drawRoundRect(new RectF(25, 41, 75, 84), 7, 7, paint); c.drawRoundRect(new RectF(43, 61, 57, 84), 3, 3, paint);
            }
            case SETTINGS -> {
                stroke(ink, 6); c.drawCircle(50, 50, 25, paint); c.drawCircle(50, 50, 8, paint);
                for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45, 50, 50); c.drawLine(50, 14, 50, 24, paint); c.restore(); }
            }
            case SPARKLE -> { star(c, 43, 54, 29, ink); star(c, 77, 24, 12, ink); }
        }
    }
    private void block(Canvas c, float l, float t, float r, float b, int color) { fill(color); c.drawRoundRect(new RectF(l, t, r, b), 6, 6, paint); }
    private void fill(int color) { paint.setStyle(Paint.Style.FILL); paint.setColor(color); }
    private void stroke(int color, float width) { paint.setStyle(Paint.Style.STROKE); paint.setColor(color); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); }
    private void star(Canvas c, float x, float y, float size, int color) {
        fill(color); Path p = new Path(); p.moveTo(x, y-size); p.quadTo(x+size*.2f, y-size*.2f, x+size, y);
        p.quadTo(x+size*.2f, y+size*.2f, x, y+size); p.quadTo(x-size*.2f, y+size*.2f, x-size, y);
        p.quadTo(x-size*.2f, y-size*.2f, x, y-size); c.drawPath(p, paint);
    }
}
