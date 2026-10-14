package br.com.rb8.cinecam;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.LinkedHashMap;
public final class FloatingHud {
 private final Activity host;
 private final FrameLayout root;
 private final SharedPreferences prefs;
 private final LinkedHashMap<String,View> panels=new LinkedHashMap<>();
 private boolean editing=false;
 private boolean recording=false;
 public FloatingHud(Activity a,FrameLayout r){host=a;root=r;prefs=a.getSharedPreferences("cinecam_hud_091",0);}
 public void register(String id,View view){
  panels.put(id,view);
  view.post(()->restorePosition(id,view));
  view.setVisibility(prefs.getBoolean(id+"_visible",true)?View.VISIBLE:View.GONE);
  View.OnTouchListener drag=new View.OnTouchListener(){
   private float rawX,rawY,originX,originY;
   @Override public boolean onTouch(View v,MotionEvent e){
    if(!editing||recording||prefs.getBoolean(id+"_locked",false))return false;
    switch(e.getActionMasked()){
     case MotionEvent.ACTION_DOWN: rawX=e.getRawX();rawY=e.getRawY();View panel=panels.get(id);originX=panel.getX();originY=panel.getY();return true;
     case MotionEvent.ACTION_MOVE:
      float nx=originX+e.getRawX()-rawX,ny=originY+e.getRawY()-rawY;
      View moving=panels.get(id);
      moving.setX(Math.max(0,Math.min(root.getWidth()-moving.getWidth(),nx)));
      moving.setY(Math.max(0,Math.min(root.getHeight()-moving.getHeight(),ny)));return true;
     case MotionEvent.ACTION_UP: savePosition(id,panels.get(id));return true;
     default:return true;
    }
   }
  };
  attachDrag(view,drag);
 }
 private void attachDrag(View view,View.OnTouchListener drag){
  view.setOnTouchListener(drag);
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)attachDrag(group.getChildAt(i),drag);}
 }
 private void restorePosition(String id,View v){
  if(!prefs.contains(id+"_x"))return;
  v.setX(prefs.getFloat(id+"_x",0)*Math.max(1,root.getWidth()-v.getWidth()));
  v.setY(prefs.getFloat(id+"_y",0)*Math.max(1,root.getHeight()-v.getHeight()));
 }
 private void savePosition(String id,View v){
  prefs.edit().putFloat(id+"_x",v.getX()/Math.max(1,root.getWidth()-v.getWidth()))
   .putFloat(id+"_y",v.getY()/Math.max(1,root.getHeight()-v.getHeight())).apply();
 }
 public boolean isEditing(){return editing;}
 public void setRecording(boolean b){recording=b;if(b)editing=false;}
 public void toggleEdit(){if(!recording)editing=!editing;}
 public boolean isVisible(String id){return prefs.getBoolean(id+"_visible",true);}
 public boolean isLocked(String id){return prefs.getBoolean(id+"_locked",false);}
 public void setVisible(String id,boolean visible){View v=panels.get(id);if(v!=null)v.setVisibility(visible?View.VISIBLE:View.GONE);prefs.edit().putBoolean(id+"_visible",visible).apply();}
 public void setLocked(String id,boolean locked){prefs.edit().putBoolean(id+"_locked",locked).apply();}
 public void reset(){prefs.edit().clear().apply();editing=false;for(View v:panels.values()){v.setVisibility(View.VISIBLE);v.setTranslationX(0);v.setTranslationY(0);}}
}