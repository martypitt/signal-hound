package io.signals

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.signals.sources.rss.RssFeedReader

class RssFeedReaderTest : DescribeSpec({

    describe("Fetching RSS feeds") {
        it("should load and parse an RSS feed") {
            val result = RssFeedReader().read("https://techcrunch.com/feed", 5)
            result.articles.shouldNotBeEmpty()
        }
    }
})