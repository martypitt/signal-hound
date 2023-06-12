package io.signals.admin.feeds

import com.fasterxml.jackson.databind.ObjectMapper
import io.signals.SignalParseResult
import io.signals.SignalParser
import io.signals.sources.SignalBatch
import io.signals.sources.rss.RssFeedReader
import jakarta.validation.Validator
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

// TODO :  We can refactor this to a cleaner strategy / factory type impl. if neded
@Component
class FeedExecutor(
    private val parser: SignalParser,
    private val validator: Validator,
    private val objectMapper: ObjectMapper,
    private val rssFeedReader: RssFeedReader
) {


    suspend fun execute(spec: FeedSpec, limit: Int = Int.MAX_VALUE): List<SignalParseResult> {
        val signals = getFeed(spec)
        return parser.parseSignals(
            signals.signals.subList(0, limit),
            spec.extractions
        )
    }

    fun executeAsFlux(spec: FeedSpec, limit: Int = Int.MAX_VALUE): Flux<SignalParseResult> {
        val signals = mono { getFeed(spec) }
            .flatMapIterable { feed -> feed.signals.subList(0, minOf(limit, feed.signals.size)) }

        return parser.mapSignals(signals, spec.extractions)
    }

    private suspend fun getFeed(spec: FeedSpec): SignalBatch {
        val config = spec.type.createTypedConfig(spec.config, objectMapper, validator)
        return when (spec.type) {
            FeedType.RssFeed -> {
                val rssConfig = config as RssFeedConfig
                rssFeedReader.read(rssConfig.url)
            }
        }
    }
}