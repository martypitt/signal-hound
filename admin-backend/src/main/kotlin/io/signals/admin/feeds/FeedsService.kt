package io.signals.admin.feeds

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.http.*
import io.signals.Extraction
import io.signals.SignalParseResult
import io.signals.admin.BadRequestException
import io.signals.admin.NotFoundException
import io.signals.admin.jpa.AnyJsonConverter
import io.signals.admin.jpa.ExtractionListConverter
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import mu.KotlinLogging
import org.hibernate.validator.constraints.URL
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@RestController
class FeedsService(
    private val repo: FeedSpecRepository,
    private val validator: Validator,
    private val objectMapper: ObjectMapper,
    private val executor: FeedExecutor,
    private val schemaGenerator: OpenAPISchemaGenerator
) {

    private val logger = KotlinLogging.logger {}

    private val feedCacheMap = ConcurrentHashMap<FeedSpec, Cache<String, Flux<SignalParseResult>>>()

    @PostMapping("/feeds/test")
    suspend fun testSignal(@RequestBody @Valid spec: FeedSpec): List<SignalParseResult> {
        validateConfig(spec)
        return executor.execute(spec, 3)
    }

    @GetMapping("/feeds/api/{apiEndpoint}")
    fun executeFeedByName(
        @PathVariable("apiEndpoint") apiEndpoint: String,
        @RequestParam(
            "limit",
            required = false,
            defaultValue = Int.MAX_VALUE.toString()
        ) limit: Int = Int.MAX_VALUE
    ): Flux<SignalParseResult> {
        val feedSpec = lookupFeedSpecByApiEndpoint(apiEndpoint)
        return executeFeed(feedSpec, limit)
    }

    private fun lookupFeedSpecByApiEndpoint(apiEndpoint: String): FeedSpec {
        val apiEndpointWithPrefix = "/$apiEndpoint"
        val feedSpec = repo.findByApiEndpoint(apiEndpointWithPrefix)
            ?: throw NotFoundException("No feed defined at /feeds/api${apiEndpointWithPrefix}")
        return feedSpec
    }

    @GetMapping("/feeds/api/{apiEndpoint}/open-api", produces = ["application/x-yaml"])
    fun getOpenApiSpec(@PathVariable("apiEndpoint") apiEndpoint: String):YamlString {
        val feedSpec = lookupFeedSpecByApiEndpoint(apiEndpoint)
        return schemaGenerator.generateForFeed(feedSpec)
    }
    @GetMapping("/feeds/api/{apiEndpoint}/open-api", produces = ["application/x-yaml"], params = ["refresh"])
    suspend fun refreshFeedSchema(@PathVariable("apiEndpoint") apiEndpoint: String):YamlString {
        val feedSpec = lookupFeedSpecByApiEndpoint(apiEndpoint)
        return refreshFeedSchema(feedSpec)
    }

    private suspend fun refreshFeedSchema(feedSpec: FeedSpec): YamlString {
        val (yaml, components) = schemaGenerator.generateModelSchemaForSpec(feedSpec)
        schemaGenerator.saveForSpec(feedSpec, yaml)
        return schemaGenerator.generateForFeed(feedSpec)
    }

    /**
     * Produces SSE
     */
    @GetMapping("/feeds/{id}/stream")
    fun executeFeed(
        @PathVariable("id") feedId: String,
        @RequestParam(
            "limit",
            required = false,
            defaultValue = Int.MAX_VALUE.toString()
        ) limit: Int = Int.MAX_VALUE
    ): Flux<ServerSentEvent<SignalParseResult>> {
        val feed = getFeed(feedId)
        return executeFeed(feed, limit)
            .map { parseResult ->
                ServerSentEvent.builder<SignalParseResult>()
                    .data(parseResult)
                    .build()
            }
    }

    private fun executeFeed(
        feed: FeedSpec,
        limit: Int
    ): Flux<SignalParseResult> {
        val cache = getOrBuildFeedCache(feed)

        return cache.get(feed.id!!) {
            executor.executeAsFlux(feed, limit)
                .replay() // replay, rather than cache, as we want the completion events replayed too.
                .autoConnect()
        }

    }

    private fun getOrBuildFeedCache(feed: FeedSpec): Cache<String, Flux<SignalParseResult>> {
        return feedCacheMap.getOrPut(feed) {
            CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(feed.cacheDurationSeconds.toLong()))
                .build<String, Flux<SignalParseResult>>()
        }
    }

    @GetMapping("/feeds/{id}")
    fun getFeed(@PathVariable("id") feedId: String): FeedSpec {
        return repo.findByIdOrNull(feedId) ?: throw NotFoundException("Feed $feedId does not exist")
    }

    @GetMapping("/feeds")
    fun listSignals(): List<FeedSpec> {
        return repo.findAll()
    }

    @PutMapping("/feeds")
    suspend fun createFeed(@RequestBody @Valid spec: FeedSpec): FeedSpec {
        validateConfig(spec)
        val feedSpec = repo.save(spec)
        try {
            refreshFeedSchema(feedSpec)
        } catch (e: Exception) {
            logger.warn { "Failed to generate schema for ${spec.id}" }
        }
        return feedSpec
    }


    private fun validateConfig(spec: FeedSpec) {
        spec.type.createTypedConfig(spec.config, objectMapper, validator)
    }
}

