-- ============================================================================
-- Warehouse Management System (WMS) — Script de creación de base de datos
-- PostgreSQL
-- Integrantes: Rufine Tadeo, Morici Matías, Dominguez Dolores
-- ============================================================================
-- Ejecutar contra una base ya creada, por ejemplo:
--   CREATE DATABASE wms_db;
--   \c wms_db
--   \i wms_schema.sql
-- ============================================================================
-- LIMPIEZA DE TABLAS (Para poder re-ejecutar el script sin errores)
-- ============================================================================
DROP TABLE IF EXISTS linea_retiro CASCADE;
DROP TABLE IF EXISTS orden_retiro CASCADE;
DROP TABLE IF EXISTS linea_compra CASCADE;
DROP TABLE IF EXISTS orden_compra CASCADE;
DROP TABLE IF EXISTS movimiento_fisico CASCADE;
DROP TABLE IF EXISTS stock CASCADE;
DROP TABLE IF EXISTS producto CASCADE;
DROP TABLE IF EXISTS valoracion_proveedor CASCADE;
DROP TABLE IF EXISTS proveedor CASCADE;
DROP TABLE IF EXISTS ubicacion CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;
-- ============================================================================
-- CAMBIOS respecto de la versión anterior:
--   * Todas las PK pasan de UUID a BIGINT GENERATED ALWAYS AS IDENTITY
--     (autoincremental nativo de PostgreSQL, reemplaza a SERIAL).
--     Motivo: mejor performance de índices (enteros de 8 bytes secuenciales
--     vs UUID random de 16 bytes que fragmenta el B-Tree), y como el sistema
--     va a correr contra una única base de datos, no hace falta que los IDs
--     sean únicos globalmente entre múltiples bases.
--   * Se agrega codigo_barras a producto, junto con origen_codigo_barras
--     para distinguir si el código lo provee el fabricante o si es
--     generado internamente por el sistema.
--   * usuario.nombre / usuario.apellido: ya estaban separados, sin cambios.
-- ============================================================================

-- ============================================================================
-- USUARIO  (tabla única — Operario y Administrador se distinguen por "rol")
-- ============================================================================
CREATE TABLE usuario (
    id_usuario      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    apellido        VARCHAR(150) NOT NULL,
    cuil            VARCHAR(20)  NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    contrasena      VARCHAR(255) NOT NULL, -- Integrado desde Script-3
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('OPERARIO', 'ADMINISTRADOR'))
);

COMMENT ON TABLE usuario IS 'Tabla única para Usuario; el rol distingue Operario de Administrador';

-- ============================================================================
-- UBICACION
-- ============================================================================
CREATE TABLE ubicacion (
    id_ubicacion     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_ubicacion VARCHAR(100) NOT NULL
);

-- ============================================================================
-- PROVEEDOR
-- ============================================================================
CREATE TABLE proveedor (
    id_proveedor  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cuit          VARCHAR(20)  NOT NULL UNIQUE,
    razon_social  VARCHAR(150) NOT NULL,
    telefono      VARCHAR(30),
    mail          VARCHAR(150),
    direccion     VARCHAR(200)
);

-- ============================================================================
-- VALORACION_PROVEEDOR  (entidad débil, depende de Proveedor)
-- ============================================================================
CREATE TABLE valoracion_proveedor (
    id_valoracion           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_proveedor            BIGINT NOT NULL REFERENCES proveedor(id_proveedor) ON DELETE CASCADE,
    fecha_hora              TIMESTAMP NOT NULL,
    tiempo_entrega          INTEGER,                 -- ej: días
    forma_entrega           VARCHAR(100),
    relacion_precio_calidad VARCHAR(100)
);

CREATE INDEX idx_valoracion_proveedor ON valoracion_proveedor(id_proveedor);

-- ============================================================================
-- PRODUCTO  (depende de Proveedor — relación "vende", 1:M)
-- ============================================================================
CREATE TABLE producto (
    id_producto          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre_producto      VARCHAR(150) NOT NULL,
    descripcion_producto VARCHAR(500),
    codigo_barras        VARCHAR(50) NOT NULL UNIQUE,
    origen_codigo_barras VARCHAR(20) NOT NULL,
    id_proveedor         BIGINT NOT NULL REFERENCES proveedor(id_proveedor),
    CONSTRAINT chk_producto_origen_codigo CHECK (origen_codigo_barras IN ('FABRICANTE', 'INTERNO'))
);

