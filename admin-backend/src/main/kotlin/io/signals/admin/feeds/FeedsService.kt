package io.signals.admin.feeds

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
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
import kotlin.reflect.KClass

@RestController
class FeedsService(
    private val repo: SignalRepository,
    private val validator: Validator,
    private val objectMapper: ObjectMapper,
    private val executor: FeedExecutor
) {

    private val logger = KotlinLogging.logger {}

    @PostMapping("/feeds/test")
    suspend fun testSignal(@RequestBody @Valid spec: FeedSpec): List<SignalParseResult> {
        validateConfig(spec)
        return executor.execute(spec, 3)
    }

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
        return executor.executeAsFlux(feed, limit)
            .map { parseResult ->
                ServerSentEvent.builder<SignalParseResult>()
                    .data(parseResult)
                    .build()
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
    fun createSignal(@RequestBody @Valid spec: FeedSpec): FeedSpec {
        validateConfig(spec)
        return repo.save(spec)
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
    @Lob
    @Convert(converter = AnyJsonConverter::class)
    @Column
    val config: Map<String, Any>,

    @Lob
    @Convert(converter = ExtractionListConverter::class)
    @Column
    val extractions: List<Extraction>,

    @Column
    @field:Min(0)
    val cacheDurationSeconds: Int,

    @Column
    @field:NotBlank
    @field:Size(max = 100)
    @field:Pattern(regexp = VALID_PATH_REGEX)
    val apiEndpoint: String,

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column
    val id: String? = null,
) {
    companion object {
        /**
         *  * Must only contain letters that are valid within a URL
         *  * Must not contain query strings
         *  * Must start with a slash
         *  * Must not contain any other slashes other than the one at the start.
         *  * Must not contain a period
         */
        const val VALID_PATH_REGEX:String = "^\\/[a-zA-Z\\-]+\$"
    }
}

interface SignalRepository : JpaRepository<FeedSpec, String>


data class RssFeedConfig(
    @field:NotEmpty(message = "url is required")
    @field:URL(message = "url is not valid")
    val url: String,

    @field:Min(1)
    val pollFrequency: Int,

    val pollPeriod: PollPeriod
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