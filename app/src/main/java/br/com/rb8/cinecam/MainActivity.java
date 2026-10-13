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
    private ManualCameraControls manualControls=new ManualCameraControls();
    private FrameLayout rootFrame;
    private boolean hudVisible=true;
    private LinearLayout bottomHud;
    private android.widget.LinearLayout sideHud;
    private android.widget.TextView focusMarker;
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
        b.setTextColor(0xFFF0F2F5); b.setTextSize(10); b.setLetterSpacing(0.02f); b.setGravity(Gravity.CENTER); b.setPadding(dp(3),0,dp(3),0); b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF252D39));
        LinearLayout.LayoutParams lp=bar.getOrientation()==LinearLayout.VERTICAL?new LinearLayout.LayoutParams(dp(68),dp(48)):new LinearLayout.LayoutParams(0,dp(48),1f);lp.setMargins(dp(3),dp(2),dp(3),dp(2));bar.addView(b,lp);
        String kind=title.equals("REC")?"rec":title.contains("Câmera")?"camera":title.contains("4K")?"quality":title.contains("Grade")?"grid":title.contains("RGB")?"rgb":title.contains("Áudio")?"audio":title.contains("Zoom")?"zoom":title.contains("EV")?"ev":"quality";
        HudIcon icon=new HudIcon(kind,0xFFF2B85B);icon.setBounds(0,0,dp(19),dp(19));b.setCompoundDrawables(icon,null,null,null);b.setCompoundDrawablePadding(dp(4));
        return b;
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        FrameLayout root = new FrameLayout(this);rootFrame=root; root.setBackgroundColor(Color.BLACK);
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
        header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(10),0,dp(10),0); header.setBackgroundColor(0xDE090E15);
        TextView brand=text("RB CINECAM  /  PRO");brand.setTextColor(amber);brand.setTextSize(13);brand.setTypeface(null,1);header.addView(brand);
        quality=text("FHD");quality.setTextColor(amber);header.addView(quality);
        status=text("Aguardando câmera");header.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        timerText=text("00:00:00");timerText.setTextColor(amber);header.addView(timerText);
        root.addView(header,new FrameLayout.LayoutParams(-1,dp(48),Gravity.TOP));
        
        LinearLayout controls=new LinearLayout(this);bottomHud=controls;controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setBackgroundColor(0xE80B121B); controls.setPadding(dp(10),dp(6),dp(10),dp(6));
        record=button("REC",controls,100);record.setTextColor(Color.WHITE);record.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFAF273B));record.setEnabled(false);record.setOnClickListener(v->toggle());
        flip=button("Câmera",controls,100);
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
        Button gridButton=button("Grade",controls,92);
        gridButton.setOnClickListener(v->{boolean show=grid.getVisibility()!=View.VISIBLE;grid.setVisibility(show?View.VISIBLE:View.GONE);gridButton.setText(show?"Grade ON":"Grade");message(show?"Grade ativada":"Grade desativada");});
        Button histogramButton=button("RGB",controls,85);
        histogramButton.setOnClickListener(v->{if(recording!=null||stopping){message("Pare REC para alterar RGB");return;}showHistogram=!showHistogram;histogramButton.setText(showHistogram?"RGB ON":"RGB");bind();});
        Button audioButton=button("Áudio",controls,90);
        audioButton.setOnClickListener(v->{
            showAudio=!showAudio;audioButton.setText(showAudio?"Áudio ON":"Áudio");updateAudioMeter();
        });

        root.addView(controls,new FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM));
        sideHud=new LinearLayout(this);sideHud.setOrientation(LinearLayout.VERTICAL);
        sideHud.setPadding(dp(6),dp(8),dp(6),dp(8));sideHud.setBackgroundColor(0xD80A111B);
        Button zoomButton=button("Zoom",sideHud,70);
        Button exposureButton=button("EV",sideHud,70);
        zoomButton.setOnClickListener(v->showSliderDialog("Zoom",true));
        exposureButton.setOnClickListener(v->showSliderDialog("Exposição",false));
        FrameLayout.LayoutParams sideLp=new FrameLayout.LayoutParams(dp(82),dp(122),Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        sideLp.rightMargin=dp(8);root.addView(sideHud,sideLp);
        LinearLayout manualHud=new LinearLayout(this);manualHud.setOrientation(LinearLayout.VERTICAL);manualHud.setBackgroundColor(0xDD0A111B);
        Button isoButton=button("ISO",manualHud,68);
        Button shutterButton=button("Shutter",manualHud,68);
        Button wbButton=button("WB",manualHud,68);
        Button focusButton=button("Foco",manualHud,68);
        isoButton.setOnClickListener(v->showManualDialog(true));
        shutterButton.setOnClickListener(v->showManualDialog(false));
        wbButton.setOnClickListener(v->showWhiteBalance());
        focusButton.setOnClickListener(v->showManualFocus());
        FrameLayout.LayoutParams mlp=new FrameLayout.LayoutParams(dp(82),dp(226),Gravity.LEFT|Gravity.CENTER_VERTICAL);
        mlp.leftMargin=dp(8);root.addView(manualHud,mlp);
        focusMarker=text("□");focusMarker.setTextSize(32);focusMarker.setTextColor(amber);focusMarker.setGravity(Gravity.CENTER);
        focusMarker.setVisibility(View.GONE);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(54),dp(54),Gravity.TOP|Gravity.LEFT);
        root.addView(focusMarker,fp);
        brand.setOnClickListener(v->{
            hudVisible=!hudVisible;
            controls.setVisibility(hudVisible?View.VISIBLE:View.GONE);
            sideHud.setVisibility(hudVisible?View.VISIBLE:View.GONE);
            message(hudVisible?"HUD completo":"HUD limpo: toque na marca para restaurar");
        });
        setContentView(root);
        preview.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_UP&&camera!=null){
                MeteringPoint point=preview.getMeteringPointFactory().createPoint(e.getX(),e.getY());
                FrameLayout.LayoutParams focusLp=(FrameLayout.LayoutParams)focusMarker.getLayoutParams();focusLp.leftMargin=(int)e.getX()-dp(27);focusLp.topMargin=(int)e.getY()-dp(27);focusMarker.setLayoutParams(focusLp);focusMarker.setVisibility(View.VISIBLE);focusMarker.postDelayed(()->focusMarker.setVisibility(View.GONE),1100);
                camera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(point).setAutoCancelDuration(3,TimeUnit.SECONDS).build());
                message("Foco ajustado");
            }
            return true;
        });
        if(cameraGranted())startCamera();else request.launch(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO});
    }
    private void showManualDialog(boolean iso){
        if(!manualControls.supportsManual()){message("Controle manual não disponível nesta câmera");return;}
        android.widget.LinearLayout pane=new android.widget.LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);pane.setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView value=text(iso?"ISO "+manualControls.minIso():"Obturador 1/60 s");pane.addView(value);
        SeekBar slider=new SeekBar(this);slider.setMax(100);pane.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
            public void onProgressChanged(SeekBar b,int n,boolean user){
                if(!user)return;
                int targetIso=iso?manualControls.minIso()+(manualControls.maxIso()-manualControls.minIso())*n/100:manualControls.currentIso();
                long shutter=iso?manualControls.currentExposure():
                    manualControls.minExposure()+(manualControls.maxExposure()-manualControls.minExposure())*n/100;
                boolean ok=manualControls.setManual(targetIso,shutter);
                value.setText(ok?(iso?"ISO "+targetIso:"Exposição "+(shutter/1000000f)+" ms"):"Recurso não aceito");
            }
        });
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(iso?"ISO manual":"Obturador manual")
            .setView(pane).setNeutralButton("Auto",(d,w)->manualControls.auto())
            .setPositiveButton("Fechar",(d,w)->{}).show();
    }
    private void showManualFocus(){
        if(!manualControls.supportsFocus()){message("Foco manual não disponível nesta câmera");return;}
        LinearLayout pane=new LinearLayout(this);pane.setPadding(dp(16),dp(12),dp(16),dp(12));
        SeekBar slider=new SeekBar(this);slider.setMax(100);pane.addView(slider,new LinearLayout.LayoutParams(-1,dp(44)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
            public void onProgressChanged(SeekBar bar,int n,boolean fromUser){
                if(fromUser&&!manualControls.setFocus(n/100f))message("Falha ao ajustar foco");
            }
        });
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Foco manual")
          .setView(pane).setNeutralButton("AF automático",(d,w)->manualControls.focusAuto())
          .setPositiveButton("Concluir",(d,w)->{}).show();
    }
    private void showWhiteBalance(){
        if(camera==null){message("Câmera indisponível");return;}
        String[] labels={"Automático","Luz do dia","Nublado","Incandescente","Fluorescente"};
        int[] modes={android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_AUTO,
          android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
          android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
          android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
          android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT};
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Balanço de branco")
          .setItems(labels,(d,w)->message(manualControls.setWhiteBalance(modes[w])?"WB: "+labels[w]:"WB não suportado"))
          .show();
    }
    private void showSliderDialog(String title,boolean isZoom){
        if(camera==null){message("Câmera não pronta");return;}
        SeekBar slider=new SeekBar(this);slider.setMax(100);slider.setProgress(isZoom?0:50);
        android.widget.LinearLayout content=new android.widget.LinearLayout(this);
        content.setPadding(dp(18),dp(10),dp(18),dp(12));content.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
            public void onProgressChanged(SeekBar b,int n,boolean user){
                if(!user||camera==null)return;
                if(isZoom){
                    androidx.camera.core.ZoomState z=camera.getCameraInfo().getZoomState().getValue();
                    if(z!=null)camera.getCameraControl().setZoomRatio(z.getMinZoomRatio()+(z.getMaxZoomRatio()-z.getMinZoomRatio())*n/100f);
                }else{
                    androidx.camera.core.ExposureState e=camera.getCameraInfo().getExposureState();
                    if(e.isExposureCompensationSupported()){
                        int min=e.getExposureCompensationRange().getLower(),max=e.getExposureCompensationRange().getUpper();
                        camera.getCameraControl().setExposureCompensationIndex(Math.round(min+(max-min)*n/100f));
                    }else message("EV indisponível");
                }
            }
        });
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(title).setView(content).setPositiveButton("Concluir",(d,w)->{}).show();
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
            manualControls.attach(camera);
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
            if(audioMeter!=null)audioMeter.stop("REC AGUARDANDO NÍVEIS");
            recording=pending.start(ContextCompat.getMainExecutor(this),event->{
                if(event instanceof VideoRecordEvent.Status){
                    androidx.camera.video.AudioStats stats=event.getRecordingStats().getAudioStats();
                    if(showAudio&&audioMeter!=null){audioMeter.setVisibility(View.VISIBLE);audioMeter.showRecordedAmplitude(stats.getAudioAmplitude(),stats.hasAudio());}
                }else if(event instanceof VideoRecordEvent.Start){began=android.os.SystemClock.elapsedRealtime();updateTimer();
                    record.setText("PARAR");record.setEnabled(true);message(audioGranted()?"Gravando":"Gravando sem áudio");
                }else if(event instanceof VideoRecordEvent.Finalize){
                    VideoRecordEvent.Finalize fin=(VideoRecordEvent.Finalize)event;
                    recording=null;stopping=false;record.setEnabled(camera!=null);began=0;clock.removeCallbacksAndMessages(null);timerText.setText("00:00:00");updateAudioMeter();
                    record.setText("REC");
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
        if(recording!=null||stopping){audioMeter.stop("REC AGUARDANDO NÍVEIS");return;}
        if(!audioGranted()){audioMeter.stop("SEM PERMISSÃO");message("Autorize o microfone");return;}
        if(!audioMeter.start())message("Microfone indisponível");
    }
    @Override protected void onDestroy(){clock.removeCallbacksAndMessages(null);if(audioMeter!=null)audioMeter.stop("MIC OFF");if(recording!=null)recording.stop();analysisExecutor.shutdown();super.onDestroy();}
}
