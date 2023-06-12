package io.signals.admin.feeds

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import io.signals.admin.NotFoundException
import io.signals.extractionsToMarkdownTable
import io.signals.openai.OpenAiClient
import io.signals.openai.OpenAiCompletionsResponse
import io.swagger.v3.core.util.Yaml31
import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

typealias YamlString = String

@Component
class OpenAPISchemaGenerator(
    private val openAiClient: OpenAiClient,
    private val repository: FeedModelSchemaRepository,

    ) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun generateForFeed(feedSpec: FeedSpec): YamlString {
        val schema = repository.findByFeedId(feedId = feedSpec.id!!) ?: throw NotFoundException("No schema has been generated for feed ${feedSpec.id}")
        return generateYamlForFeedAndSchema(feedSpec, schema)
    }

    fun generateForFeedAndSchema(feedSpec: FeedSpec, schema: FeedModelSchema): OpenAPI {
        val openApi = OpenAPI()
        openApi.info = Info().title(feedSpec.title).version("1.0.0")
        val components = parseYamlish(schema.yaml)
        val paths = Paths()

        val description = "Returns the most recent parse result from ${feedSpec.title}"
        val defaultPathItem = PathItem()
        val defaultOperation = Operation()
            .description(description)
        val responses = ApiResponses()

        responses["200"] = ApiResponse().let { apiResponse ->
            apiResponse.description(description)
            apiResponse.content(Content().let { content ->
                content.addMediaType("application/json", MediaType().let { mediaType ->
                    mediaType.schema(Schema<String>().let { schema ->
                        schema.`$ref`("#/components/schemas/${components.schemas.keys.first()}")
                    })
                })
            })
        }
        defaultOperation.responses = responses
        defaultPathItem.get = defaultOperation
        paths[feedSpec.apiPath] = defaultPathItem
        defaultPathItem.get = defaultOperation
        openApi.paths = paths
        openApi.components = components

        return openApi
    }

    fun generateYamlForFeedAndSchema(feedSpec: FeedSpec, schema: FeedModelSchema): YamlString {
        val openApi = generateForFeedAndSchema(feedSpec, schema)
        return Yaml31.pretty().writeValueAsString(openApi)
    }


    @Transactional
    fun saveForSpec(feedSpec: FeedSpec, yaml: YamlString): FeedModelSchema {
        val deleted = repository.deleteAllByFeedId(feedSpec.id!!)
        logger.info { "Removed $deleted old feed schemas" }
        val schema = repository.save(
            FeedModelSchema(
                feedSpec.id!!,
                yaml,

                )
        )
        logger.info { "Created schema ${schema.id} for feed ${feedSpec.id} (${feedSpec.title})" }
        return schema
    }

    suspend fun generateModelSchemaForSpec(feedSpec: FeedSpec): Pair<YamlString, Components> {
        return try {
            val question = buildPrompt(feedSpec)
            logger.info { "Attempting to generate API spec for ${feedSpec.id} (${feedSpec.title})" }

            val completionsResponse = openAiClient.sendCompletion(question)
            extractOpenApiSpec(completionsResponse)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to generate API Spec for ${feedSpec.id}" }
            throw e
        }


    }

    private fun extractOpenApiSpec(chatResponse: OpenAiCompletionsResponse): Pair<YamlString, Components> {
        val firstChoice = chatResponse.choices.first()
        val sanitized = sanitize(firstChoice.text)
        return sanitized to parseYamlish(sanitized)
    }

    private fun sanitize(text: String): String {
        return text.removeSurrounding("```")
    }

    fun parseYamlish(text: String): Components {
        logger.info { "Attempting to parse OpenAI response as OpenAPI components section" }
        try {
            val textAsObject = Yaml31.mapper().readValue<Map<String, Any>>(text)
            require(textAsObject.containsKey("components")) { "Expected the root value from OpenAI would be a key of Components.  Instead, got: \n$text" }
            val components = Yaml31.mapper()
                .convertValue<Components>(textAsObject["components"]!!)
            if (components.schemas.isEmpty()) {
                logger.warn { "Parsing of response from OpenAI looks suspicious, as no schemas are present." }
            } else {
                logger.info { "Successfully read ${components.schemas.size} schemas: ${components.schemas.keys.joinToString()}" }
            }

            return components
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse OpenAI response as OpenAPI components section.  ${e.message}" }
            throw e
        }
    }

    private fun buildPrompt(feedSpec: FeedSpec): String =
        """Suggest a data model for the following table, using the OpenAPI spec for Components as the data model.  Include the provided description in the OpenAPI description property of fields where possible.  Include the definitions of nested array objects where possible. Return the result as a subset of an OpenAPI spec (in YAML), returning just the components section.

${extractionsToMarkdownTable(feedSpec.extractions)}
        """
}