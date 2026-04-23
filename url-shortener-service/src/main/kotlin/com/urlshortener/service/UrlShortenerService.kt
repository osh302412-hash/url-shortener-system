package com.urlshortener.service

import com.urlshortener.model.CreateUrlRequest
import com.urlshortener.model.CreateUrlResponse
import com.urlshortener.model.ShortUrl
import com.urlshortener.repository.ShortUrlRepository
import com.urlshortener.util.Base62Encoder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
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
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CACHE_PREFIX  = "url:"
        private const val CLICK_PREFIX  = "click:"  // write-behind 버퍼
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

    fun resolve(key: String): String? {
        // 1. Redis lookup
        val cached = redisTemplate.opsForValue().get("$CACHE_PREFIX$key")
        if (cached != null) {
            // Write-Behind: DB write 없이 Redis 카운터만 증가 (O(1), non-blocking)
            redisTemplate.opsForValue().increment("$CLICK_PREFIX$key")
            return cached
        }

        // 2. DB lookup (캐시 미스)
        val shortUrl = shortUrlRepository.findByShortKey(key) ?: return null

        // 3. 만료 확인
        if (shortUrl.expireAt != null && shortUrl.expireAt.isBefore(LocalDateTime.now())) {
            return null
        }

        // 4. 캐시 저장 후 반환 (DB click count는 flush 스케줄러가 처리)
        cacheUrl(key, shortUrl.longUrl, shortUrl.expireAt)
        redisTemplate.opsForValue().increment("$CLICK_PREFIX$key")
        return shortUrl.longUrl
    }

    // 10초마다 Redis에 쌓인 클릭 수를 DB에 일괄 반영 (Write-Behind flush)
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    fun flushClickCounts() {
        val keys = redisTemplate.keys("$CLICK_PREFIX*")
        if (keys.isNullOrEmpty()) return

        var flushed = 0
        for (redisKey in keys) {
            val countStr = redisTemplate.opsForValue().getAndDelete(redisKey) ?: continue
            val count = countStr.toLongOrNull() ?: continue
            if (count <= 0) continue

            val shortKey = redisKey.removePrefix(CLICK_PREFIX)
            shortUrlRepository.addClickCount(shortKey, count)
            flushed++
        }
        if (flushed > 0) log.debug("Click flush: {}개 key, DB 반영 완료", flushed)
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
