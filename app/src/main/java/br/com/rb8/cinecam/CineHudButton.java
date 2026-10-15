package br.com.rb8.cinecam;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import androidx.appcompat.widget.AppCompatButton;

/** Compact cinematic dock control: bespoke touch surface, no platform button chrome. */
public final class CineHudButton extends AppCompatButton {
 private final Paint paint=new Paint(3);
 private final RectF rect=new RectF();
 private boolean recording;
 private boolean selected;
 private Drawable centerArt;
 public void setCenterArt(Drawable icon){centerArt=icon;invalidate();}
 public CineHudButton(Context c){super(c);setAllCaps(false);setTextColor(0xFFF2F3F5);
   setTextSize(12);setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
   setGravity(Gravity.CENTER);setPadding(px(8),0,px(8),0);setBackground(null);
   setMinHeight(0);setMinimumHeight(0);setMinWidth(0);setMinimumWidth(0);
   setStateListAnimator(null);
 }
 private int px(int dp){return (int)(getResources().getDisplayMetrics().density*dp+.5f);}
 public void setRecordingStyle(boolean b){recording=b;if(b){setCompoundDrawables(null,null,null,null);setPadding(px(56),0,px(4),0);}invalidate();}
 public void setSelectedStyle(boolean b){selected=b;invalidate();}
 @Override protected void onDraw(Canvas canvas){
  float w=getWidth(),h=getHeight(),pad=px(2);rect.set(pad,pad,w-pad,h-pad);
  paint.setStyle(Paint.Style.FILL);
  paint.setColor(isPressed()?0xFF24323D:0xED0A1017);
  canvas.drawRoundRect(rect,px(13),px(13),paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(px(1));
  paint.setColor(selected?0xFFF3B85C:0x77667889);
  canvas.drawRoundRect(rect,px(13),px(13),paint);
  if(recording){
   float diameter=Math.min(h-px(6),w*0.46f);
   float cx=pad+diameter/2+px(2),cy=h/2;
   paint.setStyle(Paint.Style.FILL);paint.setColor(0xFFEB2634);
   canvas.drawCircle(cx,cy,diameter/2,paint);
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(px(2));
   paint.setColor(0xFFFF8E91);canvas.drawCircle(cx,cy,diameter/2-px(2),paint);
   paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
   canvas.drawCircle(cx,cy,px(5),paint);
  }
  super.onDraw(canvas);
  if(centerArt!=null){
   int target=px(getHeight()>px(42)?34:25);
   int iw=Math.min(target,getWidth()-px(12));
   int ih=Math.min(target,getHeight()-px(8));
   int left=(getWidth()-iw)/2,top=(getHeight()-ih)/2;
   centerArt.setBounds(left,top,left+iw,top+ih);
   centerArt.draw(canvas);
  }
 }
}