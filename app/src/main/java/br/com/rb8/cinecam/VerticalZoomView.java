package br.com.rb8.cinecam;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
public final class VerticalZoomView extends View {
 public interface Listener {void onZoom(float fraction);}
 private final Paint p=new Paint(3);
 private float fraction=0f;
 private Listener listener;
 public VerticalZoomView(Context c){super(c);setMinimumHeight(120);}
 public void setListener(Listener l){listener=l;}
 public void setFraction(float f){fraction=Math.max(0f,Math.min(1f,f));invalidate();}
 @Override protected void onDraw(Canvas c){
  float d=getResources().getDisplayMetrics().density;
  float x=getWidth()/2f,top=14*d,bottom=getHeight()-14*d;
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setColor(0xFF657280);c.drawLine(x,top,x,bottom,p);
  for(int i=0;i<=8;i++){float y=top+(bottom-top)*i/8f;p.setColor(0xFF9BA6B3);p.setStrokeWidth(d);c.drawLine(x-5*d,y,x+5*d,y,p);}
  float y=bottom-fraction*(bottom-top);
  p.setStyle(Paint.Style.FILL);p.setColor(0xFFF2B85B);c.drawCircle(x,y,7*d,p);
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP){
   float d=getResources().getDisplayMetrics().density,top=14*d,bottom=getHeight()-14*d;
   setFraction((bottom-e.getY())/Math.max(1,bottom-top));if(listener!=null)listener.onZoom(fraction);return true;
  }return super.onTouchEvent(e);
 }
}