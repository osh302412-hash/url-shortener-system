package com.urlshortener.model

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class CreateUrlRequest(
    @field:NotBlank(message = "longUrl must not be blank")
    val longUrl: String,
    val expireAt: LocalDateTime? = null
)

data class CreateUrlResponse(
    val shortUrl: String,
    val key: String
)
