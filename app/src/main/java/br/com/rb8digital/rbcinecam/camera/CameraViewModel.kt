package br.com.rb8digital.rbcinecam.camera

import android.app.Application
import android.net.Uri
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import br.com.rb8digital.rbcinecam.core.CameraCapabilities
import br.com.rb8digital.rbcinecam.core.CameraSettings
import br.com.rb8digital.rbcinecam.core.ConfigurationPolicy
import br.com.rb8digital.rbcinecam.core.Resolution
import br.com.rb8digital.rbcinecam.core.VideoMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = CameraEngine(app)
    private val policy = ConfigurationPolicy()
    private val meter = AudioLevelMeter(app)
    private val store = SettingsStore(app)
    private val _state = MutableStateFlow(CameraUiState(settings = CameraSettings("", store.savedMode()), grid = store.savedGrid(), aspectRatio = store.savedAspect()))
    val state: StateFlow<CameraUiState> = _state.asStateFlow()
    private var audioJob: Job? = null

    fun initialize(owner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            val cameras = engine.discoverCapabilities()
            val first = cameras.firstOrNull() ?: run {
                _state.value = _state.value.copy(warning = "Nenhuma câmera compatível foi encontrada.")
                return@launch
            }
            val requested = _state.value.settings.copy(cameraId = first.cameraId)
            val resolved = policy.resolve(requested, null, first)
            _state.value = _state.value.copy(
                cameras = cameras,
                activeCapabilities = first,
                settings = resolved.applied,
                warning = resolved.reason
            )
            bind(owner, previewView, resolved.applied)
            startMeter()
        }
    }

    private suspend fun bind(owner: LifecycleOwner, previewView: PreviewView, settings: CameraSettings) {
        runCatching { engine.bind(owner, previewView, settings.cameraId, settings.mode) }
            .onSuccess {
                _state.value = _state.value.copy(ready = true, settings = settings)
            }
            .onFailure {
                _state.value = _state.value.copy(ready = false, warning = it.message ?: "Falha ao iniciar a câmera.")
            }
    }

    fun requestMode(owner: LifecycleOwner, previewView: PreviewView, mode: VideoMode) {
        val active = _state.value.activeCapabilities ?: return
        val request = _state.value.settings.copy(mode = mode)
        val resolved = policy.resolve(request, _state.value.settings, active)
        store.saveMode(resolved.applied.mode)
        _state.value = _state.value.copy(settings = resolved.applied, warning = resolved.reason)
        viewModelScope.launch { bind(owner, previewView, resolved.applied) }
    }

    fun switchCamera(owner: LifecycleOwner, previewView: PreviewView) {
        val cameras = _state.value.cameras
        if (cameras.size < 2) return
        val current = _state.value.activeCapabilities
        val index = cameras.indexOfFirst { it.cameraId == current?.cameraId }
        val next = cameras[(index + 1).mod(cameras.size)]
        val request = _state.value.settings.copy(cameraId = next.cameraId)
        val resolved = policy.resolve(request, _state.value.settings, next)
        _state.value = _state.value.copy(activeCapabilities = next, settings = resolved.applied, warning = resolved.reason)
        viewModelScope.launch { bind(owner, previewView, resolved.applied) }
    }

    fun setZoom(ratio: Float) {
        engine.setZoom(ratio)
        _state.value = _state.value.copy(zoomRatio = ratio)
    }

    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(previewView.width.toFloat(), previewView.height.toFloat())
        engine.tapToFocus(factory.createPoint(x, y))
    }

    fun setGrid(enabled: Boolean) { store.saveGrid(enabled); _state.value = _state.value.copy(grid = enabled) }
    fun setAspectRatio(value: String) { store.saveAspect(value); _state.value = _state.value.copy(aspectRatio = value) }

    fun setIso(value: Int?) {
        _state.value = _state.value.copy(iso = value)
        applyManual()
    }

    fun setShutter(ns: Long?) {
        _state.value = _state.value.copy(shutterNs = ns)
        applyManual()
    }

    fun setEv(value: Int) {
        _state.value = _state.value.copy(ev = value)
        applyManual()
    }

    fun setWb(mode: Int?) {
        _state.value = _state.value.copy(wbMode = mode)
        applyManual()
    }

    fun setManualFocus(distance: Float?) {
        _state.value = _state.value.copy(manualFocus = distance != null, focusDistance = distance)
        applyManual()
    }

    fun resetAuto() {
        engine.resetAuto()
        _state.value = _state.value.copy(iso = null, shutterNs = null, wbMode = null, manualFocus = false, focusDistance = null)
    }

    private fun applyManual() {
        val s = _state.value
        engine.applyManual(s.iso, s.shutterNs, s.ev, s.wbMode, s.focusDistance)
    }

    fun toggleRecording() {
        if (_state.value.recording) {
            engine.stopRecording()
            return
        }
        engine.startRecording { event ->
            when (event) {
                is VideoRecordEvent.Start -> _state.value = _state.value.copy(recording = true, elapsedMs = 0)
                is VideoRecordEvent.Status -> _state.value = _state.value.copy(elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000)
                is VideoRecordEvent.Finalize -> {
                    val uri = event.outputResults.outputUri.takeIf { it != Uri.EMPTY }?.toString()
                    _state.value = _state.value.copy(
                        recording = false,
                        lastVideoUri = uri,
                        warning = if (event.hasError()) "A gravação foi encerrada com erro ${event.error}." else null
                    )
                }
            }
        }
    }

    private fun startMeter() {
        audioJob?.cancel()
        audioJob = viewModelScope.launch {
            meter.run { level -> _state.value = _state.value.copy(audioLevel = level) }
        }
    }

    override fun onCleared() {
        audioJob?.cancel()
        engine.release()
        super.onCleared()
    }
}
