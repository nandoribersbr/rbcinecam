package br.com.rb8.cinecam;
import android.app.Activity;
import android.content.ContentUris;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
public final class LibraryActivity extends Activity {
 private final ArrayList<Uri> uris=new ArrayList<>();
 private final ArrayList<String> labels=new ArrayList<>();
 private VideoView player;
 private LinearLayout library;
 private TextView title;
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
  getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xFF090E14);
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
  Button back=new Button(this);back.setText("‹ CÂMERA");top.addView(back,new LinearLayout.LayoutParams(160,56));back.setOnClickListener(v->finish());
  title=new TextView(this);title.setText("RB CINECAM  /  GALERIA");title.setTextColor(0xFFF2B85B);title.setTextSize(17);top.addView(title);
  root.addView(top,new LinearLayout.LayoutParams(-1,62));
  player=new VideoView(this);player.setBackgroundColor(Color.BLACK);
  root.addView(player,new LinearLayout.LayoutParams(-1,0,3));
  MediaController mc=new MediaController(this);mc.setAnchorView(player);player.setMediaController(mc);
  TextView heading=new TextView(this);heading.setText("  GRAVAÇÕES • MOVIES/RBCINECAM");heading.setTextColor(0xFFB8C7D5);root.addView(heading);
  ScrollView scroll=new ScrollView(this);library=new LinearLayout(this);library.setOrientation(LinearLayout.VERTICAL);scroll.addView(library);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);load();
 }
 private void load(){
  Uri base=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
  String[] projection={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION};
  String where=MediaStore.Video.Media.RELATIVE_PATH+" LIKE ?";
  try(Cursor cursor=getContentResolver().query(base,projection,where,new String[]{"Movies/RBCineCam%"},MediaStore.Video.Media.DATE_ADDED+" DESC")){
   if(cursor!=null)while(cursor.moveToNext()){
    long id=cursor.getLong(0);String name=cursor.getString(1);long duration=cursor.getLong(2);
    uris.add(ContentUris.withAppendedId(base,id));labels.add(name+"   •   "+(duration/1000)+"s");
   }
  }catch(Exception ignored){}
  if(uris.isEmpty()){TextView empty=new TextView(this);empty.setText("Nenhuma gravação encontrada na pasta RB CineCam.");empty.setTextColor(Color.WHITE);empty.setPadding(24,24,24,24);library.addView(empty);return;}
  for(int i=0;i<uris.size();i++){
   final int index=i;Button item=new Button(this);item.setAllCaps(false);item.setText("▶  "+labels.get(i));
   item.setTextColor(0xFFDEE8F2);item.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1A2733));
   library.addView(item,new LinearLayout.LayoutParams(-1,58));item.setOnClickListener(v->play(index));
  }
  play(0);
 }
 private void play(int i){player.stopPlayback();player.setVideoURI(uris.get(i));title.setText("GALERIA  /  "+labels.get(i));player.setOnPreparedListener(mp->{mp.setLooping(false);player.start();});player.setOnErrorListener((mp,what,extra)->{Toast.makeText(this,"Não foi possível reproduzir o vídeo",Toast.LENGTH_SHORT).show();return true;});}
 @Override protected void onPause(){super.onPause();if(player!=null)player.pause();}
 @Override protected void onDestroy(){if(player!=null)player.stopPlayback();super.onDestroy();}
}