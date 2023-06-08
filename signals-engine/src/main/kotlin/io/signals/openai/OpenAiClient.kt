package io.signals.openai

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging
import java.math.BigDecimal
import com.fasterxml.jackson.module.kotlin.readValue
import io.signals.Json

class OpenAiClient(
    private val httpClient: HttpClient = HttpClient(CIO),
    private val apiKey: String,
    private val apiEndpoint: String = "https://api.openai.com/v1",
    private val objectMapper: ObjectMapper = Json.defaultMapper
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val baseUrl = apiEndpoint.removeSuffix("/")
    private fun url(endpoint: String) = "${baseUrl}/${endpoint.removePrefix("/")}"

    suspend fun sendChat(chatRequest: OpenAiChatRequest): OpenAiChatResponse {
        logger.debug { "Sending Chat request to OpenAI" }
        val bodyJson = objectMapper.writeValueAsString(chatRequest)
        val openAiResponse = httpClient.post(url("/chat/completions")) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(bodyJson)
        }
        logger.debug { "OpenAI Response: ${openAiResponse.status}" }
        val jsonResponse = openAiResponse.bodyAsText()
        return objectMapper.readValue<OpenAiChatResponse>(jsonResponse)
    }

    suspend fun sendCompletion(question: String): OpenAiCompletionsResponse {
        val request = OpenAiCompletionRequest(question)
        logger.debug { "Sending request to OpenAI" }
        val openAiResponse = httpClient.post(url("/completions")) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(request))
        }
        logger.debug { "OpenAI Response: ${openAiResponse.status}" }
        val jsonResponse = openAiResponse.bodyAsText()
        return objectMapper.readValue<OpenAiCompletionsResponse>(jsonResponse)
    }

    private fun parseResponse(httpResponse: HttpResponse) {

    }
}

data class OpenAiCompletionRequest(
    val prompt: String,
    val model: String = OpenAiModel.TEXT_DAVINCI_003,
    val temperature: BigDecimal = BigDecimal(0.3),
    val max_tokens: Int = 1000,
//   val top_p: Int = 1,
//   val frequency_penalty: BigDecimal = BigDecimal(0),
//   val presence_penalty: BigDecimal = BigDecimal.ZERO
)

data class OpenAiChatRequest(
    val messages: List<OpenAiChatMessage>,
    val model: String = OpenAiModel.GPT_3_5_TURBO,
    val temperature: BigDecimal = BigDecimal(1),
//   val max_tokens: Int = 1000,
    val top_p: Int = 1,
//   val frequency_penalty: BigDecimal = BigDecimal(0),
    val presence_penalty: BigDecimal = BigDecimal.ZERO
)

object OpenAiModel {
    const val GPT_4 = "gpt-4"
    const val GPT_4_0314 = "gpt-4-0314"
    const val GPT_4_32K = "gpt-4-32k"
    const val GPT_4_32K_0314 = "gpt-4-32k-0314"
    const val GPT_3_5_TURBO = "gpt-3.5-turbo"
    const val GPT_3_5_TURBO_0301 = "gpt-3.5-turbo-0301"
    const val TEXT_DAVINCI_003 = "text-davinci-003"
    const val TEXT_DAVINCI_002 = "text-davinci-002"
    const val TEXT_CURIE_001 = "text-curie-001"
    const val TEXT_BABBAGE_001 = "text-babbage-001"
    const val TEXT_ADA_001 = "text-ada-001"
    const val TEXT_DAVINCI_EDIT_001 = "text-davinci-edit-001"
    const val CODE_DAVINCI_EDIT_001 = "code-davinci-edit-001"
    const val WHISPER_1 = "whisper-1"
    const val DAVINCI = "davinci"
    const val CURIE = "curie"
    const val BABBAGE = "babbage"
    const val ADA = "ada"
    const val TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002"
    const val TEXT_SEARCH_ADA_DOC_001 = "text-search-ada-doc-001"
    const val TEXT_MODERATION_STABLE = "text-moderation-stable"
    const val TEXT_MODERATION_LATEST = "text-moderation-latest"

}

data class OpenAiChatMessage(
    val role: Role,
    val content: String,
) {
    enum class Role {
        system, user, assistant
    }
}

data class OpenAiChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatCompletionChoice>,
    val usage: ChatGptUsage
)

data class OpenAiCompletionsResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<CompletionChoice>,
    val usage: ChatGptUsage

)

data class CompletionChoice(
    val text: String,
    val index: Int,
    val logprobs: Int? = null,
    val finish_reason: String
)

data class ChatCompletionChoice(
    val index: String,
    val message: OpenAiChatMessage,
    val finish_reason: String
)

data class ChatGptUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
