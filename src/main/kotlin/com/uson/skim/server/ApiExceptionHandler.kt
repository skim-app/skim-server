package com.uson.skim.server

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MultipartException

data class ApiErrorResponse(val message: String)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MultipartException::class)
    fun invalidMultipart(exception: MultipartException): ResponseEntity<ApiErrorResponse> = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiErrorResponse(exception.message ?: "Invalid multipart request"))
}
