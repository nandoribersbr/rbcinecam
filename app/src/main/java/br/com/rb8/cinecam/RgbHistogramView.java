package br.com.rb8.cinecam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public final class RgbHistogramView extends View {
    private final Paint paint = new Paint(3);
    private final int[][] bins = new int[3][64];
    public RgbHistogramView(Context c) {super(c);setBackgroundColor(android.graphics.Color.TRANSPARENT);}
    public void submit(ImageProxy image) {
        try {
            if (image.getFormat() != android.graphics.PixelFormat.RGBA_8888) return;
            ImageProxy.PlaneProxy plane=image.getPlanes()[0];
            ByteBuffer b=plane.getBuffer();
            int[][] frame=new int[3][64];
            int width=image.getWidth(),height=image.getHeight();
            int stepX=Math.max(1,width/160),stepY=Math.max(1,height/90);
            int stride=plane.getRowStride(),pixel=plane.getPixelStride();
            for(int y=0;y<height;y+=stepY)for(int x=0;x<width;x+=stepX){
                int at=y*stride+x*pixel;
                if(at+2>=b.limit())continue;
                frame[0][(b.get(at)&255)>>2]++;
                frame[1][(b.get(at+1)&255)>>2]++;
                frame[2][(b.get(at+2)&255)>>2]++;
            }
            post(()->{for(int i=0;i<3;i++)System.arraycopy(frame[i],0,bins[i],0,64);invalidate();});
        } finally {image.close();}
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float viewW=getWidth(),viewH=getHeight();
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xEB090F17);
        c.drawRoundRect(1,1,viewW-1,viewH-1,16*getResources().getDisplayMetrics().density,16*getResources().getDisplayMetrics().density,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(0xAA8695A5);paint.setStrokeWidth(getResources().getDisplayMetrics().density);
        c.drawRoundRect(1,1,viewW-1,viewH-1,16*getResources().getDisplayMetrics().density,16*getResources().getDisplayMetrics().density,paint);

        float w=getWidth(),h=getHeight();float d=getResources().getDisplayMetrics().density;
        float left=12*d,right=w-28*d,top=25*d,bottom=h-12*d;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(0x997E8D9C);
        c.drawRoundRect(d,d,w-d,h-d,12*d,12*d,paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);paint.setColor(0x44FFFFFF);
        for(int i=1;i<4;i++)c.drawLine(left,top+(bottom-top)*i/4f,right,top+(bottom-top)*i/4f,paint);
        int[] colors={0xCCFF5266,0xCC53F1A0,0xCC5AA8FF};
        int max=1;for(int[] a:bins)for(int n:a)max=Math.max(max,n);
        for(int ch=0;ch<3;ch++){
            Path graph=new Path();graph.moveTo(left,bottom);
            for(int i=0;i<64;i++){
                float x=left+i*(right-left)/63f;
                float y=bottom-bins[ch][i]*(bottom-top)/max;
                graph.lineTo(x,y);
            }
            graph.lineTo(right,bottom);graph.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[ch]&0x66FFFFFF);
            c.drawPath(graph,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(colors[ch]);paint.setStrokeWidth(Math.max(1.1f,d));
            c.drawPath(graph,paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(12f*getResources().getDisplayMetrics().density);
        paint.setColor(Color.WHITE);c.drawText("RGB",left,17*d,paint);
        int[] legend={0xFFFF6571,0xFF59E8A5,0xFF6EAEFF};
        for(int i=0;i<3;i++){paint.setColor(legend[i]);paint.setTextSize(10*d);c.drawText(new String[]{"R","G","B"}[i],w-17*d,(30+i*17)*d,paint);}
    }
}
