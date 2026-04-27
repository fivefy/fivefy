#!/usr/bin/env bash

set -euo pipefail

SCALE="${1:-smoke}"
SEED="${2:-42}"

ENV_FILE=".env"

if [ -f "${ENV_FILE}" ]; then
  set -a
  source "${ENV_FILE}"
  set +a
fi

DB_NAME="${DB_NAME:-fivefy_db}"
DB_USER="${DB_USER:-root}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "[seed] DB_PASSWORD is required."
  echo "[seed] Set it in scripts/seed/.env or run:"
  echo "  DB_PASSWORD='your-password' scripts/seed/run_music_content_seed.sh smoke 42"
  exit 1
fi

echo "[seed] scale=${SCALE}"
echo "[seed] seed=${SEED}"
echo "[seed] db=${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"

# PyMySQL 설치 여부 확인
if ! python3 -c "import pymysql" >/dev/null 2>&1; then
  echo "[seed] PyMySQL is not installed."
  echo "[seed] Install dependencies first:"
  echo "  python3 -m pip install -r scripts/seed/requirements.txt"
  exit 1
fi

# 기존 CSV output 전체 삭제
OUTPUT_ROOT_DIR="scripts/seed/output"

echo "[seed] clean output directory: ${OUTPUT_ROOT_DIR}"
rm -rf "${OUTPUT_ROOT_DIR}"

# CSV 생성
python3 scripts/seed/generate_music_content_seed.py \
  --scale "${SCALE}" \
  --seed "${SEED}"

DB_NAME="${DB_NAME}" \
DB_USER="${DB_USER}" \
DB_PASSWORD="${DB_PASSWORD}" \
DB_HOST="${DB_HOST}" \
DB_PORT="${DB_PORT}" \
python3 scripts/seed/insert_music_content_seed.py \
  --scale "${SCALE}"

mysql \
  -h "${DB_HOST}" \
  -P "${DB_PORT}" \
  -u "${DB_USER}" \
  -p"${DB_PASSWORD}" \
  "${DB_NAME}" < scripts/seed/validate_music_content_seed.sql

echo "[seed] completed."