package io.signals.admin.feeds

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.*
import io.mockk.mockk
import io.signals.Http
import io.signals.openai.OpenAiClient

class OpenApiSchemaGeneratorTest : DescribeSpec({
    describe("Generating Schema for spec") {
        val httpClient: HttpClient = Http.httpClient
        val openAiClient = OpenAiClient(httpClient, System.getenv("OPENAI_API_KEY") ?: "FAKE_API_KEY")
        val schemaGenerator = OpenAPISchemaGenerator(openAiClient, mockk())
        it("should call OpenAI") {
            val feed = SampleFeed.feed
            val (_,components) = schemaGenerator.generateModelSchemaForSpec(feed)
            components.schemas.shouldHaveSize(2)
        }

        val sampleYaml = """
components:
  schemas:
    Company:
      type: object
      properties:
        companyName:
          type: string
        url:
          type: string
        companyDescription:
          type: string
        eventDescription:
          type: string
        articleDescribesFundingRound:
          type: boolean
        fundingRoundName:
          type: string
        fundingRoundSize:
          type: string
        keyPeople:
          type: array
          items:
            type: object
            properties:
              name:
                type: string
              title:
                type: string
        requiresConsumingExternalDataFeeds:
          type: boolean
        justificationForExternalDataFeeds:
          type: string"""

        it("should generate OpenAPI schema") {
            val modelSchema = FeedModelSchema("feedId",sampleYaml, "modelSchemaId" )
            val yaml = schemaGenerator.generateYamlForFeedAndSchema(SampleFeed.feed, modelSchema)
            yaml.shouldNotBeEmpty()
        }

        it("should parse response to YAML") {

            schemaGenerator.parseYamlish(sampleYaml)
        }
    }
})