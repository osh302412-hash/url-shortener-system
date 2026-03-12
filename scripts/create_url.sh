#!/bin/bash
# Create a short URL
URL="${1:-https://www.google.com/search?q=url+shortener+system+design}"

echo "Creating short URL for: $URL"
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\": \"$URL\"}" | python3 -m json.tool 2>/dev/null || \
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\": \"$URL\"}"
