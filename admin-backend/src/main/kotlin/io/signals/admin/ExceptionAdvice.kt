package  io.signals.admin

import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class ExceptionAdvice {

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationErrors(ex: WebExchangeBindException):ResponseEntity<BadRequestErrorMessage> {
        val errors = ex.bindingResult.allErrors.joinToString { error -> error.defaultMessage ?: "Unknown error" }
        return ResponseEntity(BadRequestErrorMessage(errors), HttpStatus.BAD_REQUEST)
    }
    @ExceptionHandler(DecodingException::class)
    fun handleDecodingErrors(ex: DecodingException):ResponseEntity<BadRequestErrorMessage> {
        val errors = ex.mostSpecificCause.message ?: "Failed to decode the request"
        return ResponseEntity(BadRequestErrorMessage(errors), HttpStatus.BAD_REQUEST)
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)

data class BadRequestErrorMessage(
    val error:String
)