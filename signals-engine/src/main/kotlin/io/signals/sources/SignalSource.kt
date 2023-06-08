package io.signals.sources

import java.time.Instant

data class SignalId(
    val feedType: String,
    val id: String
) {
    override fun toString(): String {
        return "$feedType:$id"
    }
}

interface SignalSource {
    val id: SignalId
    fun text(): String

    /**
     * A URL that points to the origination of the signal,
     * if possible.
     */
    val signalUri: String?
}

interface SignalBatch {
    val signals: List<SignalSource>
    val proposedNextRefreshTime: Instant?
}