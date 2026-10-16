package br.com.rb8.cinecam;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
public final class HudIcon extends Drawable {
 private final Paint p=new Paint(3);
 private final String type;private int color;
 public HudIcon(String type,int color){this.type=type;this.color=color;p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 @Override public void draw(Canvas canvas){
  Rect b=getBounds();int save=canvas.save();
  canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);
  p.setColor(color);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);
  switch(type){
   case "rec":p.setStyle(Paint.Style.FILL);canvas.drawCircle(12,12,5.5f,p);break;
   case "camera":canvas.drawRoundRect(3,7,21,19,2,2,p);canvas.drawCircle(12,13,4,p);canvas.drawLine(7,7,9,4,p);canvas.drawLine(9,4,15,4,p);break;
   case "switch":
    canvas.drawRoundRect(5,8,19,17,2,2,p);canvas.drawCircle(12,12.5f,3,p);
    canvas.drawArc(2,1,22,23,200,115,false,p);
    canvas.drawLine(19.6f,3.8f,21.2f,10.2f,p);canvas.drawLine(19.6f,3.8f,14,5.8f,p);
    canvas.drawArc(2,1,22,23,20,115,false,p);
    canvas.drawLine(4.5f,20,2.8f,13.9f,p);canvas.drawLine(4.5f,20,10,18,p);break;
   case "wb":
    canvas.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p);}break;
   case "iso":
    canvas.drawRoundRect(2,5,22,19,3,3,p);p.setStyle(Paint.Style.FILL);p.setTextSize(7);p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",1));canvas.drawText("ISO",4.6f,14.6f,p);p.setTypeface(null);break;
   case "shutter":
    canvas.drawCircle(12,12,9,p);for(int i=0;i<6;i++){double a=i*Math.PI/3;canvas.drawLine(12+(float)Math.cos(a)*3,12+(float)Math.sin(a)*3,12+(float)Math.cos(a+0.7)*9,12+(float)Math.sin(a+0.7)*9,p);}break;
   case "focus":
    canvas.drawCircle(12,12,5,p);canvas.drawLine(12,1,12,6,p);canvas.drawLine(12,18,12,23,p);canvas.drawLine(1,12,6,12,p);canvas.drawLine(18,12,23,12,p);break;
   case "quality":canvas.drawRoundRect(2,5,22,19,2,2,p);canvas.drawLine(6,16,11,16,p);canvas.drawLine(13,16,18,16,p);break;
   case "grid":canvas.drawRoundRect(3,3,21,21,1,1,p);canvas.drawLine(9,3,9,21,p);canvas.drawLine(15,3,15,21,p);canvas.drawLine(3,9,21,9,p);canvas.drawLine(3,15,21,15,p);break;
   case "rgb":canvas.drawLine(3,20,21,20,p);canvas.drawLine(4,16,4,11,p);canvas.drawLine(8,19,8,5,p);canvas.drawLine(12,18,12,9,p);canvas.drawLine(16,14,16,4,p);canvas.drawLine(20,19,20,12,p);break;
   case "audio":canvas.drawRoundRect(9,3,15,16,3,3,p);canvas.drawLine(5,11,5,13,p);canvas.drawLine(5,13,8,19,p);canvas.drawLine(8,19,16,19,p);canvas.drawLine(16,19,19,13,p);canvas.drawLine(19,13,19,11,p);canvas.drawLine(12,19,12,22,p);break;
   case "zoom":canvas.drawCircle(10,10,6,p);canvas.drawLine(14,14,21,21,p);canvas.drawLine(7,10,13,10,p);canvas.drawLine(10,7,10,13,p);break;
   case "ev":canvas.drawCircle(12,12,5,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+(float)Math.cos(a)*8,12+(float)Math.sin(a)*8,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10,p);}break;
   default:canvas.drawCircle(12,12,7,p);
  }canvas.restoreToCount(save);
 }
 @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 @Override public int getIntrinsicWidth(){return 24;}
 @Override public int getIntrinsicHeight(){return 24;}
}