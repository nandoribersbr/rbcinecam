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
 public AudioLevelView(Context context){super(context);setBackgroundColor(0xD10D1420);}
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
 public synchronized void stop(String reason){
  running=false;state=reason;level=0f;clipping=false;
  AudioRecord rec=capture;capture=null;
  if(rec!=null){try{rec.stop();}catch(Exception ignored){}try{rec.release();}catch(Exception ignored){}}
  invalidate();
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float density=getResources().getDisplayMetrics().density;
  p.setTextSize(11*density);p.setColor(0xFFE9EEF8);c.drawText(state,8*density,15*density,p);
  float left=8*density,top=22*density,width=Math.max(1,getWidth()-16*density),height=12*density;
  p.setColor(0xFF263443);c.drawRoundRect(left,top,left+width,top+height,3*density,3*density,p);
  p.setColor(clipping?0xFFFF5168:level>0.85f?0xFFFFB14A:0xFF42DEA0);
  c.drawRoundRect(left,top,left+width*level,top+height,3*density,3*density,p);
  p.setColor(0xFFB7C7D8);p.setTextSize(9*density);
  c.drawText(clipping?"CLIP":"-60          -30            0 dB",left,47*density,p);
 }
}