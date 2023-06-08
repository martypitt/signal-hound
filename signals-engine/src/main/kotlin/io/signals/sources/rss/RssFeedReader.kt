package io.signals.sources.rss

import com.rometools.rome.feed.synd.SyndContent
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.util.*
import io.ktor.util.*
import io.signals.sources.SignalBatch
import io.signals.sources.SignalId
import io.signals.sources.SignalSource
import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class RssFeedReader(
    private val httpClient: HttpClient = HttpClient(CIO)

) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @OptIn(InternalAPI::class)
    suspend fun read(url: String, limit: Int = Int.MAX_VALUE): RssFeed {
        logger.info { "Loading RSS feed at $url" }
        val response = httpClient.get(url)

        val input = ByteArrayInputStream(response.readBytes())
        val feed = try {
            SyndFeedInput()
                .build(XmlReader(input)) as SyndFeed
        } catch (e: Exception) {
            logger.error { "Failed to parse RSS Response.  Start of response: ${input.toString().subSequence(0, 50)}" }
            throw e
        }

        val articles = feed.entries.map { entry ->
            Article(
                entry.title,
                entry.author,
                entry.publishedDate.toZonedDateTime(),
                parsePossibleHtml(entry.description),
                parsePossibleHtml(entry.contents.first()),
                entry.uri
            )
        }

        return RssFeed(
            feed.title,
            feed.description,
            articles.subList(0, limit),
            nextRefreshTime = Instant.now().plus(Duration.ofHours(1))

        )

    }

    private fun parsePossibleHtml(content: SyndContent?): String {
        if (content == null) return ""
        return when (content.type.lowercase()) {
            "html" -> parseHtmlToMarkdown(content.value)
            "text/html" -> parseHtmlToMarkdown(content.value)
            else -> content.value
        }
    }

    private fun parseHtmlToMarkdown(value: String?): String {
        if (value == null) return ""
        return try {
            val converter = FlexmarkHtmlConverter.builder().build()
            val markdown = converter.convert(value)
            markdown
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse HTML to markdown" }
            value
        }


    }
}

data class Article(
    val title: String,
    val author: String,
    val publishedDate: ZonedDateTime,
    val description: String,
    val content: String,
    val uri: String
) : SignalSource {
    override val id: SignalId = SignalId("RssArticle", uri)
    override fun text(): String {
        return content
    }

    override val signalUri: String? = uri
}


data class RssFeed(
    val title: String,
    val description: String,
    val articles: List<Article>,
    val nextRefreshTime: Instant?
) : SignalBatch {
    override val signals: List<SignalSource>
        get() = articles
    override val proposedNextRefreshTime: Instant? = nextRefreshTime
}