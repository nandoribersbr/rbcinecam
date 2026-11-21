package br.com.rb8.rbcinecam

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingUiStateTest {
    @Test fun startStopFinalize() {
        var state=transitionRecordingState(RecordingUiState.IDLE,"TAP")
        assertEquals(RecordingUiState.PREPARING,state)
        state=transitionRecordingState(state,"START")
        assertEquals(RecordingUiState.RECORDING,state)
        state=transitionRecordingState(state,"TAP")
        assertEquals(RecordingUiState.STOPPING,state)
        state=transitionRecordingState(state,"FINALIZED")
        assertEquals(RecordingUiState.IDLE,state)
    }
    @Test fun repeatTapDoesNotDoubleStart() {
        assertEquals(RecordingUiState.PREPARING,
            transitionRecordingState(RecordingUiState.PREPARING,"TAP"))
    }
    @Test fun failureAllowsRetry() {
        assertEquals(RecordingUiState.PREPARING,
            transitionRecordingState(RecordingUiState.ERROR,"TAP"))
    }
}
