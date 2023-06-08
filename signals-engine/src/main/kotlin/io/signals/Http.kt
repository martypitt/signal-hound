package io.signals

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.http.*
import mu.KotlinLogging
import java.time.Duration

object Http {
    private val logger = KotlinLogging.logger {}
    val httpClient = HttpClient(CIO) {
//        install(ContentEncoding) {
//            deflate()
//            gzip()
//        }
        engine {
            requestTimeout = Duration.ofSeconds(90).toMillis()
        }
//        install(HttpRequestRetry) {
//            constantDelay(millis = 100)
//            retryIf(maxRetries = 5) { request, response ->
//                if (!response.status.isSuccess()) {
//                    logger.info("Request to ${request.url} failed with response ${response.status}.")
//                    true
//                } else {
//                    false
//                }
//            }
//        }

    }
}