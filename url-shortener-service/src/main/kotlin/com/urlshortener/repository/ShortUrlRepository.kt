package com.urlshortener.repository

import com.urlshortener.model.ShortUrl
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ShortUrlRepository : JpaRepository<ShortUrl, Long> {
    fun findByShortKey(shortKey: String): ShortUrl?

    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.shortKey = :shortKey")
    fun incrementClickCount(shortKey: String)

    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + :delta WHERE s.shortKey = :shortKey")
    fun addClickCount(shortKey: String, delta: Long)
}
