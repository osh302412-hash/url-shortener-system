package com.urlshortener.service

import com.urlshortener.model.CreateUrlRequest
import com.urlshortener.model.CreateUrlResponse
import com.urlshortener.model.ShortUrl
import com.urlshortener.repository.ShortUrlRepository
import com.urlshortener.util.Base62Encoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@Service
class UrlShortenerService(
    private val shortUrlRepository: ShortUrlRepository,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${url-shortener.domain}") private val domain: String,
    @Value("\${url-shortener.cache-ttl-seconds}") private val cacheTtlSeconds: Long
) {
    private val counter = AtomicLong(System.currentTimeMillis())

    companion object {
        private const val CACHE_PREFIX = "url:"
    }

    fun createShortUrl(request: CreateUrlRequest): CreateUrlResponse {
        val shortKey = generateUniqueKey()

        val shortUrl = ShortUrl(
            shortKey = shortKey,
            longUrl = request.longUrl,
            expireAt = request.expireAt
        )
        shortUrlRepository.save(shortUrl)

        cacheUrl(shortKey, request.longUrl, request.expireAt)

        return CreateUrlResponse(
            shortUrl = "$domain/$shortKey",
            key = shortKey
        )
    }

    @Transactional
    fun resolve(key: String): String? {
        // 1. Redis lookup
        val cached = redisTemplate.opsForValue().get("$CACHE_PREFIX$key")
        if (cached != null) {
            shortUrlRepository.incrementClickCount(key)
            return cached
        }

        // 2. DB lookup
        val shortUrl = shortUrlRepository.findByShortKey(key) ?: return null

        // 3. Check expiration
        if (shortUrl.expireAt != null && shortUrl.expireAt.isBefore(LocalDateTime.now())) {
            return null
        }

        // 4. Cache and return
        cacheUrl(key, shortUrl.longUrl, shortUrl.expireAt)
        shortUrlRepository.incrementClickCount(key)
        return shortUrl.longUrl
    }

    private fun generateUniqueKey(): String {
        var key: String
        do {
            val id = counter.incrementAndGet()
            key = Base62Encoder.encode(id)
        } while (shortUrlRepository.findByShortKey(key) != null)
        return key
    }

    private fun cacheUrl(key: String, longUrl: String, expireAt: LocalDateTime?) {
        val ttl = if (expireAt != null) {
            Duration.between(LocalDateTime.now(), expireAt).coerceAtLeast(Duration.ofSeconds(1))
        } else {
            Duration.ofSeconds(cacheTtlSeconds)
        }
        redisTemplate.opsForValue().set("$CACHE_PREFIX$key", longUrl, ttl)
    }


}
