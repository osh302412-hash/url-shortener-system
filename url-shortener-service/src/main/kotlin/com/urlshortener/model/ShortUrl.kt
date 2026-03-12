package com.urlshortener.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "short_urls")
class ShortUrl(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "short_key", nullable = false, unique = true, length = 10)
    val shortKey: String,

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    val longUrl: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expire_at")
    val expireAt: LocalDateTime? = null,

    @Column(name = "click_count", nullable = false)
    var clickCount: Long = 0
)
