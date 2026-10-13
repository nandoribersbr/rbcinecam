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
 private Integer selectedIso=null;
 private Long selectedExposure=null;
 private int selectedWb=CaptureRequest.CONTROL_AWB_MODE_AUTO;
 public boolean attach(Camera camera){
  this.camera=camera;manual=false;isoRange=null;exposureRange=null;selectedIso=null;selectedExposure=null;selectedWb=CaptureRequest.CONTROL_AWB_MODE_AUTO;
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
 public int currentIso(){return selectedIso!=null?selectedIso:minIso();}
 public long currentExposure(){return selectedExposure!=null?selectedExposure:Math.max(minExposure(),Math.min(maxExposure(),16666667L));}
 public boolean setManual(int iso,long shutterNs){
  if(!supportsManual())return false;
  selectedIso=Math.max(minIso(),Math.min(maxIso(),iso));
  selectedExposure=Math.max(minExposure(),Math.min(maxExposure(),shutterNs));
  return apply();
 }
 private boolean apply(){
  if(camera==null)return false;
  try{
   CaptureRequestOptions.Builder builder=new CaptureRequestOptions.Builder();
   if(selectedIso!=null&&selectedExposure!=null){
    builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
    builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY,selectedIso);
    builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME,selectedExposure);
   } else builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
   builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE,selectedWb);
   Camera2CameraControl.from(camera.getCameraControl()).setCaptureRequestOptions(builder.build());
   return true;
  }catch(Exception e){return false;}
 }
 public void auto(){selectedIso=null;selectedExposure=null;apply();}
 public boolean setWhiteBalance(int preset){selectedWb=preset;return apply();}

}