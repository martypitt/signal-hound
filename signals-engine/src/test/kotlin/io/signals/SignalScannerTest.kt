package io.signals

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.ktor.client.*
import io.signals.openai.OpenAiClient
import io.signals.sources.rss.RssFeedReader

class SignalScannerTest : DescribeSpec({
    describe("e2e signal scanner test") {
        val httpClient: HttpClient = Http.httpClient
        val openAiClient = OpenAiClient(httpClient, System.getProperty("OPENAI_API_KEY"))
        val rssFeedReader = RssFeedReader(httpClient)
        val signalScanner = SignalParser(openAiClient)
        it("should look for signals in TechCrunch rss feed") {
            val rssFeed = rssFeedReader.read("https://techcrunch.com/feed", limit = 5)
            val extractions = listOf(
                Extraction("companyName", "The name of the company", "Acme Incorporated"),
                Extraction("url", "The website of the company", "https://acme.com"),
                Extraction("companyDescription", "A short summary of what the company does.  Max 250 chars",
                    "Acme is a fictional company that creates things used to blow up a road runner"),
                Extraction("eventDescription", "A very short summary of the article.  Max 250 chars",
                    "Acme has announced their Series B round"),
                Extraction("articleDescribesFundingRound", "Boolean (true/false) indicating if this article describes a funding round",
                    "true"),
                Extraction("fundingRoundName", "The name of the funding round.  Use null if no funding round is present",
                    "Series A, Seed, Series B"),
                Extraction("fundingRoundSize", "The size of the funding round", "$300M"),
                Extraction("keyPeople", "A JSON array of the key people mentioned in the article.",
                    "[ { \"name\" : \"Joe Rogan\", \"title\" : \"CEO\" }, { \"name\" : \"Josh Manpoor\", \"title\" : \"CTO\" } ]"),
                Extraction("requiresConsumingExternalDataFeeds", "Boolean (true/false) indicating if the article suggests the company's mission would require them to consume multiple external data feeds",
                    "true"),
                Extraction("justificationForExternalDataFeeds", "A description summarizing why you believe the company does or does not require consuming external data feeds",
                    "Acme is an aggregator of external financial data")
            )

            val parseResult = signalScanner.parseSignals(rssFeed, extractions)
            parseResult.shouldNotBeEmpty()

        }
    }
})