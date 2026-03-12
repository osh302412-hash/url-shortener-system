#!/bin/bash
# Test redirect for a given short key
KEY="${1}"

if [ -z "$KEY" ]; then
  echo "Usage: ./redirect_test.sh <short_key>"
  echo "Example: ./redirect_test.sh abc123X"
  exit 1
fi

echo "Testing redirect for key: $KEY"
curl -v http://localhost:8080/$KEY 2>&1 | grep -E "< HTTP|< Location"
