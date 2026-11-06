-- ============================================================================
-- WMS — Migración V4: baja lógica de usuarios (trazabilidad)
-- ============================================================================
-- NULL = usuario activo. Con valor = fecha/hora en que se dio de baja.
-- Se reemplaza el borrado físico para no perder la trazabilidad de
-- orden_retiro y movimiento_fisico, que referencian a usuario.
-- ============================================================================

ALTER TABLE usuario
    ADD COLUMN fecha_baja TIMESTAMP NULL;

-- Acelera "usuarios activos" (el filtro más frecuente)
CREATE INDEX idx_usuario_activos ON usuario(id_usuario) WHERE fecha_baja IS NULL;