package io.signals

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Json {
    val defaultMapper = jacksonObjectMapper()
}