CREATE INDEX idx_producto_proveedor ON producto(id_proveedor);
CREATE INDEX idx_producto_codigo_barras ON producto(codigo_barras);

-- ============================================================================
-- STOCK  (1:1 con Producto — snapshot actual, no historial)
-- ============================================================================
CREATE TABLE stock (
    id_producto          BIGINT PRIMARY KEY REFERENCES producto(id_producto) ON DELETE CASCADE,
    fecha_hora           TIMESTAMP NOT NULL,
    cantidad_disponible  INTEGER NOT NULL DEFAULT 0 CHECK (cantidad_disponible >= 0),
    cantidad_pendiente   INTEGER NOT NULL DEFAULT 0 CHECK (cantidad_pendiente >= 0)
);

-- ============================================================================
-- ORDEN_COMPRA  (depende de Proveedor — relación "compra_proveedor")
-- ============================================================================
CREATE TABLE orden_compra (
    id_orden_compra BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_hora      TIMESTAMP NOT NULL,
    id_proveedor    BIGINT NOT NULL REFERENCES proveedor(id_proveedor),
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT chk_orden_compra_estado CHECK (estado IN ('PENDIENTE', 'RECIBIDA', 'CANCELADA'))
);

CREATE INDEX idx_orden_compra_proveedor ON orden_compra(id_proveedor);

-- ============================================================================
-- LINEA_COMPRA  (entidad asociativa: OrdenCompra <-> Producto)
-- PK compuesta — un producto aparece una sola vez por orden
-- ============================================================================
CREATE TABLE linea_compra (
    id_orden_compra BIGINT NOT NULL REFERENCES orden_compra(id_orden_compra) ON DELETE CASCADE,
    id_producto     BIGINT NOT NULL REFERENCES producto(id_producto),
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id_orden_compra, id_producto)
);

-- ============================================================================
-- ORDEN_RETIRO  (relación "realiza" con Operario, vía Usuario)
-- ============================================================================
CREATE TABLE orden_retiro (
    id_orden_retiro BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_hora      TIMESTAMP NOT NULL,
    id_usuario      BIGINT NOT NULL REFERENCES usuario(id_usuario)
);

CREATE INDEX idx_orden_retiro_usuario ON orden_retiro(id_usuario);

-- ============================================================================
-- LINEA_RETIRO  (entidad asociativa: OrdenRetiro <-> Producto)
-- PK compuesta — mismo criterio que LineaCompra
-- ============================================================================
CREATE TABLE linea_retiro (
    id_orden_retiro BIGINT NOT NULL REFERENCES orden_retiro(id_orden_retiro) ON DELETE CASCADE,
    id_producto     BIGINT NOT NULL REFERENCES producto(id_producto),
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id_orden_retiro, id_producto)
);

-- ============================================================================
-- MOVIMIENTO_FISICO  (historial de eventos — PK compuesta producto+fechaHora)
-- relaciona Producto, Ubicacion (desde/hasta) y Usuario (realiza)
-- ============================================================================
CREATE TABLE movimiento_fisico (
    id_producto         BIGINT    NOT NULL REFERENCES producto(id_producto),
    fecha_hora          TIMESTAMP NOT NULL,
    id_ubicacion_desde  BIGINT REFERENCES ubicacion(id_ubicacion),
    id_ubicacion_hasta  BIGINT NOT NULL REFERENCES ubicacion(id_ubicacion),
    id_usuario          BIGINT NOT NULL REFERENCES usuario(id_usuario),
    PRIMARY KEY (id_producto, fecha_hora)
);

CREATE INDEX idx_movimiento_fecha    ON movimiento_fisico(fecha_hora);
CREATE INDEX idx_movimiento_usuario  ON movimiento_fisico(id_usuario);

-- ============================================================================
-- Fin del script
-- ============================================================================
