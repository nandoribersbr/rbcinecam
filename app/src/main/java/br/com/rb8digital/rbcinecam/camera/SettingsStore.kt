package br.com.rb8digital.rbcinecam.camera

import android.content.Context
import br.com.rb8digital.rbcinecam.core.Resolution
import br.com.rb8digital.rbcinecam.core.VideoMode

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("rbcinecam_settings", Context.MODE_PRIVATE)

    fun savedMode(): VideoMode {
        val resolution = runCatching { Resolution.valueOf(prefs.getString("resolution", Resolution.FHD.name)!!) }.getOrDefault(Resolution.FHD)
        return VideoMode(resolution, prefs.getInt("fps", 30))
    }

    fun savedGrid(): Boolean = prefs.getBoolean("grid", true)
    fun savedAspect(): String = prefs.getString("aspect", "16:9") ?: "16:9"

    fun saveMode(mode: VideoMode) = prefs.edit().putString("resolution", mode.resolution.name).putInt("fps", mode.fps).apply()
    fun saveGrid(value: Boolean) = prefs.edit().putBoolean("grid", value).apply()
    fun saveAspect(value: String) = prefs.edit().putString("aspect", value).apply()
}