@Entity
data class FeedSpec(
    @field:NotBlank(message = "Must not be blank")
    @field:Size(message = "Title must be between 1 and 255 characters long", min = 1, max = 255)
    @Column
    val title: String,

    @Enumerated(value = EnumType.STRING)
    val type: FeedType,
    @Convert(converter = AnyJsonConverter::class)
    @Column(length = 5000)
    val config: Map<String, Any>,

    @Convert(converter = ExtractionListConverter::class)
    @Column(length = 5000)
    val extractions: List<Extraction>,

    @Column
    @field:Min(0)
    val cacheDurationSeconds: Int,

    @Column(unique = true)
    @field:NotBlank
    @field:Size(max = 100)
    @field:Pattern(regexp = VALID_PATH_REGEX)
    val apiEndpoint: String,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column
    val id: String? = null,
) {
    val apiPath: String
        get() = "/feeds/api$apiEndpoint"

    val openApiSpecPath: String
        get() = "/feeds/api${apiEndpoint}/open-api"
    val regenerateApiSpecPath: String
        get() = "/feeds/api${apiEndpoint}/open-api?refresh"

    companion object {
        /**
         *  * Must only contain letters that are valid within a URL
         *  * Must not contain query strings
         *  * Must start with a slash
         *  * Must not contain any other slashes other than the one at the start.
         *  * Must not contain a period
         */
        const val VALID_PATH_REGEX: String = "^\\/[a-zA-Z\\-]+\$"
    }
}

interface FeedSpecRepository : JpaRepository<FeedSpec, String> {
    fun findByApiEndpoint(apiEndpoint: String): FeedSpec?
}


data class RssFeedConfig(
    @field:NotEmpty(message = "url is required")
    @field:URL(message = "url is not valid")
    val url: String,

//    @field:Min(1)
//    val pollFrequency: Int,
//
//    val pollPeriod: PollPeriod
)

enum class FeedType(val configClass: KClass<*>) {
    RssFeed(RssFeedConfig::class);

    fun createTypedConfig(config: Map<String, Any>, objectMapper: ObjectMapper, validator: Validator): Any {
        val typedConfig = try {
            objectMapper.convertValue(config, configClass.java)
        } catch (e: IllegalArgumentException) {
            when (val cause = e.cause) {
                is MissingKotlinParameterException -> {
                    throw BadRequestException("${configClass.simpleName} is invalid: Parameter ${cause.parameter.name} not provided")
                }

                else -> BadRequestException(e.message!!)
            }
        }
        val validationResult = validator.validate(typedConfig)
        if (validationResult.isNotEmpty()) {
            throw BadRequestException(validationResult.joinToString { it.message })
        }
        return typedConfig
    }
}

enum class PollPeriod {
    Seconds,
    Minutes,
    Hours,
}