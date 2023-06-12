package io.signals

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.CaseFormat
import io.signals.openai.*
import io.signals.sources.SignalBatch
import io.signals.sources.SignalSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.mono
import mu.KotlinLogging
import reactor.core.publisher.Flux
import java.lang.Exception

class SignalParser(
    private val openAiClient: OpenAiClient,
    private val objectMapper: ObjectMapper = Json.defaultMapper
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun parseSignals(
        signalBatch: SignalBatch,
        extractions: List<Extraction>
    ): List<SignalParseResult> {
        return parseSignals(signalBatch.signals, extractions)
    }

    fun mapSignals(signals: Flux<SignalSource>, extractions: List<Extraction>): Flux<SignalParseResult> {
        val systemPrompt = buildSystemPrompt(extractions)
        return signals.flatMap { signal ->
            mono { parseSignal(signal, systemPrompt) }
        }
    }

    suspend fun parseSignals(
        signals: List<SignalSource>,
        extractions: List<Extraction>
    ): List<SignalParseResult> {
        val systemPrompt = buildSystemPrompt(extractions)
        val parsed = coroutineScope {

            signals
                .map { signalSource ->
                    async {
                        parseSignal(signalSource, systemPrompt)
                    }
                }
        }.awaitAll()
        return parsed
    }

    private suspend fun parseSignal(
        signalSource: SignalSource,
        systemPrompt: String
    ): SignalParseResult {
        return try {
            logger.info { "Attempting enrichment of signal ${signalSource.id}" }
            val question = systemPrompt + "\n\n\n" + signalSource.text()

            val completionsResponse = openAiClient.sendCompletion(question)
            val signals = readSignalsFromCompletionResponse(completionsResponse)

            logger.info { "Signal ${signalSource.id} enriched successfully" }
            SignalParseResult.success(signalSource, signalSource.signalUri, signals)
        } catch (e: OpenAiApiException) {
            logger.warn { "Signal ${signalSource.id} error at OpenAPI: ${e.message!!}.  Source: ${signalSource.signalUri}" }
            SignalParseResult.failed(signalSource, signalSource.signalUri, e.error.error.message)
        } catch (e: Exception) {
            logger.warn { "Signal ${signalSource.id} failed to provide signals: ${e.message!!}.  Source: ${signalSource.signalUri}" }
            SignalParseResult.failed(signalSource, signalSource.signalUri, e.message!!)
        }
    }

    private fun buildSystemPrompt(extractions: List<Extraction>) =
        """You are a system that provides article summaries in JSON.
    
    The user will provide the text of an article.  You are to provide a response in JSON.  Only provide JSON, no other content.
    
    The JSON should follow this schema:
    
    ```
    ${extractionsToMarkdownTable(extractions)}
    ```
    """


    private fun readSignalsFromCompletionResponse(chatResponse: OpenAiCompletionsResponse): Map<String, Any> {
        val firstChoice = chatResponse.choices.first()
        return parseJsonIsh(firstChoice.text)
    }

    private fun parseJsonIsh(content: String): Map<String, Any> {
        val jsonIsh = content.trim().removeSurrounding("```")
        try {
            return objectMapper.readValue<Map<String, Any>>(jsonIsh)
        } catch (e: Exception) {
            throw RuntimeException("Didn't receive valid JSON.  Response: $jsonIsh")
        }
    }

    private fun readSignalsFromChatResponse(chatResponse: OpenAiChatResponse): Map<String, Any> {
        val choice = chatResponse.choices.first()
        return parseJsonIsh(choice.message.content)

    }
}

fun extractionsToMarkdownTable(extractions: List<Extraction>):String {
    val header = listOf("Field name", "Description", "Example")
        .asMarkdownTableRow()
    val seperatorToken = "-----------------"
    val seperator = listOf(seperatorToken, seperatorToken, seperatorToken)
        .asMarkdownTableRow(appendWhitespace = false)
    val body = extractions.joinToString("\n") { extraction ->
        val fieldName = if (extraction.title.contains(" ")) {
            CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, extraction.title.uppercase().replace(" ", "_"))
        } else {
            extraction.title
        }

        listOf(fieldName, extraction.description, extraction.sample)
            .asMarkdownTableRow()
    }

    return listOf(header, seperator, body)
        .joinToString("\n")
}

private fun List<String>.asMarkdownTableRow(appendWhitespace: Boolean = true): String {
    val padded = if (appendWhitespace) {
        this.map { "$it            " }
    } else {
        this
    }

    return padded.joinToString("|", prefix = "|", postfix = "|")
}