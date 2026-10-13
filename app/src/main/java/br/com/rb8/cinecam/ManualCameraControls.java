package br.com.rb8.cinecam;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;
import androidx.camera.core.Camera;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.CaptureRequestOptions;

public final class ManualCameraControls {
 private Camera camera;
 private Range<Integer> isoRange;
 private Range<Long> exposureRange;
 private boolean manual=false;
 public boolean attach(Camera camera){
  this.camera=camera;manual=false;isoRange=null;exposureRange=null;
  try{
   Camera2CameraInfo info=Camera2CameraInfo.from(camera.getCameraInfo());
   isoRange=info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
   exposureRange=info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
   int[] modes=info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
   boolean sensor=false;
   if(modes!=null)for(int mode:modes)if(mode==CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)sensor=true;
   manual=sensor&&isoRange!=null&&exposureRange!=null;
  }catch(Exception ignored){}
  return manual;
 }
 public boolean supportsManual(){return manual&&camera!=null;}
 public int minIso(){return isoRange==null?100:isoRange.getLower();}
 public int maxIso(){return isoRange==null?100:isoRange.getUpper();}
 public long minExposure(){return exposureRange==null?1000000:exposureRange.getLower();}
 public long maxExposure(){return exposureRange==null?33333333:exposureRange.getUpper();}
 public boolean setManual(int iso,long shutterNs){
  if(!supportsManual())return false;
  try{
   int targetIso=Math.max(minIso(),Math.min(maxIso(),iso));
   long ns=Math.max(minExposure(),Math.min(maxExposure(),shutterNs));
   CaptureRequestOptions opts=new CaptureRequestOptions.Builder()
    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF)
    .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY,targetIso)
    .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME,ns).build();
   Camera2CameraControl.from(camera.getCameraControl()).setCaptureRequestOptions(opts);
   return true;
  }catch(Exception e){return false;}
 }
 public void auto(){
  if(camera!=null)try{
   Camera2CameraControl.from(camera.getCameraControl()).clearCaptureRequestOptions();
  }catch(Exception ignored){}
 }
 public boolean setWhiteBalance(int preset){
  if(camera==null)return false;
  try{
   CaptureRequestOptions opts=new CaptureRequestOptions.Builder()
    .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE,preset).build();
   Camera2CameraControl.from(camera.getCameraControl()).setCaptureRequestOptions(opts);
   return true;
  }catch(Exception e){return false;}
 }
}