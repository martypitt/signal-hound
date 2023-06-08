package io.signals

import io.signals.sources.SignalSource

data class SignalParseResult(
    val source: SignalSource,
    val success: Boolean,
    val signalUri: String?,
    val errorMessage: String? = null,
    val signals: Map<String, Any>? = null
) {
    companion object {
        fun success(source:SignalSource, signalUri: String?, signals: Map<String,Any>) = SignalParseResult(
            source = source,
            success = true,
            errorMessage = null,
            signalUri = signalUri,
            signals = signals
        )
        fun failed(source:SignalSource, signalUri: String?, errorMessage: String) = SignalParseResult(
            source = source,
            success = false,
            signalUri = signalUri,
            errorMessage = errorMessage,
            signals = null
        )
    }
}