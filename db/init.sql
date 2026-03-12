CREATE TABLE IF NOT EXISTS short_urls (
    id BIGSERIAL PRIMARY KEY,
    short_key VARCHAR(10) NOT NULL UNIQUE,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expire_at TIMESTAMP,
    click_count BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_short_urls_short_key ON short_urls (short_key);
CREATE INDEX idx_short_urls_expire_at ON short_urls (expire_at) WHERE expire_at IS NOT NULL;
