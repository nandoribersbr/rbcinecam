package br.com.rb8.cinecam;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends AppCompatActivity {
    private PreviewView preview;
    private ProcessCameraProvider provider;
    private Camera camera;
    private VideoCapture<Recorder> capture;
    private Recording recording;
    private CameraSelector lens = CameraSelector.DEFAULT_BACK_CAMERA;
    private Quality preferred = Quality.FHD;
    private Button record, qualityButton, flip;
    private int amber=0xFFF2B85B;
    private int panel=0xF20C1119;
    private TextView status, quality;
    private View grid;
    private boolean stopping;
    private RgbHistogramView histogram;
    private AudioLevelView audioMeter;
    private boolean showAudio=false;
    private TextView timerText;
    private final android.os.Handler clock=new android.os.Handler(android.os.Looper.getMainLooper());
    private long began=0;
    private boolean showHistogram=false;
    private ExecutorService analysisExecutor=Executors.newSingleThreadExecutor();
    private boolean ready=false;
    private final ActivityResultLauncher<String[]> request = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (cameraGranted()) startCamera();
                else message("Permissão de câmera necessária");
            });
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private boolean cameraGranted() { return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED; }
    private boolean audioGranted() { return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED; }
    private TextView text(String s) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(12); t.setLetterSpacing(0.07f);
        t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(9),0,dp(9),0);
        return t;
    }
    private Button button(String title, LinearLayout bar, int width) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false);
        b.setTextColor(0xFFF0F2F5); b.setTextSize(11); b.setLetterSpacing(0.04f);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF252D39));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(45));lp.setMargins(dp(3),0,dp(3),0);bar.addView(b,lp);
        return b;
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        preview = new PreviewView(this); preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        preview.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        root.addView(preview, new FrameLayout.LayoutParams(-1,-1));
        histogram=new RgbHistogramView(this);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(180),dp(86),Gravity.TOP|Gravity.RIGHT);
        hp.topMargin=dp(53);hp.rightMargin=dp(12);
        root.addView(histogram,hp);histogram.setVisibility(View.GONE);
        audioMeter=new AudioLevelView(this);
        FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(176),dp(55),Gravity.LEFT|Gravity.TOP);
        ap.topMargin=dp(55);ap.leftMargin=dp(10);
        root.addView(audioMeter,ap);audioMeter.setVisibility(View.GONE);
        grid = new View(this) {
            android.graphics.Paint p = new android.graphics.Paint(1);
            @Override protected void onDraw(android.graphics.Canvas c) {
                p.setColor(0x44FFFFFF);p.setStrokeWidth(dp(1));
                float w=getWidth()/3f,h=getHeight()/3f;
                for(int i=1;i<3;i++){c.drawLine(i*w,0,i*w,getHeight(),p);c.drawLine(0,i*h,getWidth(),i*h,p);}
            }
        };
        grid.setVisibility(View.GONE);
        root.addView(grid,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL); header.setBackgroundColor(panel);
        TextView brand=text("◉  RB CINECAM  /  PRO");brand.setTextColor(amber);brand.setTextSize(13);brand.setTypeface(null,1);header.addView(brand);
        quality=text("FHD");quality.setTextColor(amber);header.addView(quality);
        status=text("Aguardando câmera");header.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        timerText=text("00:00:00");timerText.setTextColor(amber);header.addView(timerText);
        root.addView(header,new FrameLayout.LayoutParams(-1,dp(45),Gravity.TOP));
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setBackgroundColor(panel); controls.setPadding(dp(6),0,dp(6),0);
        record=button("●  REC",controls,100);record.setTextColor(Color.WHITE);record.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFAF273B));record.setEnabled(false);record.setOnClickListener(v->toggle());
        flip=button("↻  Câmera",controls,100);
        flip.setOnClickListener(v->{
            if(recording!=null||stopping){message("Pare a gravação para trocar de câmera");return;}
            if(provider==null||!ready){message("Câmera ainda não está pronta");return;}
            CameraSelector next = lens==CameraSelector.DEFAULT_BACK_CAMERA?CameraSelector.DEFAULT_FRONT_CAMERA:CameraSelector.DEFAULT_BACK_CAMERA;
            try {if(!provider.hasCamera(next)){message("Câmera indisponível");return;}}
            catch(Exception e){message("Erro ao verificar câmera");return;}
            lens=next;bind();
        });
        qualityButton=button("FHD / 4K",controls,100);
        qualityButton.setOnClickListener(v->{if(recording!=null||stopping){message("Pare a gravação para mudar a resolução");return;}
            if(provider==null){message("Câmera não inicializada");return;}
            preferred=preferred==Quality.FHD?Quality.UHD:Quality.FHD;bind();});
        Button gridButton=button("▦  Grade",controls,92);
        gridButton.setOnClickListener(v->{boolean show=grid.getVisibility()!=View.VISIBLE;grid.setVisibility(show?View.VISIBLE:View.GONE);gridButton.setText(show?"▦  Grade ON":"▦  Grade");message(show?"Grade ativada":"Grade desativada");});
        Button histogramButton=button("◫  RGB",controls,85);
        histogramButton.setOnClickListener(v->{showHistogram=!showHistogram;histogramButton.setText(showHistogram?"◫  RGB ON":"◫  RGB");bind();});
        Button audioButton=button("♫ AUDIO",controls,90);
        audioButton.setOnClickListener(v->{
            showAudio=!showAudio;audioButton.setText(showAudio?"♫ AUDIO ON":"♫ AUDIO");updateAudioMeter();
        });
        controls.addView(text("ZOOM"));
        SeekBar zoom=new SeekBar(this);zoom.setMax(100);controls.addView(zoom,new LinearLayout.LayoutParams(dp(115),dp(40)));
        zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int n,boolean fromUser){
                if(!fromUser)return;
                if(camera==null||camera.getCameraInfo().getZoomState().getValue()==null){message("Zoom indisponível: câmera não pronta");return;}
                androidx.camera.core.ZoomState z=camera.getCameraInfo().getZoomState().getValue();
                float ratio=z.getMinZoomRatio()+(z.getMaxZoomRatio()-z.getMinZoomRatio())*n/100f;
                camera.getCameraControl().setZoomRatio(ratio);
            }
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        controls.addView(text("EXPOSIÇÃO"));
        SeekBar ev=new SeekBar(this);ev.setMax(100);ev.setProgress(50);controls.addView(ev,new LinearLayout.LayoutParams(dp(110),dp(40)));
        ev.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int n,boolean fromUser){
                if(!fromUser)return;
                if(camera==null){message("Exposição indisponível: câmera não pronta");return;}
                androidx.camera.core.ExposureState x=camera.getCameraInfo().getExposureState();
                if(!x.isExposureCompensationSupported()){message("Compensação de exposição não suportada");return;}
                int min=x.getExposureCompensationRange().getLower(), max=x.getExposureCompensationRange().getUpper();
                camera.getCameraControl().setExposureCompensationIndex(Math.round(min+(max-min)*n/100f));
            }
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        scroll.addView(controls);
        root.addView(scroll,new FrameLayout.LayoutParams(-1,dp(60),Gravity.BOTTOM));
        setContentView(root);
        preview.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_UP&&camera!=null){
                MeteringPoint point=preview.getMeteringPointFactory().createPoint(e.getX(),e.getY());
                camera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(point).setAutoCancelDuration(3,TimeUnit.SECONDS).build());
                message("Foco ajustado");
            }
            return true;
        });
        if(cameraGranted())startCamera();else request.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO});
    }
    private void message(String s){if(status!=null)status.setText(s);}
    @Override protected void onResume(){super.onResume(); if(preview!=null && !cameraGranted()) message("Autorize a câmera nas configurações do Android");}
    private void startCamera(){
        com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
        f.addListener(()->{try{provider=f.get();bind();}catch(Exception e){message("Falha ao iniciar câmera: "+e.getMessage());}}, ContextCompat.getMainExecutor(this));
    }
    private void bind(){
        if(provider==null||recording!=null||stopping)return;
        record.setEnabled(false);ready=false;capture=null;camera=null;message("Configurando câmera...");
        try{
            provider.unbindAll();
            Preview p=new Preview.Builder().build();
            p.setSurfaceProvider(preview.getSurfaceProvider());
            List<Quality> supported=QualitySelector.getSupportedQualities(provider.getCameraInfo(lens));
            Quality actual=supported.contains(preferred)?preferred:supported.contains(Quality.FHD)?Quality.FHD:supported.contains(Quality.HD)?Quality.HD:supported.isEmpty()?null:supported.get(0);
            if(actual==null){message("Sem qualidade de vídeo compatível");return;}
            Recorder recorder=new Recorder.Builder().setQualitySelector(QualitySelector.from(actual)).build();
            capture=VideoCapture.withOutput(recorder);
            if(showHistogram){
                ImageAnalysis analysis=new ImageAnalysis.Builder().setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                  .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setTargetResolution(new android.util.Size(640,360)).build();
                analysis.setAnalyzer(analysisExecutor,image->histogram.submit(image));
                try {camera=provider.bindToLifecycle(this,lens,p,capture,analysis);histogram.setVisibility(View.VISIBLE);}
                catch(Exception unsupported) {
                    provider.unbindAll();
                    camera=provider.bindToLifecycle(this,lens,p,capture);
                    showHistogram=false;histogram.setVisibility(View.GONE);
                    message("RGB indisponível nesta câmera/resolução");
                }
            }else {camera=provider.bindToLifecycle(this,lens,p,capture);histogram.setVisibility(View.GONE);}
            quality.setText(actual==Quality.UHD?"4K":actual==Quality.FHD?"FHD":actual==Quality.HD?"HD":"SD");
            record.setEnabled(true);ready=true;
            qualityButton.setText(actual==Quality.UHD?"4K ▾":actual==Quality.FHD?"FHD ▾":actual==Quality.HD?"HD ▾":"SD ▾");
            message(actual!=preferred?"Resolução ajustada ao hardware":"PRONTO  •  TOQUE PARA FOCO");
        }catch(Exception e){message("Falha ao configurar câmera: "+e.getMessage());}
    }
    private void toggle(){
        if(recording!=null){if(!stopping){stopping=true;record.setEnabled(false);recording.stop();}return;}
        if(capture==null||camera==null||!ready){message("Câmera não está pronta");return;}
        try{
            ContentValues values=new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME,"RBCineCam_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".mp4");
            values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/RBCineCam");
            MediaStoreOutputOptions opts=new MediaStoreOutputOptions.Builder(getContentResolver(),MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build();
            PendingRecording pending=capture.getOutput().prepareRecording(this,opts);
            if(audioGranted())pending=pending.withAudioEnabled();
            record.setEnabled(false);
            if(audioMeter!=null)audioMeter.stop("REC: MONITOR PAUSADO");
            recording=pending.start(ContextCompat.getMainExecutor(this),event->{
                if(event instanceof VideoRecordEvent.Start){began=android.os.SystemClock.elapsedRealtime();updateTimer();
                    record.setText("■  STOP");record.setEnabled(true);message(audioGranted()?"Gravando":"Gravando sem áudio");
                }else if(event instanceof VideoRecordEvent.Finalize){
                    VideoRecordEvent.Finalize fin=(VideoRecordEvent.Finalize)event;
                    recording=null;stopping=false;record.setEnabled(camera!=null);began=0;clock.removeCallbacksAndMessages(null);timerText.setText("00:00:00");updateAudioMeter();
                    record.setText("●  REC");
                    if(fin.hasError())message("Falha na gravação: "+fin.getError());
                    else{message("Salvo em Movies/RBCineCam");Toast.makeText(this,"Vídeo salvo",Toast.LENGTH_SHORT).show();}
                }
            });
        }catch(Exception e){recording=null;stopping=false;record.setEnabled(true);updateAudioMeter();message("Falha ao gravar: "+e.getMessage());}
    }
    private void updateTimer(){
        if(began==0||timerText==null)return;
        long t=(android.os.SystemClock.elapsedRealtime()-began)/1000;
        timerText.setText(String.format(Locale.US,"%02d:%02d:%02d",t/3600,(t/60)%60,t%60));
        clock.postDelayed(()->updateTimer(),500);
    }
    private void updateAudioMeter(){
        if(audioMeter==null)return;
        audioMeter.setVisibility(showAudio?View.VISIBLE:View.GONE);
        if(!showAudio){audioMeter.stop("MIC OFF");return;}
        if(recording!=null||stopping){audioMeter.stop("REC: MONITOR PAUSADO");return;}
        if(!audioGranted()){audioMeter.stop("SEM PERMISSÃO");message("Autorize o microfone");return;}
        if(!audioMeter.start())message("Microfone indisponível");
    }
    @Override protected void onDestroy(){clock.removeCallbacksAndMessages(null);if(audioMeter!=null)audioMeter.stop("MIC OFF");if(recording!=null)recording.stop();analysisExecutor.shutdown();super.onDestroy();}
}
