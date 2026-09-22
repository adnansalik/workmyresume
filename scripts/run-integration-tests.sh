#!/usr/bin/env bash
# Runs the database-backed test suite against a throwaway PostgreSQL container,
# keeping the dev database untouched.
#
# Usage:  scripts/run-integration-tests.sh [extra mvn args...]
set -euo pipefail

cd "$(dirname "$0")/../backend"

CONTAINER=workmyresume-test-db
HOST_PORT=54329
IMAGE=postgres:15.15-trixie

cleanup() {
    docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

cleanup

echo "==> Starting throwaway PostgreSQL ($IMAGE) on host port $HOST_PORT"
docker run -d --name "$CONTAINER" \
    -e POSTGRES_USER=test \
    -e POSTGRES_PASSWORD=test \
    -e POSTGRES_DB=test \
    -p "$HOST_PORT:5432" \
    "$IMAGE" >/dev/null

for i in $(seq 1 30); do
    if docker exec "$CONTAINER" pg_isready -U test >/dev/null 2>&1; then
        break
    fi
    sleep 1
done
docker exec "$CONTAINER" pg_isready -U test >/dev/null || {
    echo "PostgreSQL did not become ready"; exit 1;
}

echo "==> Running tests against localhost:$HOST_PORT"
TEST_DB_PORT=$HOST_PORT ./mvnw test "$@"

echo "==> Tests finished; removing container"