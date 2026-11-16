-- ============================================================================
-- WMS — Migración V5: baja lógica y trazabilidad
-- Flyway — PostgreSQL
-- ============================================================================
-- Agrega las columnas de baja lógica a las seis entidades que la necesitan:
--   * fecha_baja       TIMESTAMP NULL — NULL = activo; con valor = dado de baja.
--   * id_usuario_baja  BIGINT NULL    — quién realizó la baja (FK a usuario).
--   * chk_<tabla>_baja — no puede haber "quién" sin "cuándo". El caso inverso
--                        (fecha sin usuario) queda permitido para correcciones
--                        manuales en la base.
--
-- NOTA: usuario.fecha_baja ya existe desde V4; acá solo se agrega su
-- id_usuario_baja y el CHECK correspondiente. No se repite la columna.
--
-- Las migraciones aplicadas son inmutables, por eso esta única migración deja
-- creadas también las columnas de usuario, producto, proveedor y ubicación,
-- aunque su baja lógica se implemente en un paso posterior.
--
-- Además se agrega orden_compra.fecha_recepcion (fecha/hora en que la orden
-- pasó a RECIBIDA). No se hace backfill: las órdenes recibidas históricas
-- quedan con NULL porque no existe un dato confiable de cuándo se recibieron;
-- copiar fecha_hora (fecha de creación de la orden) sería inventar información.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- USUARIO (fecha_baja ya agregada en V4)
-- ----------------------------------------------------------------------------
ALTER TABLE usuario
    ADD COLUMN id_usuario_baja BIGINT REFERENCES usuario(id_usuario);

ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_baja
    CHECK (id_usuario_baja IS NULL OR fecha_baja IS NOT NULL);

-- ----------------------------------------------------------------------------
-- PRODUCTO
-- ----------------------------------------------------------------------------
ALTER TABLE producto
    ADD COLUMN fecha_baja TIMESTAMP;

ALTER TABLE producto
    ADD COLUMN id_usuario_baja BIGINT REFERENCES usuario(id_usuario);

ALTER TABLE producto
    ADD CONSTRAINT chk_producto_baja
    CHECK (id_usuario_baja IS NULL OR fecha_baja IS NOT NULL);

-- ----------------------------------------------------------------------------
-- PROVEEDOR
-- ----------------------------------------------------------------------------
ALTER TABLE proveedor
    ADD COLUMN fecha_baja TIMESTAMP;

ALTER TABLE proveedor
    ADD COLUMN id_usuario_baja BIGINT REFERENCES usuario(id_usuario);

ALTER TABLE proveedor
    ADD CONSTRAINT chk_proveedor_baja
    CHECK (id_usuario_baja IS NULL OR fecha_baja IS NOT NULL);

-- ----------------------------------------------------------------------------
-- UBICACION
-- ----------------------------------------------------------------------------
ALTER TABLE ubicacion
    ADD COLUMN fecha_baja TIMESTAMP;

ALTER TABLE ubicacion
    ADD COLUMN id_usuario_baja BIGINT REFERENCES usuario(id_usuario);

ALTER TABLE ubicacion
    ADD CONSTRAINT chk_ubicacion_baja
    CHECK (id_usuario_baja IS NULL OR fecha_baja IS NOT NULL);

-- ----------------------------------------------------------------------------
-- ORDEN_COMPRA
-- ----------------------------------------------------------------------------
ALTER TABLE orden_compra
    ADD COLUMN fecha_baja TIMESTAMP;

ALTER TABLE orden_compra
    ADD COLUMN id_usuario_baja BIGINT REFERENCES usuario(id_usuario);

ALTER TABLE orden_compra
    ADD CONSTRAINT chk_orden_compra_baja
    CHECK (id_usuario_baja IS NULL OR fecha_baja IS NOT NULL);

-- Solo si V4 no la agregó (no lo hizo): fecha/hora de recepción de la orden.
ALTER TABLE orden_compra
    ADD COLUMN fecha_recepcion TIMESTAMP;

-- ----------------------------------------------------------------------------
-- ORDEN_RETIRO
-- ----------------------------------------------------------------------------
ALTER TABLE orden_retiro
    ADD COLUMN fecha_baja TIMESTAMP;

ALTER TABLE orden_retiro
    ADD COLUMN id_usuario_baja BIGINT REFERENCES usuario(id_usuario);

ALTER TABLE orden_retiro
    ADD CONSTRAINT chk_orden_retiro_baja
    CHECK (id_usuario_baja IS NULL OR fecha_baja IS NOT NULL);

-- ============================================================================
-- TABLAS QUE NO LLEVAN BAJA LÓGICA
-- ============================================================================
-- * valoracion_proveedor: nadie la referencia y su ciclo de vida depende del
--   proveedor (FK ON DELETE CASCADE). No hace falta conservarla por trazabilidad.
-- * linea_compra: sigue a su orden de compra. Si la orden se da de baja, sus
--   líneas se conservan con ella, pero no se dan de baja por separado.
-- * linea_retiro: mismo criterio que linea_compra, sigue a su orden de retiro.
-- * movimiento_fisico: es un historial append-only; los eventos nunca se
--   modifican ni se borran, así que no necesita una fecha de baja propia.
-- * stock: es un snapshot 1:1 del producto; su vigencia sigue a la del producto
--   y no se da de baja de forma independiente.
-- ============================================================================

-- ============================================================================
-- Fin de la migración V5
-- ============================================================================
