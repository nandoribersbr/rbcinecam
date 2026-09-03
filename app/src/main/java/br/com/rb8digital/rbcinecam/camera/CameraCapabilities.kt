package br.com.rb8digital.rbcinecam.camera

data class CameraCapabilities(
    val supports4k: Boolean = false,
    val supports60fps: Boolean = false,
    val supportsHdr10Bit: Boolean = false,
    val supportsRaw: Boolean = false
)

enum class RecordingProfile(val label: String) {
    SDR("SDR"),
    HLG10("HLG10"),
    RB_LOG("RB Log"),
    RAW("RAW Experimental")
}

object RecordingProfilePolicy {
    fun availableProfiles(capabilities: CameraCapabilities): List<RecordingProfile> = buildList {
        add(RecordingProfile.SDR)
        if (capabilities.supportsHdr10Bit) {
            add(RecordingProfile.HLG10)
            add(RecordingProfile.RB_LOG)
        }
        if (capabilities.supportsRaw) add(RecordingProfile.RAW)
    }
}
