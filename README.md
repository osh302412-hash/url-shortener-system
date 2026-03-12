# URL Shortener System

Kotlin, Spring Boot 3, Redis, PostgreSQL 기반의 URL 단축 서비스입니다.

## 아키텍처

```
Client → Spring Boot → Redis Cache → PostgreSQL
```

- **Cache-Aside 패턴**: Redis 우선 조회 → DB 폴백 → 캐시 저장
- **Base62 인코딩**: 타임스탬프 기반 고유 ID → 7자 short key 생성
- **302 Redirect**: 짧은 URL 접근 시 원본 URL로 리다이렉트

## 기술 스택

- Kotlin + Spring Boot 3 (JDK 21)
- PostgreSQL 16
- Redis 7
- Docker Compose

## 실행 방법

```bash
docker compose up --build
```

## API

### URL 생성

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://example.com/very/long/url"}'
```

응답:
```json
{
  "shortUrl": "http://localhost:8080/abc123X",
  "key": "abc123X"
}
```

### 리다이렉트

```bash
curl -v http://localhost:8080/{key}
```

`302 Found` 상태 코드와 함께 `Location` 헤더에 원본 URL이 포함되어 응답됩니다.

### 만료 시간 설정

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://example.com", "expireAt": "2025-12-31T23:59:59"}'
```

## 테스트 스크립트

```bash
# URL 생성
./scripts/create_url.sh "https://example.com"

# 리다이렉트 테스트
./scripts/redirect_test.sh <key>
```

## 설계 결정 사항

- **Key 생성**: AtomicLong 카운터 + Base62 인코딩으로 충돌 없는 키 생성
- **캐싱**: 만료 시간 기반 TTL을 적용한 Redis 캐시 (기본 24시간)
- **클릭 수 집계**: 리다이렉트 시마다 DB 업데이트를 통해 증가
- **확장성**: 무상태(Stateless) 앱 레이어로 수평 확장 가능
