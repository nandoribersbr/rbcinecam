package br.com.rb8.cinecam;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
/** Preview-only aspect ratio guides. Never alters the encoded camera sensor image. */
public final class CinemaFramingView extends View {
 private final Paint p=new Paint(3);
 private int selection=0;
 private final String[] names={"NATIVO","URSA / 2.39:1","RED / 2.40:1","IMAX / 1.90:1","SONY CINE / 2.35:1","ANAMÓRFICO / 2.39:1"};
 private final float[] ratios={0,2.39f,2.40f,1.90f,2.35f,2.39f};
 public CinemaFramingView(Context context){super(context);setClickable(false);}
 public void select(int index){selection=Math.max(0,Math.min(index,ratios.length-1));invalidate();}
 public int selected(){return selection;}
 public String selectedName(){return names[selection];}
 @Override protected void onDraw(Canvas c){
  if(selection==0)return;
  float w=getWidth(),h=getHeight(),desired=w/ratios[selection];float gap=Math.max(0,(h-desired)/2f);
  p.setStyle(Paint.Style.FILL);p.setColor(0xAA05090E);
  c.drawRect(0,0,w,gap,p);c.drawRect(0,h-gap,w,h,p);
  p.setColor(0xFFF2B85B);p.setTextSize(getResources().getDisplayMetrics().scaledDensity*11);
  c.drawText(names[selection]+"  •  GUIA (NÃO GRAVADO)",16,Math.max(23,gap-12),p);
  p.setColor(0x88F2B85B);p.setStrokeWidth(1);
  c.drawLine(0,gap,w,gap,p);c.drawLine(0,h-gap,w,h-gap,p);
 }
}