package br.com.rb8digital.rbcinecam.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureTransactionPolicyTest {
    @Test
    fun `requested state is not committed before successful bind`() {
        val current = CaptureConfiguration("FHD", 30)
        val requested = CaptureConfiguration("4K", 60)
        val pending = CaptureTransactionPolicy.begin(current, requested)
        assertEquals(current, pending.applied)
        assertEquals(requested, pending.requested)
    }

    @Test
    fun `success commits requested state`() {
        val current = CaptureConfiguration("FHD", 30)
        val requested = CaptureConfiguration("4K", 30)
        val committed = CaptureTransactionPolicy.success(CaptureTransactionPolicy.begin(current, requested))
        assertEquals(requested, committed.applied)
    }

    @Test
    fun `failure preserves previous applied state`() {
        val current = CaptureConfiguration("FHD", 30)
        val requested = CaptureConfiguration("4K", 60)
        val rolledBack = CaptureTransactionPolicy.failure(CaptureTransactionPolicy.begin(current, requested))
        assertEquals(current, rolledBack.applied)
    }
}
