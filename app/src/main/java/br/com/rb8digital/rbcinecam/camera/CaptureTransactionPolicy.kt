package br.com.rb8digital.rbcinecam.camera

data class CaptureTransaction(
    val previous: CaptureConfiguration,
    val requested: CaptureConfiguration,
    val applied: CaptureConfiguration
)

object CaptureTransactionPolicy {
    fun begin(current: CaptureConfiguration, requested: CaptureConfiguration) =
        CaptureTransaction(previous = current, requested = requested, applied = current)

    fun success(transaction: CaptureTransaction) =
        transaction.copy(applied = transaction.requested)

    fun failure(transaction: CaptureTransaction) =
        transaction.copy(applied = transaction.previous)
}
