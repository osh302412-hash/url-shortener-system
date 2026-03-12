package com.urlshortener.controller

import com.urlshortener.model.CreateUrlRequest
import com.urlshortener.model.CreateUrlResponse
import com.urlshortener.service.UrlShortenerService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UrlShortenerController(
    private val urlShortenerService: UrlShortenerService
) {
    @PostMapping("/api/v1/urls")
    fun createShortUrl(@Valid @RequestBody request: CreateUrlRequest): ResponseEntity<CreateUrlResponse> {
        val response = urlShortenerService.createShortUrl(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{key}")
    fun redirect(@PathVariable key: String): ResponseEntity<Void> {
        val longUrl = urlShortenerService.resolve(key)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, longUrl)
            .build()
    }
}
