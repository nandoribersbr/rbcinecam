package br.com.rb8.cinecam;

import android.app.Activity;
import android.content.ContentUris;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

/** Landscape media library: player + browsable recordings, no dead placeholder surface. */
public final class LibraryActivity extends Activity {
 private static final int GOLD=0xFFF2B85B, INK=0xFF090E14, MUTED=0xFFADBCCA;
 private final ArrayList<Uri> uris=new ArrayList<>();
 private final ArrayList<String> names=new ArrayList<>();
 private final ArrayList<Long> durations=new ArrayList<>();
 private final ArrayList<LinearLayout> cards=new ArrayList<>();
 private LinearLayout list;
 private VideoView player;
 private FrameLayout playerFrame;
 private TextView nowPlaying, count, empty, playOverlay;
 private int selected=-1;
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private GradientDrawable surface(int fill,int line,int radius){
  GradientDrawable b=new GradientDrawable();b.setColor(fill);b.setCornerRadius(dp(radius));
  if(line!=0)b.setStroke(dp(1),line);return b;
 }
 private TextView label(String s,int sp,int color){
  TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);
  t.setGravity(Gravity.CENTER_VERTICAL);return t;
 }
 private String duration(long ms){
  long sec=Math.max(0,ms/1000);return String.format(Locale.US,"%02d:%02d",sec/60,sec%60);
 }
 private TextView action(String title){
  TextView v=label(title,12,GOLD);v.setGravity(Gravity.CENTER);v.setPadding(dp(13),0,dp(13),0);
  v.setBackground(surface(0xFF141F29,0x66657685,10));return v;
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(INK);root.setPadding(dp(12),dp(7),dp(12),dp(10));
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  TextView back=action("‹  VOLTAR");header.addView(back,new LinearLayout.LayoutParams(dp(108),dp(42)));back.setOnClickListener(v->finish());
  TextView brand=label("  RB CINECAM  /  GALERIA",16,GOLD);brand.setTypeface(null,1);
  header.addView(brand,new LinearLayout.LayoutParams(0,dp(42),1));
  count=label("",11,MUTED);count.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  header.addView(count,new LinearLayout.LayoutParams(dp(155),dp(42)));
  root.addView(header,new LinearLayout.LayoutParams(-1,dp(49)));

  LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.HORIZONTAL);
  root.addView(main,new LinearLayout.LayoutParams(-1,0,1));

  LinearLayout stage=new LinearLayout(this);stage.setOrientation(LinearLayout.VERTICAL);
  LinearLayout.LayoutParams stageLp=new LinearLayout.LayoutParams(0,-1,7);stageLp.rightMargin=dp(10);
  main.addView(stage,stageLp);
  playerFrame=new FrameLayout(this);
  playerFrame.setBackground(surface(0xFF030506,0xFF36414D,15));playerFrame.setClipToOutline(true);
  player=new VideoView(this);
  FrameLayout.LayoutParams videoLp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);
  videoLp.setMargins(dp(2),dp(2),dp(2),dp(2));playerFrame.addView(player,videoLp);
  playOverlay=label("▶  SELECIONE UM VÍDEO",15,0xFFC9D4DD);playOverlay.setGravity(Gravity.CENTER);
  playerFrame.addView(playOverlay,new FrameLayout.LayoutParams(-1,-1));
  stage.addView(playerFrame,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);
  nowPlaying=label("Nenhuma gravação selecionada",13,0xFFF2F4F6);nowPlaying.setSingleLine(true);
  nowPlaying.setEllipsize(android.text.TextUtils.TruncateAt.END);
  info.addView(nowPlaying,new LinearLayout.LayoutParams(0,dp(48),1));
  TextView replay=action("↺  REINICIAR");info.addView(replay,new LinearLayout.LayoutParams(dp(123),dp(38)));
  replay.setOnClickListener(v->{if(selected>=0){player.seekTo(0);player.start();}});
  stage.addView(info,new LinearLayout.LayoutParams(-1,dp(50)));
  MediaController mc=new MediaController(this);mc.setAnchorView(playerFrame);player.setMediaController(mc);

  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(10),dp(9),dp(10),dp(9));
  panel.setBackground(surface(0xE8111822,0x806A7888,15));
  main.addView(panel,new LinearLayout.LayoutParams(0,-1,4));
  TextView title=label("GRAVAÇÕES",13,GOLD);title.setTypeface(null,1);
  panel.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));
  TextView subtitle=label("Movies / RBCineCam  •  recentes primeiro",10,MUTED);
  panel.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(24)));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
  list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
  scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
  panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);load();
 }
 private void load(){
  uris.clear();names.clear();durations.clear();cards.clear();list.removeAllViews();
  Uri base=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
  String[] projection={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION};
  try(Cursor c=getContentResolver().query(base,projection,MediaStore.Video.Media.RELATIVE_PATH+" LIKE ?",
     new String[]{"Movies/RBCineCam%"},MediaStore.Video.Media.DATE_ADDED+" DESC")){
   if(c!=null)while(c.moveToNext()){
    uris.add(ContentUris.withAppendedId(base,c.getLong(0)));
    names.add(c.getString(1));durations.add(c.isNull(2)?0L:c.getLong(2));
   }
  }catch(Exception ex){Toast.makeText(this,"Não foi possível carregar todas as gravações",Toast.LENGTH_SHORT).show();}
  count.setText(uris.size()+" VÍDEO"+(uris.size()==1?"":"S"));
  if(uris.isEmpty()){
   TextView nothing=label("Nenhum vídeo encontrado.\nGrave com o botão REC para começar.",13,MUTED);
   nothing.setGravity(Gravity.CENTER);list.addView(nothing,new LinearLayout.LayoutParams(-1,dp(130)));
   return;
  }
  for(int i=0;i<uris.size();i++)addCard(i);
  // Select the first item without autoplay: user decides when to preview.
  play(0,false);
 }
 private void addCard(int index){
  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);
  card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(7),dp(6),dp(7),dp(6));
  LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,dp(88));clp.bottomMargin=dp(7);
  list.addView(card,clp);cards.add(card);
  ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
  thumb.setBackground(surface(0xFF1C2631,0,8));thumb.setClipToOutline(true);
  card.addView(thumb,new LinearLayout.LayoutParams(dp(108),dp(68)));
  try{Bitmap image=getContentResolver().loadThumbnail(uris.get(index),new Size(dp(216),dp(136)),null);thumb.setImageBitmap(image);}
  catch(Exception ex){thumb.setImageDrawable(null);}
  LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
  details.setGravity(Gravity.CENTER_VERTICAL);details.setPadding(dp(10),0,0,0);
  card.addView(details,new LinearLayout.LayoutParams(0,-1,1));
  String name=names.get(index)==null?"Gravação":names.get(index);
  TextView filename=label(name,12,0xFFF1F3F6);filename.setMaxLines(2);
  filename.setEllipsize(android.text.TextUtils.TruncateAt.END);details.addView(filename);
  TextView meta=label("▶  "+duration(durations.get(index))+"     MP4",10,MUTED);details.addView(meta);
  card.setOnClickListener(v->play(index,true));
 }
 private void play(int i,boolean autoPlay){
  if(i<0||i>=uris.size())return;
  selected=i;
  for(int j=0;j<cards.size();j++)cards.get(j).setBackground(
      surface(j==i?0xFF202C38:0xFF111A24,j==i?GOLD:0x554A5766,11));
  String name=names.get(i)==null?"Gravação":names.get(i);
  nowPlaying.setText("▶  "+name+"    •    "+duration(durations.get(i)));
  player.stopPlayback();player.setVideoURI(uris.get(i));
  playOverlay.setVisibility(View.VISIBLE);playOverlay.setText("▶  TOQUE PARA REPRODUZIR");
  playOverlay.setOnClickListener(v->{playOverlay.setVisibility(View.GONE);player.start();});
  player.setOnPreparedListener(mp->{
   mp.setLooping(false);
   if(autoPlay){playOverlay.setVisibility(View.GONE);player.start();}
   else{player.seekTo(1);}
  });
  player.setOnErrorListener((mp,what,extra)->{
   playOverlay.setVisibility(View.VISIBLE);playOverlay.setText("Vídeo indisponível ou formato incompatível");
   return true;
  });
 }
 @Override protected void onPause(){super.onPause();if(player!=null&&player.isPlaying())player.pause();}
 @Override protected void onDestroy(){if(player!=null)player.stopPlayback();super.onDestroy();}
}