package br.com.rb8digital.rbcinecam.ui

/** Release contract for the compact professional HUD. */
object ProfessionalHudContract {
    val topTechnicalControls = listOf("QUALITY", "FPS", "ASPECT", "SCOPES", "GRID")
    val allVisibleControls = topTechnicalControls + listOf(
        "VIDEO", "PHOTO", "GALLERY", "SWITCH_CAMERA", "REC",
        "ISO", "SHUTTER", "WB", "FOCUS", "EV"
    )

    const val topBarHeightDp = 46
    const val sideRailWidthDp = 50
    const val recordButtonDp = 58
    const val audioExpandedWidthDp = 42
    const val manualControlHeightDp = 34
    const val minTouchTargetDp = 44
    const val activeAccentArgb: Long = 0xFFFFC400
    const val floatingScopeButton = false
    const val floatingGridButton = false
}
