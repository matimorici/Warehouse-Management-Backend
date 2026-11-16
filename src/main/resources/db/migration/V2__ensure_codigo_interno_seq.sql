-- ============================================================================
-- WMS — Migración V2: asegurar la secuencia codigo_interno_seq
-- Flyway — PostgreSQL
-- ============================================================================
-- Migración REPARACIÓN (no agrega features): cierra la brecha del baseline.
--
-- Contexto: con spring.flyway.baseline-on-migrate=true, las bases que ya
-- existían (creadas con el viejo wms_schema.sql) se marcan como "V1 ya
-- aplicada" y Flyway saltea V1__create_schema.sql. Cualquier base armada
-- ANTES de que la secuencia se agregara al script no tiene codigo_interno_seq,
-- y recién la pediría en runtime al crear un producto sin código de barras
-- (nextval('codigo_interno_seq')) → error.
--
-- Esta migración garantiza que la secuencia exista en TODAS las bases:
--   * Base nueva: V1 ya la creó → IF NOT EXISTS es no-op.
--   * Base vieja baselineada sin la secuencia → la crea acá.
--     (Equivale al fix manual que antes había que correr a mano.)
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS codigo_interno_seq;

-- ============================================================================
-- Fin de la migración V2
-- ============================================================================
