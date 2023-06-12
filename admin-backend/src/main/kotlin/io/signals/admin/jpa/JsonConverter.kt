package io.signals.admin.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.signals.Extraction
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

abstract class JsonConverter<T> : AttributeConverter<T, String> {
   companion object {
      val objectMapper = jacksonObjectMapper()
   }

   override fun convertToDatabaseColumn(attribute: T?): String? {
      if (attribute == null) {
         return null
      }
      return objectMapper.writeValueAsString(attribute)
   }

   abstract fun fromJson(json: String): T
   override fun convertToEntityAttribute(dbData: String?): T? {
      if (dbData == null) {
         return null
      }
      return fromJson(dbData)
   }
}

/**
 * Converts Json to / from an Any.
 * Writing out, can be anything, but will always be read back as either
 * Map<String,Any> or List<Map<String,Any>>
 */
@Converter
class AnyJsonConverter : JsonConverter<Any>() {
   override fun fromJson(json: String): Any = objectMapper.readValue(json)
}

@Converter
class ExtractionListConverter : JsonConverter<List<Extraction>>() {
   override fun fromJson(json: String): List<Extraction> = objectMapper.readValue(json)
}