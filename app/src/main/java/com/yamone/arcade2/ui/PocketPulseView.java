package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.PocketPulseEngine;
import java.util.Locale;

/** Candy-colored concentric waves with thin dashed tolerance guides. */
public final class PocketPulseView extends GameView {
    public interface Listener { void finished(PocketPulseEngine engine); }
    private static final int BG=0xFFFAF8FF, TEXT=0xFF302A43, MUTED=0xFF6D627D;
    private static final int[] INK={0xFFAF456F,0xFF247B65,0xFF397EAF,0xFF7957AC,0xFFAE6A2B};
    private static final int[] SOFT={0xFFFBE2ED,0xFFDDF4EA,0xFFE1F0FD,0xFFEDE3FB,0xFFFFEACF};
    private static final String[] NAMES={"핑크","민트","하늘","보라","살구"};
    private final PocketPulseEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DashPathEffect dashed=new DashPathEffect(new float[]{3,5},0);
    private final SparseBooleanArray pointers=new SparseBooleanArray();
    private long lastFrame;
    private boolean foreground=true, delivered;
    private int lastFeedback;
    private float scale, offsetX, height;

    public PocketPulseView(Context context,PocketPulseEngine engine,boolean haptics,Listener listener) {
        super(context); this.engine=engine; this.haptics=haptics; this.listener=listener;
        setClickable(true); setFocusable(true); setTag("pulseBoard");
        setContentDescription("포켓 펄스. 커지는 원이 두 점선 사이에 들어오면 톡. 여러 원을 바깥쪽부터 차례로 탭하세요. 다섯 번 미스하면 종료됩니다.");
    }
    public PocketPulseEngine engine() { return engine; }
    public boolean isPaused() { return engine.state()==PocketPulseEngine.State.PAUSED; }
    @Override public void pauseGame() { engine.pause(); pointers.clear(); lastFrame=0; invalidate(); }
    @Override public void resumeGame() { engine.resume(); pointers.clear(); lastFrame=0; invalidate(); }
    @Override public void setForeground(boolean value) {
        foreground=value; if (!value) pauseGame(); else { lastFrame=0; invalidate(); }
    }
    private void geometry() {
        scale = Math.min(getWidth() / 360f, getHeight() / 520f);
        if (scale<=0) return;
        offsetX=(getWidth()-360*scale)/2; height=getHeight()/scale;
    }
    private void syncTime() {
        long now=SystemClock.elapsedRealtimeNanos();
        if (foreground && lastFrame!=0) engine.advance((now-lastFrame)/1_000_000_000.0);
        lastFrame=now;
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); syncTime(); geometry(); c.drawColor(BG); if (scale<=0) return;
        c.save(); c.translate(offsetX,0); c.scale(scale,scale);
        int color=engine.pulseCount()>0?engine.color(0):3;
        label(c,"SCORE",22,18,9,MUTED,false);
        label(c,String.format(Locale.getDefault(),"%,d",engine.score()),20,55,engine.score()>=10000000?25:34,TEXT,false);
        label(c,"남은 기회",254,17,9,MUTED,false);
        for (int i=0;i<5;i++) heart(c,258+i*17,38,i<engine.lives()?0xFFE67CA6:0xFFE8E1EF);
        round(c,20,69,340,99,15,Color.WHITE);
        label(c,"LEVEL "+engine.level(),32,89,11,INK[3],false);
        label(c,"무제한 · "+engine.maxCircles()+" PULSE",185,89,10,MUTED,true);
        label(c,String.format(Locale.US,"×%.2f",engine.speed()/60),310,89,11,INK[3],true);
        label(c,engine.state()==PocketPulseEngine.State.READY?"아래 버튼을 누르면 시작해요":"두 점선 사이에 들어오면, 톡!",180,126,13,INK[3],true);
        label(c,"바깥쪽 원부터 차례로",180,145,10,MUTED,true);
        float top=158, bottom=height-125, cy=(top+bottom)/2;
        float radius=Math.min(146,(bottom-top)/2-10), unit=radius/116;
        fill(Color.WHITE); c.drawCircle(180,cy,radius+9,paint);
        stroke(SOFT[color],16*unit); c.drawCircle(180,cy,108*unit,paint);
        stroke(INK[color],1.1f); paint.setPathEffect(dashed);
        c.drawCircle(180,cy,100*unit,paint); c.drawCircle(180,cy,116*unit,paint); paint.setPathEffect(null);
        for (int i=engine.pulseCount()-1;i>=0;i--) {
            float r=(float)engine.radius(i)*unit;
            stroke(SOFT[engine.color(i)],7*unit); c.drawCircle(180,cy,r,paint);
            stroke(INK[engine.color(i)],i==0?3:2.4f); c.drawCircle(180,cy,r,paint);
            double angle=-Math.PI/2+i*.8;
            fill(INK[engine.color(i)]); c.drawCircle(180+(float)Math.cos(angle)*r,cy+(float)Math.sin(angle)*r,3,paint);
        }
        fill(SOFT[color]); c.drawCircle(180,cy,8,paint); sparkle(c,180,cy,4,INK[color]);
        sparkle(c,32,top+12,4,0xFFD3C1EA); sparkle(c,329,bottom-10,5,0xFFF0BED0);
        String feedback=engine.state()==PocketPulseEngine.State.READY?"한 번씩 톡 · 원이 늘면 톡톡톡!":"COMBO "+engine.combo()+"  ·  PERFECT "+engine.perfects();
        int feedbackColor=MUTED;
        if (engine.feedbackRemaining()>0) {
            feedback=switch(engine.feedback()) {
                case PERFECT -> "✦ PERFECT!  "+engine.combo()+" COMBO";
                case GREAT -> "GREAT!  "+engine.combo()+" COMBO";
                case GOOD -> "NICE!  "+engine.combo()+" COMBO";
                case MISS -> "괜찮아요, 다음 원을 기다려요";
                default -> feedback;
            };
            feedbackColor=engine.feedback()==PocketPulseEngine.Feedback.MISS?INK[0]:INK[1];
        }
        label(c,feedback,180,height-101,12,feedbackColor,true);
        round(c,24,height-79,336,height-17,22,engine.inWindow()?INK[color]:SOFT[color]);
        label(c,engine.state()==PocketPulseEngine.State.READY?"TAP TO START":"TAP  ✦  "+NAMES[color],180,height-50,16,engine.inWindow()?Color.WHITE:INK[color],true);
        label(c,engine.inWindow()?"지금, 톡!":"길게 누르지 말고 한 번씩",180,height-32,9,engine.inWindow()?Color.WHITE:INK[color],true);
        c.restore();
        if (engine.feedbackId()!=lastFeedback) {
            lastFeedback=engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback()==PocketPulseEngine.Feedback.MISS?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state()==PocketPulseEngine.State.FINISHED && !delivered) { delivered=true; post(()->listener.finished(engine)); }
        else if (foreground && engine.state()==PocketPulseEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private void heart(Canvas c,float x,float y,int color) {
        fill(color); Path p=new Path(); p.moveTo(x,y+7); p.cubicTo(x-15,y-3,x-4,y-12,x,y-5); p.cubicTo(x+4,y-12,x+15,y-3,x,y+7); c.drawPath(p,paint);
    }
    private void sparkle(Canvas c,float x,float y,float r,int color) {
        stroke(color,1.5f); c.drawLine(x-r,y,x+r,y,paint); c.drawLine(x,y-r,x,y+r,paint);
    }
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color) { fill(color); c.drawRoundRect(l,t,r,b,radius,radius,paint); }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void stroke(int color,float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); }
    private void label(Canvas c,String text,float x,float y,float size,int color,boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); paint.setTextSize(size);
        paint.setTextAlign(centered?Paint.Align.CENTER:Paint.Align.LEFT); c.drawText(text,x,y,paint);
    }
    private boolean accepts(float px,float py,boolean ready) {
        geometry(); if (scale<=0) return false;
        float x=(px-offsetX)/scale, y=py/scale;
        if (x<24 || x>336) return false;
        return y>=height-79 && y<=height-17 || !ready && y>=158 && y<=height-125;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!foreground || isPaused() || engine.state()==PocketPulseEngine.State.FINISHED) return true;
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int index=event.getActionIndex(), id=event.getPointerId(index);
            if (pointers.indexOfKey(id)>=0) return true;
            pointers.put(id,true);
            boolean ready=engine.state()==PocketPulseEngine.State.READY;
            if (!accepts(event.getX(index),event.getY(index),ready)) return true;
            if (ready) { engine.start(); lastFrame=SystemClock.elapsedRealtimeNanos(); }
            else { syncTime(); engine.tap(); }
            invalidate();
        } else if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
            pointers.delete(event.getPointerId(event.getActionIndex()));
            if (action==MotionEvent.ACTION_UP) { pointers.clear(); performClick(); }
        } else if (action==MotionEvent.ACTION_CANCEL) pointers.clear();
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
