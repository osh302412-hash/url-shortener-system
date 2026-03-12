package com.urlshortener.util

object Base62Encoder {
    private const val CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val BASE = 62

    fun encode(value: Long): String {
        require(value >= 0) { "Value must be non-negative" }
        if (value == 0L) return CHARSET[0].toString()

        var num = value
        val sb = StringBuilder()
        while (num > 0) {
            sb.append(CHARSET[(num % BASE).toInt()])
            num /= BASE
        }
        return sb.reverse().toString()
    }
}
