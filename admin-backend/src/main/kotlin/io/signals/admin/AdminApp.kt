package io.signals.admin

import io.ktor.client.*
import io.signals.Http
import io.signals.SignalParser
import io.signals.openai.OpenAiClient
import io.signals.sources.rss.RssFeedReader
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@SpringBootApplication
class AdminApp {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(AdminApp::class.java)
        }
    }
}

@Configuration
class SignalEngineConfig {
    @Bean()
    fun httpClient(): HttpClient = Http.httpClient

    @Bean()
    fun openAiClient(httpClient: HttpClient, @Value("\${signals.openai.api-key}") openAiApiKey: String): OpenAiClient {
        return OpenAiClient(httpClient, apiKey = openAiApiKey)
    }

    @Bean
    fun rssFeedReader(httpClient: HttpClient) = RssFeedReader(httpClient)

    @Bean
    fun signalScanner(openAiClient: OpenAiClient): SignalParser = SignalParser(openAiClient)

}

@Configuration
class WebConfig : WebFluxConfigurer {
    @Value("\${signals.cors.enabled:true}")
    var corsEnabled: Boolean = true

    private val logger = KotlinLogging.logger {}
    override fun addCorsMappings(registry: CorsRegistry) {
        if (!corsEnabled) {
            logger.warn { "CORS is disabled.  Allowing all access" }
            registry
                .addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .exposedHeaders("*")
        }
    }
}