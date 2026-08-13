#!/usr/bin/env bash
# ============================================================================
# Verificación manual de la migración V2 (reparación de codigo_interno_seq)
# ============================================================================
# CONTEXTO (por qué existe este script):
#   Con spring.flyway.baseline-on-migrate=true, una base que ya tiene tablas
#   pero NO tiene flyway_schema_history se marca como "V1 ya aplicada" y Flyway
#   SALTE a V1__create_schema.sql. V2__ensure_codigo_interno_seq.sql existe
#   para reparar el único desfase conocido de esas bases viejas: la secuencia
#   codigo_interno_seq que no tenían. Pero V2 es un no-op en bases NUEVAS, así
#   que su única rama con lógica real queda sin probar si nadie la corre contra
#   una base vieja de verdad. Este script arma ESA base vieja y la prueba.
#
# QUÉ HACE:
#   1. Crea un scratch DB (wms_db_v2_test) — NO toca wms_db ni ningún otro.
#   2. Le aplica el schema "viejo": V1__create_schema.sql SIN la secuencia
#      (equivalente a una base creada antes de que se agregara al script).
#   3. Verifica el estado previo: hay tablas pero NO codigo_interno_seq.
#   4. Arranca la app contra el scratch DB → Flyway hace baseline V1 y corre
#      V2 + V3; Hibernate valida el schema.
#   5. Verifica el estado posterior: la secuencia existe (V2) y el admin de
#      seed está (V3). Si todo pasó, imprime "PASÓ".
#
# USO:   bash scripts/verify-v2-repair.sh
# REQUISITOS: psql en el PATH y PostgreSQL corriendo (defaults iguales a
#   application.properties: localhost:5432, postgres / 12345).
#   Se pueden sobreescribir con PGHOST, PGPORT, PGUSER, PGPASSWORD.
# ============================================================================
set -euo pipefail

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-postgres}"
PGPASSWORD="${PGPASSWORD:-12345}"
SCRATCH_DB="wms_db_v2_test"
export PGPASSWORD

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
V1="$ROOT/src/main/resources/db/migration/V1__create_schema.sql"
OLD_STATE="$ROOT/target/verify-v2-old-state.sql"
BOOT_LOG="$ROOT/target/verify-v2-boot.log"
APP_PID=""

psql_flags=(-h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -v ON_ERROR_STOP=1 --quiet)

say()  { printf '\n\033[1;34m==>\033[0m %s\n' "$*"; }
fail() { printf '\n\033[1;31mFALLO:\033[0m %s\n' "$*" >&2; exit 1; }

seq_count() { psql "${psql_flags[@]}" -d "$1" -tAc "SELECT count(*) FROM pg_sequences WHERE sequencename = 'codigo_interno_seq';"; }

cleanup() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    kill -TERM "$APP_PID" 2>/dev/null || true
    sleep 3
  fi
  psql "${psql_flags[@]}" -d postgres -c "DROP DATABASE IF EXISTS $SCRATCH_DB;" >/dev/null 2>&1 || \
    printf 'aviso: no se pudo borrar %s (¿conexiones abiertas?)\n' "$SCRATCH_DB" >&2
  rm -f "$OLD_STATE"
}
trap cleanup EXIT

command -v psql >/dev/null || fail "psql no está en el PATH."
[ -f "$V1" ] || fail "No encuentro la migración V1: $V1"

say "1/5 Creando scratch DB $SCRATCH_DB (drop + create)..."
psql "${psql_flags[@]}" -d postgres -c "DROP DATABASE IF EXISTS $SCRATCH_DB;" >/dev/null
psql "${psql_flags[@]}" -d postgres -c "CREATE DATABASE $SCRATCH_DB;" >/dev/null

say "2/5 Armando schema 'viejo' = V1 SIN 'CREATE SEQUENCE codigo_interno_seq' y aplicándolo..."
mkdir -p "$ROOT/target"
grep -v '^CREATE SEQUENCE codigo_interno_seq;' "$V1" > "$OLD_STATE"
psql "${psql_flags[@]}" -d "$SCRATCH_DB" -f "$OLD_STATE" >/dev/null

TABLES=$(psql "${psql_flags[@]}" -d "$SCRATCH_DB" -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('usuario','producto','proveedor','stock');")
PRE=$(seq_count "$SCRATCH_DB")
[ "$TABLES" = "4" ] || fail "El scratch DB no quedó armado: tablas=$TABLES (esperado 4)."
[ "$PRE" = "0" ] || fail "El scratch DB no es 'viejo': la secuencia YA existe (count=$PRE)."

say "3/5 Estado previo OK: $SCRATCH_DB tiene tablas ($TABLES) pero NO codigo_interno_seq. Arrancando la app (baseline V1 + V2 + V3)..."
[ -d "$ROOT/target/classes" ] || "$ROOT/mvnw" -q -DskipTests compile
(
  exec "$ROOT/mvnw" spring-boot:run \
    -Dspring-boot.run.fork=false \
    -Dspring-boot.run.jvmArguments="-Dspring.datasource.url=jdbc:postgresql://$PGHOST:$PGPORT/$SCRATCH_DB" \
    > "$BOOT_LOG" 2>&1
) &
APP_PID=$!

STARTED=0
for _ in $(seq 1 120); do
  if ! kill -0 "$APP_PID" 2>/dev/null; then break; fi
  if grep -q "Started WmsApplication in" "$BOOT_LOG"; then STARTED=1; break; fi
  sleep 2
done
if [ "$STARTED" = "0" ]; then
  echo "----- último boot.log -----"
  tail -40 "$BOOT_LOG" >&2
  fail "La app no arrancó contra el scratch DB. Log completo: $BOOT_LOG"
fi

say "4/5 App arrancada y schema validado. Verificando V2 y V3..."
POST=$(seq_count "$SCRATCH_DB")
[ "$POST" = "1" ] || fail "V2 no creó la secuencia: count=$POST (esperado 1)."
SEED=$(psql "${psql_flags[@]}" -d "$SCRATCH_DB" -tAc "SELECT count(*) FROM usuario WHERE cuil = '20-00000000-1';")
[ "$SEED" = "1" ] || fail "V3 no insertó el admin de seed: count=$SEED (esperado 1)."

say "5/5 PASÓ ✓"
say "  - codigo_interno_seq presente en $SCRATCH_DB (V2 reparó el desfase del baseline)"
say "  - admin seed '20-00000000-1' presente (V3)"
say "  - la app arrancó y Hibernate validó el schema de una base 'vieja' baselineada"
say "Limpieza automática: la app se detiene y $SCRATCH_DB se borra al salir."
