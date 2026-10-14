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
 public CineHudButton(Context c){super(c);setAllCaps(false);setTextColor(0xFFF2F3F5);
   setTextSize(12);setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
   setGravity(Gravity.CENTER);setPadding(px(8),0,px(8),0);setBackground(null);
   setMinHeight(0);setMinimumHeight(0);setMinWidth(0);setMinimumWidth(0);
   setStateListAnimator(null);
 }
 private int px(int dp){return (int)(getResources().getDisplayMetrics().density*dp+.5f);}
 public void setRecordingStyle(boolean b){recording=b;invalidate();}
 public void setSelectedStyle(boolean b){selected=b;invalidate();}
 @Override protected void onDraw(Canvas canvas){
  float w=getWidth(),h=getHeight(),pad=px(2);rect.set(pad,pad,w-pad,h-pad);
  paint.setStyle(Paint.Style.FILL);
  paint.setColor(recording?0xFFD92839:(isPressed()?0xFF27343F:0xEA0B1118));
  canvas.drawRoundRect(rect,px(recording?25:12),px(recording?25:12),paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(px(1));
  paint.setColor(recording?0xFFFF9E99:(selected?0xFFF5BA60:0x665B6D7A));
  canvas.drawRoundRect(rect,px(recording?25:12),px(recording?25:12),paint);
  super.onDraw(canvas);
 }
}