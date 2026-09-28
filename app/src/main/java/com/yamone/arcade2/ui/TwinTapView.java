package com.yamone.arcade2.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.SparseIntArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import com.yamone.arcade2.core.TwinTapEngine;
import java.util.Locale;

/** Tap Tap: four lanes, independent pointer-downs and full-height pastel controls. */
public final class TwinTapView extends GameView {
    public interface Listener { void finished(TwinTapEngine engine); }
    private static final int BG=0xFFFAF8FF, TEXT=0xFF302A43, MUTED=0xFF6D627D, PURPLE=0xFF7754AD;
    private static final int[] INK={0xFF247B65,0xFFAF456F,0xFF397EAF,0xFF7957AC};
    private static final int[] SOFT={0xFFDDF4EA,0xFFFBE2ED,0xFFE1F0FD,0xFFEDE3FB};
    private static final String[] NAMES={"민트","핑크","하늘","보라"};
    private final TwinTapEngine engine;
    private final Listener listener;
    private final boolean haptics;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseIntArray pointerLanes=new SparseIntArray();
    private long lastFrame;
    private boolean foreground=true, delivered;
    private int lastFeedback;
    private float scale, offsetX, height;

    public TwinTapView(Context context,TwinTapEngine engine,boolean haptics,Listener listener) {
        super(context); this.engine=engine; this.haptics=haptics; this.listener=listener;
        setClickable(true); setFocusable(true); setTag("tapTapBoard");
        setContentDescription("탭탭. 네 레인의 노트가 아래 링에 닿을 때 같은 색 버튼을 탭하세요. 두 노트는 두 손가락으로 함께 누릅니다. 다섯 번 미스하면 종료됩니다.");
    }
    public TwinTapEngine engine() { return engine; }
    public boolean isPaused() { return engine.state()==TwinTapEngine.State.PAUSED; }
    @Override public void pauseGame() { engine.pause(); pointerLanes.clear(); lastFrame=0; invalidate(); }
    @Override public void resumeGame() { engine.resume(); pointerLanes.clear(); lastFrame=0; invalidate(); }
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
        super.onDraw(c); syncTime(); geometry(); c.drawColor(BG);
        if (scale<=0) return;
        c.save(); c.translate(offsetX,0); c.scale(scale,scale);
        label(c,"SCORE",22,18,9,MUTED,false);
        label(c,String.format(Locale.getDefault(),"%,d",engine.score()),20,55,engine.score()>=10000000?25:34,TEXT,false);
        label(c,"남은 기회",254,17,9,MUTED,false);
        for (int i=0;i<5;i++) heart(c,258+i*17,38,i<engine.lives()?0xFFE67CA6:0xFFE8E1EF);
        round(c,20,69,340,99,15,Color.WHITE);
        label(c,"LEVEL "+engine.level(),32,89,11,PURPLE,false);
        label(c,"무제한",180,89,11,MUTED,true);
        label(c,String.format(Locale.US,"×%.2f",1.6/engine.travelTime()),310,89,11,PURPLE,true);
        label(c,engine.state()==TwinTapEngine.State.READY?"아래 색 버튼을 누르면 시작해요":"노트가 링에 닿으면, 톡톡!",180,126,12,PURPLE,true);
        float hitY=height-173, top=147, bottom=height-111;
        for (int lane=0;lane<4;lane++) {
            float left=20+81*lane, x=left+38.5f;
            round(c,left,top,left+77,bottom,22,SOFT[lane]);
            stroke(0x99FFFFFF,1); c.drawLine(x,top+15,x,hitY-30,paint);
            for (int j=0;j<3;j++) {
                fill(0x88FFFFFF); c.drawCircle(x,top+30+(hitY-top-70)*j/3,2,paint);
            }
            fill(Color.WHITE); c.drawCircle(x,hitY,26,paint);
            stroke(INK[lane],engine.inHitWindow() && (engine.noteMask()&(1<<lane))!=0?3:1.5f);
            c.drawCircle(x,hitY,23,paint); symbol(c,lane,x,hitY,7,INK[lane]);
        }
        float noteY=top+19+(hitY-top-19)*(float)engine.noteProgress();
        if (engine.targetOffset()<0) noteY+=30*(float)Math.min(1,-engine.targetOffset()/TwinTapEngine.HIT_WINDOW);
        if (Integer.bitCount(engine.noteMask())==2) {
            int first=Integer.numberOfTrailingZeros(engine.noteMask()), last=31-Integer.numberOfLeadingZeros(engine.noteMask());
            stroke(0x88A792C5,1.5f); c.drawLine(58.5f+81*first,noteY,58.5f+81*last,noteY,paint);
        }
        for (int lane=0;lane<4;lane++) {
            if ((engine.noteMask()&(1<<lane))==0) continue;
            float x=58.5f+81*lane;
            if ((engine.tappedMask()&(1<<lane))==0) {
                round(c,x-19,noteY-12,x+19,noteY+16,12,0x18A28BBF);
                round(c,x-19,noteY-16,x+19,noteY+12,12,Color.WHITE);
                symbol(c,lane,x,noteY-2,9,INK[lane]);
            } else label(c,"✓",x,noteY+5,20,INK[lane],true);
        }
        String feedback=engine.state()==TwinTapEngine.State.READY?"한 개는 톡 · 두 개는 함께 톡톡":"COMBO "+engine.combo()+"  ·  PERFECT "+engine.perfects();
        int feedbackColor=MUTED;
        if (engine.feedbackRemaining()>0) {
            feedback=switch(engine.feedback()) {
                case PERFECT -> "✦ PERFECT!  "+engine.combo()+" COMBO";
                case HIT -> "NICE!  "+engine.combo()+" COMBO";
                case MISS -> "괜찮아요, 다음 리듬!";
                default -> feedback;
            };
            feedbackColor=engine.feedback()==TwinTapEngine.Feedback.MISS?INK[1]:INK[0];
        }
        label(c,feedback,180,height-91,11,feedbackColor,true);
        for (int lane=0;lane<4;lane++) {
            float left=20+81*lane, x=left+38.5f; boolean pressed=lanePressed(lane);
            round(c,left,height-72,left+77,height-17,18,pressed?INK[lane]:SOFT[lane]);
            symbol(c,lane,x,height-53,6,pressed?Color.WHITE:INK[lane]);
            label(c,NAMES[lane],x,height-29,11,pressed?Color.WHITE:INK[lane],true);
        }
        c.restore();
        if (engine.feedbackId()!=lastFeedback) {
            lastFeedback=engine.feedbackId();
            if (haptics) performHapticFeedback(engine.feedback()==TwinTapEngine.Feedback.MISS?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.CLOCK_TICK);
        }
        if (engine.state()==TwinTapEngine.State.FINISHED && !delivered) {
            delivered=true; post(()->listener.finished(engine));
        } else if (foreground && engine.state()==TwinTapEngine.State.RUNNING) postInvalidateOnAnimation();
    }
    private boolean lanePressed(int lane) {
        for (int i=0;i<pointerLanes.size();i++) if (pointerLanes.valueAt(i)==lane) return true;
        return false;
    }
    private void heart(Canvas c,float x,float y,int color) {
        fill(color); Path p=new Path(); p.moveTo(x,y+7); p.cubicTo(x-15,y-3,x-4,y-12,x,y-5);
        p.cubicTo(x+4,y-12,x+15,y-3,x,y+7); c.drawPath(p,paint);
    }
    private void symbol(Canvas c,int lane,float x,float y,float r,int color) {
        fill(color);
        if (lane==0) c.drawCircle(x,y,r,paint);
        else if (lane==1) { Path p=new Path(); p.moveTo(x,y-r); p.lineTo(x+r,y); p.lineTo(x,y+r); p.lineTo(x-r,y); p.close(); c.drawPath(p,paint); }
        else if (lane==2) c.drawRoundRect(x-r,y-r,x+r,y+r,3,3,paint);
        else { Path p=new Path(); p.moveTo(x,y-r); p.quadTo(x+r*.2f,y-r*.2f,x+r,y); p.quadTo(x+r*.2f,y+r*.2f,x,y+r); p.quadTo(x-r*.2f,y+r*.2f,x-r,y); p.quadTo(x-r*.2f,y-r*.2f,x,y-r); c.drawPath(p,paint); }
    }
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color) { fill(color); c.drawRoundRect(l,t,r,b,radius,radius,paint); }
    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void stroke(int color,float width) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); }
    private void label(Canvas c,String text,float x,float y,float size,int color,boolean centered) {
        fill(color); paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); paint.setTextSize(size);
        paint.setTextAlign(centered?Paint.Align.CENTER:Paint.Align.LEFT); c.drawText(text,x,y,paint);
    }
    private int touchedLane(float physicalX,float physicalY) {
        geometry(); if (scale<=0) return -1;
        float x=(physicalX-offsetX)/scale, y=physicalY/scale;
        if (y<height-72 || y>height-17 || x<20 || x>340) return -1;
        int lane=(int)((x-20)/81);
        return lane<4 && x<=20+81*lane+77?lane:-1;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!foreground || engine.state()==TwinTapEngine.State.PAUSED || engine.state()==TwinTapEngine.State.FINISHED) return true;
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int index=event.getActionIndex(), pointer=event.getPointerId(index);
            if (pointerLanes.indexOfKey(pointer)>=0) return true;
            int lane=touchedLane(event.getX(index),event.getY(index)); pointerLanes.put(pointer,lane);
            if (lane<0) return true;
            if (engine.state()==TwinTapEngine.State.READY) { engine.start(); lastFrame=SystemClock.elapsedRealtimeNanos(); }
            else { syncTime(); engine.tapLane(lane); }
            invalidate();
        } else if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
            pointerLanes.delete(event.getPointerId(event.getActionIndex()));
            if (action==MotionEvent.ACTION_UP) { pointerLanes.clear(); performClick(); }
            invalidate();
        } else if (action==MotionEvent.ACTION_CANCEL) { pointerLanes.clear(); invalidate(); }
        // MOVE/hold/release never judge; each distinct pointer-down is consumed once.
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
