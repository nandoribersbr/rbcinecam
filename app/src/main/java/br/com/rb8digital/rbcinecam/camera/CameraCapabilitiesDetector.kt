package br.com.rb8digital.rbcinecam.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build

class CameraCapabilitiesDetector(context: Context) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    fun detect(cameraId: String): CameraCapabilities {
        val c = cameraManager.getCameraCharacteristics(cameraId)
        val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toSet().orEmpty()
        val raw = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW in caps
        val tenBit = if (Build.VERSION.SDK_INT >= 33) {
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT in caps
        } else false
        val profiles = if (Build.VERSION.SDK_INT >= 33) {
            c.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
        } else null
        val hasHlg10 = if (Build.VERSION.SDK_INT >= 33 && profiles != null) {
            profiles.supportedProfiles.contains(DynamicRangeProfiles.HLG10)
        } else false

        return CameraCapabilities(
            supportsHdr10Bit = tenBit && hasHlg10,
            supportsRaw = raw
        )
    }

    fun cameraIds(): List<String> = cameraManager.cameraIdList.toList()
}
