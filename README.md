# URL Shortener System

A URL shortener service built with Kotlin, Spring Boot 3, Redis, and PostgreSQL.

## Architecture

```
Client → Spring Boot → Redis Cache → PostgreSQL
```

- **Cache-Aside pattern**: Redis lookup first, DB fallback, then cache population
- **Base62 encoding**: Timestamp-based unique ID → 7-char short key
- **302 Redirect**: Short URL access redirects to original URL

## Tech Stack

- Kotlin + Spring Boot 3 (JDK 21)
- PostgreSQL 16
- Redis 7
- Docker Compose

## Quick Start

```bash
docker compose up --build
```

## API

### Create Short URL

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://example.com/very/long/url"}'
```

Response:
```json
{
  "shortUrl": "http://localhost:8080/abc123X",
  "key": "abc123X"
}
```

### Redirect

```bash
curl -v http://localhost:8080/{key}
```

Returns `302 Found` with `Location` header pointing to the original URL.

### With Expiration

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://example.com", "expireAt": "2025-12-31T23:59:59"}'
```

## Test Scripts

```bash
# Create a short URL
./scripts/create_url.sh "https://example.com"

# Test redirect
./scripts/redirect_test.sh <key>
```

## Design Decisions

- **Key Generation**: AtomicLong counter + Base62 encoding for collision-free keys
- **Caching**: Redis with TTL based on expiration time (default 24h)
- **Click Counting**: Incremented on each redirect via DB update
- **Scalability**: Stateless app layer, ready for horizontal scaling
