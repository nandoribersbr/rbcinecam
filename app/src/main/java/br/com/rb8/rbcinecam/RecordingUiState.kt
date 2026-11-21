package br.com.rb8.rbcinecam

/** Recording UI mirrors CameraX events. PREPARING is shown immediately on touch. */
enum class RecordingUiState {
    IDLE, PREPARING, RECORDING, STOPPING, ERROR
}

fun transitionRecordingState(current: RecordingUiState, action: String): RecordingUiState = when(action) {
    "TAP" -> when(current) {
        RecordingUiState.IDLE, RecordingUiState.ERROR -> RecordingUiState.PREPARING
        RecordingUiState.RECORDING -> RecordingUiState.STOPPING
        else -> current
    }
    "START" -> if(current == RecordingUiState.PREPARING) RecordingUiState.RECORDING else current
    "STOP" -> if(current == RecordingUiState.RECORDING) RecordingUiState.STOPPING else current
    "FINALIZED" -> RecordingUiState.IDLE
    "FAILED" -> RecordingUiState.ERROR
    else -> current
}
