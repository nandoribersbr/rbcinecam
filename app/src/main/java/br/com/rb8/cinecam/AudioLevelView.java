package br.com.rb8.cinecam;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.view.View;

public final class AudioLevelView extends View {
 private final Paint p=new Paint(3);
 private volatile boolean running=false;
 private AudioRecord capture;
 private Thread worker;
 private volatile float level=0f;
 private volatile boolean clipping=false;
 private String state="MIC OFF";
 public AudioLevelView(Context context){super(context);setBackgroundColor(android.graphics.Color.TRANSPARENT);}
 public synchronized boolean start(){
  if(running)return true;
  int rate=16000;
  int min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
  if(min<=0){state="MIC INDISPONÍVEL";invalidate();return false;}
  try{
   AudioRecord rec=new AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,
       AudioFormat.ENCODING_PCM_16BIT,Math.max(min,4096));
   if(rec.getState()!=AudioRecord.STATE_INITIALIZED){rec.release();state="MIC INDISPONÍVEL";invalidate();return false;}
   capture=rec; running=true; state="MIC MONO";
   rec.startRecording();
   worker=new Thread(()->{
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
    short[] data=new short[1024];
    while(running){
     int n;
     try{n=rec.read(data,0,data.length);}catch(Exception e){break;}
     if(n<=0)continue;
     double squares=0;int peak=0;
     for(int i=0;i<n;i++){int v=Math.abs((int)data[i]);squares+=(double)v*v;peak=Math.max(peak,v);}
     float rms=(float)Math.sqrt(squares/n)/32768f;
     float normalized=Math.min(1f,(float)(20*Math.log10(Math.max(0.00001,rms))+60)/60f);
     level=0.65f*level+0.35f*normalized;clipping=peak>=32400;
     postInvalidate();
    }
   },"RBCineCam-AudioMonitor");
   worker.start();invalidate();return true;
  }catch(Exception e){running=false;if(capture!=null){capture.release();capture=null;}state="MIC INDISPONÍVEL";invalidate();return false;}
 }
 public void showRecordedAmplitude(double amplitude, boolean active){
  running=false;
  state=active?"REC PEAK":"REC SEM ÁUDIO";
  level=active?Math.max(0f,Math.min(1f,(float)amplitude)):0f;
  clipping=active&&amplitude>=0.98;
  postInvalidate();
 }
 public synchronized void stop(String reason){
  running=false;state=reason;level=0f;clipping=false;
  AudioRecord rec=capture;capture=null;
  if(rec!=null){try{rec.stop();}catch(Exception ignored){}try{rec.release();}catch(Exception ignored){}}
  invalidate();
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);
        float viewW=getWidth(),viewH=getHeight();
        p.setStyle(Paint.Style.FILL);p.setColor(0xEB090F17);
        c.drawRoundRect(1,1,viewW-1,viewH-1,16*getResources().getDisplayMetrics().density,16*getResources().getDisplayMetrics().density,p);
float d=getResources().getDisplayMetrics().density;
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(0x997E8D9C);
  c.drawRoundRect(d,d,getWidth()-d,getHeight()-d,11*d,11*d,p);
  p.setStyle(Paint.Style.FILL);
  p.setTextSize(10*d);p.setColor(0xFFE9EEF8);
  c.drawText(state,9*d,15*d,p);
  float left=9*d,top=23*d,w=Math.max(2,getWidth()-18*d),height=29*d;
  int bars=19,segments=6;float space=2*d,bar=(w-space*(bars-1))/bars;
  for(int i=0;i<bars;i++){
   float x=left+i*(bar+space),fraction=(i+1f)/bars;
   for(int j=0;j<segments;j++){
    float y=top+height-(j+1)*height/segments+0.6f*d;
    int col=j>4?0xFFFF6A64:j>2?0xFFFFC15E:0xFF42DEA0;
    p.setColor(fraction<=level?col:0xFF293847);
    c.drawRoundRect(x,y,x+Math.max(bar,1),y+height/segments-1*d,d*0.5f,d*0.5f,p);
   }
  }
  p.setColor(0xFFBBC6D3);p.setTextSize(9*d);
  c.drawText("-60",left,66*d,p);
  c.drawText("-30",getWidth()/2f-10*d,48*d,p);
  c.drawText(clipping?"CLIP":"0 dB",getWidth()-38*d,48*d,p);
 }
}
