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
    private boolean showAudio=true;
    private TextView timerText;
    private final android.os.Handler clock=new android.os.Handler(android.os.Looper.getMainLooper());
    private long began=0;
    private boolean showHistogram=true;
    private ExecutorService analysisExecutor=Executors.newSingleThreadExecutor();
    private boolean ready=false;
    private ManualCameraControls manualControls=new ManualCameraControls();
    private FrameLayout rootFrame;
    private boolean hudVisible=true;
    private LinearLayout bottomHud;
    private android.widget.LinearLayout sideHud;
    private android.widget.TextView focusMarker;
    private SeekBar exposureRail;
    private VerticalZoomView zoomRuler;
    private TextView zoomReadout, exposureReadout;
    private Button wbHud, isoHud, shutterHud, histogramButton;
    private CinemaFramingView cinemaFraming;
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
    private android.graphics.drawable.GradientDrawable bg(int color,int border,int radius){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));
        if(border!=0)g.setStroke(dp(1),border);return g;
    }
    private Button button(String title, LinearLayout bar, int width) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false);
        b.setTextColor(0xFFF0F2F5); b.setTextSize(10); b.setLetterSpacing(0.035f); b.setGravity(Gravity.CENTER); b.setPadding(dp(3),0,dp(3),0); b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0);
        b.setBackground(bg(0xDF101820,0x507F909E,11));
        LinearLayout.LayoutParams lp=bar.getOrientation()==LinearLayout.VERTICAL?new LinearLayout.LayoutParams(dp(62),dp(31)):new LinearLayout.LayoutParams(0,dp(43),1f);lp.setMargins(dp(3),dp(2),dp(3),dp(2));bar.addView(b,lp);
        String kind=title.equals("REC")?"rec":title.contains("Câmera")?"camera":title.contains("4K")||title.contains("FHD")?"quality":title.contains("Grade")?"grid":title.contains("RGB")?"rgb":title.contains("Áudio")?"audio":title.contains("Zoom")?"zoom":title.contains("EV")?"ev":title.equals("WB")?"ev":title.equals("ISO")?"rgb":title.contains("Foco")?"zoom":title.contains("1/")?"camera":"quality";
        HudIcon icon=new HudIcon(kind,0xFFF2B85B);icon.setBounds(0,0,dp(19),dp(19));b.setCompoundDrawables(icon,null,null,null);b.setCompoundDrawablePadding(dp(4));
        return b;
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        FrameLayout root = new FrameLayout(this);rootFrame=root; root.setBackgroundColor(Color.BLACK);
        preview = new PreviewView(this); preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(preview, new FrameLayout.LayoutParams(-1,-1));
        cinemaFraming=new CinemaFramingView(this);
        root.addView(cinemaFraming,new FrameLayout.LayoutParams(-1,-1));
        histogram=new RgbHistogramView(this);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(210),dp(100),Gravity.TOP|Gravity.LEFT);
        hp.topMargin=dp(56);hp.leftMargin=dp(12);
        root.addView(histogram,hp);histogram.setVisibility(View.VISIBLE);
        audioMeter=new AudioLevelView(this);
        FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(170),dp(65),Gravity.RIGHT|Gravity.TOP);
        ap.topMargin=dp(56);ap.rightMargin=dp(12);
        root.addView(audioMeter,ap);audioMeter.setVisibility(View.VISIBLE);
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
        TextView brand=text("☰   RB CINECAM / PRO");brand.setTextColor(amber);brand.setTextSize(13);brand.setTypeface(null,1);header.addView(brand);
        quality=text("FHD  1920 × 1080");quality.setTextColor(0xFFE7EAEE);header.addView(quality);
        status=text("Aguardando câmera");header.addView(status,new LinearLayout.LayoutParams(0,-1,1));
        int batteryPercent=((android.os.BatteryManager)getSystemService(BATTERY_SERVICE)).getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        TextView battery=text("▰  "+batteryPercent+"%");battery.setTextColor(0xFFD6DEE6);header.addView(battery);
        timerText=text("●  00:00:00");timerText.setTextColor(0xFFFF5353);timerText.setTextSize(15);header.addView(timerText);
        root.addView(header,new FrameLayout.LayoutParams(-1,dp(48),Gravity.TOP));
        
        LinearLayout controls=new LinearLayout(this);bottomHud=controls;controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setBackground(bg(0xE40A1016,0x657B8994,16)); controls.setPadding(dp(11),dp(3),dp(11),dp(3));
        record=button("REC",controls,100);record.setTextColor(Color.WHITE);record.setBackground(bg(0xFFE73543,0xFFFF8888,40));record.setEnabled(false);record.setOnClickListener(v->toggle());
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
        histogramButton=button("RGB ON",controls,85);
        histogramButton.setOnClickListener(v->{if(recording!=null||stopping){message("Pare REC para alterar RGB");return;}showHistogram=!showHistogram;histogramButton.setText(showHistogram?"RGB ON":"RGB");bind();});
        Button audioButton=button("Áudio ON",controls,90);
        audioButton.setOnClickListener(v->{
            showAudio=!showAudio;audioButton.setText(showAudio?"Áudio ON":"Áudio");updateAudioMeter();
        });

        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(-1,dp(65),Gravity.BOTTOM);bottomLp.leftMargin=dp(10);bottomLp.rightMargin=dp(10);bottomLp.bottomMargin=dp(6);root.addView(controls,bottomLp);
        // Right-hand optical zoom, overlaid on preview as in approved concept.
        sideHud=new LinearLayout(this);sideHud.setOrientation(LinearLayout.VERTICAL);
        sideHud.setGravity(Gravity.CENTER);sideHud.setPadding(dp(4),dp(9),dp(4),dp(9));
        sideHud.setBackground(bg(0xD90A1018,0x788D9DA9,17));
        TextView plus=text("+");plus.setTextSize(24);plus.setGravity(Gravity.CENTER);sideHud.addView(plus);plus.setOnClickListener(v->zoomRuler.changeBy(0.05f));
        zoomRuler=new VerticalZoomView(this);
        sideHud.addView(zoomRuler,new LinearLayout.LayoutParams(dp(58),0,1f));
        zoomReadout=text("1.0x");zoomReadout.setTextColor(amber);zoomReadout.setGravity(Gravity.CENTER);sideHud.addView(zoomReadout);
        TextView minus=text("−");minus.setTextSize(20);minus.setGravity(Gravity.CENTER);sideHud.addView(minus);minus.setOnClickListener(v->zoomRuler.changeBy(-0.05f));
        zoomRuler.setListener(fraction->{
                if(camera==null)return;
                androidx.camera.core.ZoomState z=camera.getCameraInfo().getZoomState().getValue();
                if(z!=null){float ratio=z.getMinZoomRatio()+(z.getMaxZoomRatio()-z.getMinZoomRatio())*fraction;
                    camera.getCameraControl().setZoomRatio(ratio);zoomReadout.setText(String.format(Locale.US,"%.1fx",ratio));}
        });
        FrameLayout.LayoutParams sideLp=new FrameLayout.LayoutParams(dp(77),dp(183),Gravity.RIGHT|Gravity.TOP);
        sideLp.rightMargin=dp(10);sideLp.topMargin=dp(135);root.addView(sideHud,sideLp);
        LinearLayout manualHud=new LinearLayout(this);manualHud.setOrientation(LinearLayout.VERTICAL);
        manualHud.setPadding(dp(5),dp(6),dp(5),dp(6));manualHud.setBackground(bg(0xD80A111B,0x788D9DA9,17));
        Button wbButton=button("WB Auto",manualHud,68);wbHud=wbButton;
        Button isoButton=button("ISO Auto",manualHud,68);isoHud=isoButton;
        Button shutterButton=button("S Auto",manualHud,68);shutterHud=shutterButton;
        Button focusButton=button("Foco",manualHud,68);
        wbButton.setOnClickListener(v->showWhiteBalance());
        isoButton.setOnClickListener(v->showManualDialog(true));
        shutterButton.setOnClickListener(v->showManualDialog(false));
        focusButton.setOnClickListener(v->showManualFocus());
        FrameLayout.LayoutParams mlp=new FrameLayout.LayoutParams(dp(77),dp(151),Gravity.LEFT|Gravity.TOP);
        mlp.leftMargin=dp(10);mlp.topMargin=dp(161);root.addView(manualHud,mlp);
        // Bottom-right exposure strip: keeps hardware EV accessible without a modal.
        LinearLayout exposurePanel=new LinearLayout(this);exposurePanel.setGravity(Gravity.CENTER_VERTICAL);
        exposurePanel.setPadding(dp(8),0,dp(8),0);exposurePanel.setBackground(bg(0xE10A1016,0x557B8892,12));
        TextView evLabel=text("EXPOSIÇÃO");evLabel.setTextSize(9);exposurePanel.addView(evLabel);
        exposureRail=new SeekBar(this);exposureRail.setMax(100);exposureRail.setProgress(50);
        exposurePanel.addView(exposureRail,new LinearLayout.LayoutParams(0,dp(40),1));
        exposureReadout=text("+0.0");exposureReadout.setTextColor(amber);exposurePanel.addView(exposureReadout);
        exposureRail.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
            public void onProgressChanged(SeekBar b,int n,boolean user){
                if(!user||camera==null)return;
                androidx.camera.core.ExposureState e=camera.getCameraInfo().getExposureState();
                if(!e.isExposureCompensationSupported()){message("EV não suportado");return;}
                int lo=e.getExposureCompensationRange().getLower(),hi=e.getExposureCompensationRange().getUpper();
                int index=Math.round(lo+(hi-lo)*n/100f);camera.getCameraControl().setExposureCompensationIndex(index);
                exposureReadout.setText(String.format(Locale.US,"%+.1f",(float)(index*e.getExposureCompensationStep().doubleValue())));
            }
        });
        // EV appears only as a compact drawer above the dock, toggled by its real EV control.
        FrameLayout.LayoutParams evlp=new FrameLayout.LayoutParams(dp(275),dp(46),Gravity.RIGHT|Gravity.BOTTOM);
        evlp.bottomMargin=dp(78);evlp.rightMargin=dp(14);root.addView(exposurePanel,evlp);
        exposurePanel.setVisibility(View.GONE);
        Button evToggle=button("EV",controls,68);
        evToggle.setOnClickListener(v->exposurePanel.setVisibility(exposurePanel.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        focusMarker=text("□");focusMarker.setTextSize(32);focusMarker.setTextColor(amber);focusMarker.setGravity(Gravity.CENTER);
        focusMarker.setVisibility(View.GONE);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(54),dp(54),Gravity.TOP|Gravity.LEFT);
        root.addView(focusMarker,fp);
        brand.setOnClickListener(v->{
            String[] actions={"Galeria • reproduzir vídeos","Perfis de enquadramento cinematográfico","Alternar HUD limpo"};
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("RB CINECAM / PRO")
                .setItems(actions,(dialog,which)->{
                    if(which==0){
                        if(recording!=null||stopping){message("Pare REC antes de abrir a galeria");return;}
                        startActivity(new android.content.Intent(this,LibraryActivity.class));
                    }else if(which==1){
                        String[] profiles={"Nativo (sem guia)","URSA inspirada • 2.39:1","RED inspirada • 2.40:1","IMAX inspirada • 1.90:1","Sony Cinema inspirada • 2.35:1","Anamórfico • 2.39:1"};
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Perfis de enquadramento")
                            .setSingleChoiceItems(profiles,cinemaFraming.selected(),(d,n)->{
                                cinemaFraming.select(n);
                                message("GUIA: "+cinemaFraming.selectedName()+" • não altera sensor nem MP4");
                                d.dismiss();
                            }).setNegativeButton("Fechar",null).show();
                    }else{
                        hudVisible=!hudVisible;
                        controls.setVisibility(hudVisible?View.VISIBLE:View.GONE);
                        exposurePanel.setVisibility(View.GONE);
                        manualHud.setVisibility(hudVisible?View.VISIBLE:View.GONE);
                        sideHud.setVisibility(hudVisible?View.VISIBLE:View.GONE);
                    }
                }).show();
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
    private void syncManualHud(){
        if(wbHud==null||isoHud==null||shutterHud==null)return;
        if(!manualControls.isManualExposure()){isoHud.setText("ISO Auto");shutterHud.setText("S Auto");}
        else{
            isoHud.setText("ISO "+manualControls.currentIso());
            long ns=manualControls.currentExposure();
            shutterHud.setText("1/"+Math.max(1,Math.round(1000000000d/ns)));
        }
        int mode=manualControls.currentWb();
        String wb=mode==android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_AUTO?"Auto":
          mode==android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT?"Dia":
          mode==android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT?"Nuvens":
          mode==android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT?"Tungst.":"Fluor.";
        wbHud.setText("WB "+wb);
    }
    private void showManualDialog(boolean iso){
        if(!manualControls.supportsManual()){message("Controle manual não disponível nesta câmera");return;}
        android.widget.LinearLayout pane=new android.widget.LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);pane.setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView value=text(iso?"ISO "+manualControls.currentIso():"Obturador 1/"+Math.max(1,Math.round(1000000000d/manualControls.currentExposure()))+" s");pane.addView(value);
        SeekBar slider=new SeekBar(this);slider.setMax(100);
        if(iso){slider.setProgress(Math.round(100f*(manualControls.currentIso()-manualControls.minIso())/Math.max(1,manualControls.maxIso()-manualControls.minIso())));}
        else{slider.setProgress(Math.round(100f*(manualControls.currentExposure()-manualControls.minExposure())/Math.max(1L,manualControls.maxExposure()-manualControls.minExposure())));}
        pane.addView(slider);
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
                if(ok)syncManualHud();
            }
        });
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(iso?"ISO manual":"Obturador manual")
            .setView(pane).setNeutralButton("Auto",(d,w)->{manualControls.auto();syncManualHud();})
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
          .setItems(labels,(d,w)->{boolean ok=manualControls.setWhiteBalance(modes[w]);if(ok)syncManualHud();message(ok?"WB: "+labels[w]:"WB não suportado");})
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
                try {camera=provider.bindToLifecycle(this,lens,p,capture,analysis);histogram.setVisibility(View.VISIBLE);if(histogramButton!=null)histogramButton.setText("RGB ON");}
                catch(Exception unsupported) {
                    provider.unbindAll();
                    camera=provider.bindToLifecycle(this,lens,p,capture);
                    histogram.setVisibility(View.GONE);
                    if(histogramButton!=null)histogramButton.setText("RGB N/D");
                    message("4K: RGB indisponível; retorne ao FHD para analisar");
                }
            }else {camera=provider.bindToLifecycle(this,lens,p,capture);histogram.setVisibility(View.GONE);if(histogramButton!=null)histogramButton.setText("RGB OFF");}
            manualControls.attach(camera);
            syncManualHud();
            quality.setText(actual==Quality.UHD?"4K  3840 × 2160":actual==Quality.FHD?"FHD  1920 × 1080":actual==Quality.HD?"HD  1280 × 720":"SD");
            record.setEnabled(true);ready=true;
            qualityButton.setText(actual==Quality.UHD?"4K ▾":actual==Quality.FHD?"FHD ▾":actual==Quality.HD?"HD ▾":"SD ▾");
            updateAudioMeter();
            if(showHistogram&&histogram.getVisibility()!=View.VISIBLE)message("RGB N/D em "+(actual==Quality.UHD?"4K":"esta câmera")+" • FHD pode permitir RGB");
            else message(actual!=preferred?"Resolução ajustada ao hardware":"● PRONTO  •  TOQUE PARA FOCO");
            updateAudioMeter();
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
                if(event instanceof VideoRecordEvent.Status || event instanceof VideoRecordEvent.Start){
                    androidx.camera.video.AudioStats stats=event.getRecordingStats().getAudioStats();
                    if(showAudio&&audioMeter!=null){audioMeter.setVisibility(View.VISIBLE);audioMeter.showRecordedAmplitude(stats.getAudioAmplitude(),stats.hasAudio());}
                }
                if(event instanceof VideoRecordEvent.Start){began=android.os.SystemClock.elapsedRealtime();updateTimer();
                    record.setText("PARAR");record.setEnabled(true);message(audioGranted()?"Gravando":"Gravando sem áudio");
                }else if(event instanceof VideoRecordEvent.Finalize){
                    VideoRecordEvent.Finalize fin=(VideoRecordEvent.Finalize)event;
                    recording=null;stopping=false;record.setEnabled(camera!=null);began=0;clock.removeCallbacksAndMessages(null);timerText.setText("●  00:00:00");updateAudioMeter();
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
        timerText.setText(String.format(Locale.US,"●  %02d:%02d:%02d",t/3600,(t/60)%60,t%60));
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
