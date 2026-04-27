#!/usr/bin/env bash

set -euo pipefail

COMMAND="${1:-run}"
DATASET="${2:-integrated}"
SCALE="${3:-smoke}"
SEED="${4:-42}"

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

DATASET_DIR="scripts/seed/datasets/${DATASET}"
OUTPUT_ROOT_DIR="scripts/seed/output"

if [ ! -d "${DATASET_DIR}" ]; then
  echo "[seed] dataset not found: ${DATASET}"
  echo "[seed] expected directory: ${DATASET_DIR}"
  exit 1
fi

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "[seed] DB_PASSWORD is required."
  echo "[seed] Set DB_PASSWORD in root .env or run:"
  echo "  DB_PASSWORD='your-password' scripts/seed/run_seed.sh ${COMMAND} ${DATASET} ${SCALE} ${SEED}"
  exit 1
fi

run_mysql_file() {
  local sql_file="$1"

  mysql \
    -h "${DB_HOST}" \
    -P "${DB_PORT}" \
    -u "${DB_USER}" \
    -p"${DB_PASSWORD}" \
    "${DB_NAME}" < "${sql_file}"
}

clean_seed() {
  local clean_file="${DATASET_DIR}/clean.sql"

  if [ ! -f "${clean_file}" ]; then
    echo "[seed] clean file not found: ${clean_file}"
    exit 1
  fi

  echo "[seed] clean dataset=${DATASET}"
  run_mysql_file "${clean_file}"

  echo "[seed] clean output directory: ${OUTPUT_ROOT_DIR}"
  rm -rf "${OUTPUT_ROOT_DIR}"

  echo "[seed] clean completed."
}

run_seed() {
  if ! python3 -c "import pymysql" >/dev/null 2>&1; then
    echo "[seed] PyMySQL is not installed."
    echo "[seed] Install dependencies first:"
    echo "  python3 -m pip install -r scripts/seed/requirements.txt"
    exit 1
  fi

  echo "[seed] dataset=${DATASET}"
  echo "[seed] scale=${SCALE}"
  echo "[seed] seed=${SEED}"
  echo "[seed] db=${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"

  echo "[seed] clean output directory: ${OUTPUT_ROOT_DIR}"
  rm -rf "${OUTPUT_ROOT_DIR}"

  python3 "${DATASET_DIR}/generate.py" \
    --scale "${SCALE}" \
    --seed "${SEED}" \
    --output-dir "${OUTPUT_ROOT_DIR}/${DATASET}"

  DB_NAME="${DB_NAME}" \
  DB_USER="${DB_USER}" \
  DB_PASSWORD="${DB_PASSWORD}" \
  DB_HOST="${DB_HOST}" \
  DB_PORT="${DB_PORT}" \
  python3 "${DATASET_DIR}/insert.py" \
    --scale "${SCALE}" \
    --input-dir "${OUTPUT_ROOT_DIR}/${DATASET}"

  run_mysql_file "${DATASET_DIR}/validate.sql"

  echo "[seed] completed."
}

case "${COMMAND}" in
  run)
    run_seed
    ;;
  clean)
    clean_seed
    ;;
  reset)
    clean_seed
    run_seed
    ;;
  *)
    echo "[seed] unknown command: ${COMMAND}"
    echo "[seed] usage:"
    echo "  scripts/seed/run_seed.sh"
    echo "  scripts/seed/run_seed.sh run integrated smoke 42"
    echo "  scripts/seed/run_seed.sh clean integrated"
    echo "  scripts/seed/run_seed.sh reset integrated smoke 42"
    exit 1
    ;;
esac