package big_three.wms.repository;

import big_three.wms.dto.report.ProductoSinRotacionDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends Repository<Product, Long> {

    /**
     * Qué trae: productos activos con stock disponible menor o igual a {@code umbral}, con
     * proveedor, cantidades y fecha del stock. Un producto sin fila de stock cuenta como 0.
     * Cómo: JPQL sobre {@code Product} con join al proveedor activo, {@code LEFT JOIN} a
     * {@code Stock}, {@code COALESCE} para el stock faltante y orden por disponible y nombre.
     */
    @Query("""
            SELECT new big_three.wms.dto.report.StockBajoDTO(
                p.idProducto,
                p.nombreProducto,
                p.codigoBarras,
                pr.idProveedor,
                pr.razonSocial,
                COALESCE(s.cantidadDisponible, 0),
                COALESCE(s.cantidadPendiente, 0),
                s.fechaHora)
            FROM Product p
            JOIN p.proveedor pr
            LEFT JOIN Stock s ON s.idProducto = p.idProducto
            WHERE p.fechaBaja IS NULL
              AND pr.fechaBaja IS NULL
              AND COALESCE(s.cantidadDisponible, 0) <= :umbral
            ORDER BY COALESCE(s.cantidadDisponible, 0) ASC, p.nombreProducto ASC, p.idProducto ASC
            """)
    List<StockBajoDTO> findStockBajo(@Param("umbral") Integer umbral);

    /**
     * Qué trae: por cada proveedor activo, cantidad de productos activos y suma de unidades
     * disponibles y pendientes. Incluye proveedores sin productos (con ceros).
     * Cómo: JPQL desde {@code Proveedor} con {@code LEFT JOIN} a sus productos activos y a
     * {@code Stock}, agrupando por proveedor y usando {@code COALESCE}/{@code SUM}.
     */
    @Query("""
            SELECT new big_three.wms.dto.report.StockPorProveedorDTO(
                pr.idProveedor,
                pr.razonSocial,
                COUNT(p.idProducto),
                SUM(COALESCE(s.cantidadDisponible, 0)),
                SUM(COALESCE(s.cantidadPendiente, 0)))
            FROM Proveedor pr
            LEFT JOIN Product p ON p.proveedor = pr AND p.fechaBaja IS NULL
            LEFT JOIN Stock s ON s.idProducto = p.idProducto
            WHERE pr.fechaBaja IS NULL
            GROUP BY pr.idProveedor, pr.razonSocial
            ORDER BY SUM(COALESCE(s.cantidadDisponible, 0)) DESC, pr.razonSocial ASC, pr.idProveedor ASC
            """)
    List<StockPorProveedorDTO> findStockPorProveedor();

    /**
     * Qué trae: productos activos con su ubicación actual y la fecha del último movimiento
     * físico; los nunca movidos quedan con ubicación y fecha nulas. Con {@code soloSinUbicar}
     * devuelve solo esos últimos.
     * Cómo: SQL nativo con {@code LEFT JOIN LATERAL} que toma el movimiento más reciente de
     * cada producto ({@code ORDER BY fecha_hora DESC LIMIT 1}) y luego une la ubicación activa.
     */
    @Query(value = """
            SELECT p.id_producto AS "idProducto",
                   p.nombre_producto AS "nombreProducto",
                   p.codigo_barras AS "codigoBarras",
                   u.id_ubicacion AS "idUbicacion",
                   u.nombre_ubicacion AS "nombreUbicacion",
                   m.fecha_hora AS "fechaUltimoMovimiento"
            FROM producto p
            LEFT JOIN LATERAL (
                SELECT mf.id_ubicacion_hasta, mf.fecha_hora
                FROM movimiento_fisico mf
                WHERE mf.id_producto = p.id_producto
                ORDER BY mf.fecha_hora DESC
                LIMIT 1
            ) m ON TRUE
            LEFT JOIN ubicacion u ON u.id_ubicacion = m.id_ubicacion_hasta
                                 AND u.fecha_baja IS NULL
            WHERE p.fecha_baja IS NULL
              AND (:soloSinUbicar = FALSE OR m.id_ubicacion_hasta IS NULL)
            ORDER BY u.nombre_ubicacion ASC NULLS LAST, p.nombre_producto ASC, p.id_producto ASC
            """, nativeQuery = true)
    List<ProductoUbicacionActualProjection> findProductosUbicacionActual(
            @Param("soloSinUbicar") boolean soloSinUbicar);

    /**
     * Qué trae: cada ubicación activa con la cantidad de productos activos distintos cuya
     * última ubicación es esa. Incluye ubicaciones vacías (cantidad 0).
     * Cómo: SQL nativo con una CTE y {@code DISTINCT ON} para quedarse con el último
     * movimiento de cada producto, y luego {@code LEFT JOIN} a ubicación y producto activo
     * con {@code COUNT} y {@code GROUP BY}.
     */
    @Query(value = """
            WITH ultima_ubicacion AS (
                SELECT DISTINCT ON (mf.id_producto) mf.id_producto, mf.id_ubicacion_hasta
                FROM movimiento_fisico mf
                ORDER BY mf.id_producto, mf.fecha_hora DESC
            )
            SELECT u.id_ubicacion AS "idUbicacion",
                   u.nombre_ubicacion AS "nombreUbicacion",
                   COUNT(p.id_producto) AS "cantidadProductos"
            FROM ubicacion u
            LEFT JOIN ultima_ubicacion uu ON uu.id_ubicacion_hasta = u.id_ubicacion
            LEFT JOIN producto p ON p.id_producto = uu.id_producto
                                AND p.fecha_baja IS NULL
            WHERE u.fecha_baja IS NULL
            GROUP BY u.id_ubicacion, u.nombre_ubicacion
            ORDER BY COUNT(p.id_producto) DESC, u.nombre_ubicacion ASC, u.id_ubicacion ASC
            """, nativeQuery = true)
    List<UbicacionOcupacionProjection> findUbicacionesOcupacion();

    /**
     * Qué trae: top de productos por unidades retiradas en el período, con la cantidad de
     * órdenes de retiro activas que los incluyeron. Incluye productos dados de baja
     * (con {@code activo = false}) y aplica {@code limit}.
     * Cómo: SQL nativo desde {@code orden_retiro} uniendo líneas y producto, filtrando
     * órdenes activas dentro del rango, agrupando por producto y sumando cantidades.
     */
    @Query(value = """
            SELECT p.id_producto AS "idProducto",
                   p.nombre_producto AS "nombreProducto",
                   p.codigo_barras AS "codigoBarras",
                   (p.fecha_baja IS NULL) AS "activo",
                   SUM(l.cantidad) AS "unidadesRetiradas",
                   COUNT(DISTINCT o.id_orden_retiro) AS "cantidadOrdenes"
            FROM orden_retiro o
            JOIN linea_retiro l ON l.id_orden_retiro = o.id_orden_retiro
            JOIN producto p ON p.id_producto = l.id_producto
            WHERE o.fecha_baja IS NULL
              AND o.fecha_hora >= :from
              AND o.fecha_hora < :toExclusive
            GROUP BY p.id_producto, p.nombre_producto, p.codigo_barras, p.fecha_baja
            ORDER BY SUM(l.cantidad) DESC, p.nombre_producto ASC, p.id_producto ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<ProductoMasRetiradoProjection> findProductosMasRetirados(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("limit") int limit);

    /**
     * Qué trae: productos activos sin retiros activos en el período, con su stock disponible
     * y la fecha de su último retiro histórico (sin limitar al período). Con
     * {@code soloConStock} exige stock disponible mayor a 0.
     * Cómo: JPQL con {@code NOT EXISTS} sobre órdenes/líneas del período y una subconsulta
     * {@code MAX} para el último retiro, más {@code LEFT JOIN} a {@code Stock}.
     */
    @Query("""
            SELECT new big_three.wms.dto.report.ProductoSinRotacionDTO(
                p.idProducto,
                p.nombreProducto,
                p.codigoBarras,
                COALESCE(s.cantidadDisponible, 0),
                (SELECT MAX(o2.fechaHora)
                 FROM PickOrder o2
                 JOIN PickOrderLine l2 ON l2.idOrdenRetiro = o2.idOrdenRetiro
                 WHERE l2.idProducto = p.idProducto
                   AND o2.fechaBaja IS NULL))
            FROM Product p
            LEFT JOIN Stock s ON s.idProducto = p.idProducto
            WHERE p.fechaBaja IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM PickOrder o
                  JOIN PickOrderLine l ON l.idOrdenRetiro = o.idOrdenRetiro
                  WHERE l.idProducto = p.idProducto
                    AND o.fechaBaja IS NULL
                    AND o.fechaHora >= :from
                    AND o.fechaHora < :toExclusive)
              AND (:soloConStock = FALSE OR COALESCE(s.cantidadDisponible, 0) > 0)
            ORDER BY COALESCE(s.cantidadDisponible, 0) DESC, p.nombreProducto ASC, p.idProducto ASC
            """)
    List<ProductoSinRotacionDTO> findProductosSinRotacion(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("soloConStock") boolean soloConStock);

    /**
     * Qué trae: serie temporal de retiros activos en el período, agrupada por día, semana o
     * mes, con cantidad de órdenes y unidades retiradas.
     * Cómo: SQL nativo que usa {@code date_trunc} con la agrupación validada por el service,
     * filtra órdenes activas en el rango y agrupa/ordena por la primera columna.
     */
    @Query(value = """
            SELECT CAST(date_trunc(CAST(:agrupacion AS text), o.fecha_hora) AS date) AS "periodo",
                   COUNT(DISTINCT o.id_orden_retiro) AS "cantidadOrdenes",
                   SUM(l.cantidad) AS "unidadesRetiradas"
            FROM orden_retiro o
            JOIN linea_retiro l ON l.id_orden_retiro = o.id_orden_retiro
            WHERE o.fecha_baja IS NULL
              AND o.fecha_hora >= :from
              AND o.fecha_hora < :toExclusive
            GROUP BY 1
            ORDER BY 1 ASC
            """, nativeQuery = true)
    List<RetiroPorPeriodoProjection> findRetirosPorPeriodo(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("agrupacion") String agrupacion);

    /**
     * Qué trae: la lista base de todos los usuarios con nombre, apellido, CUIL, rol y si están
     * activos. Es el punto de partida del reporte de actividad, que luego se cruza con retiros
     * y movimientos en el service.
     * Cómo: {@code SELECT} directo sobre {@code usuario}, ordenado por id.
     */
    @Query(value = """
            SELECT u.id_usuario AS "idUsuario",
                   u.nombre AS "nombre",
                   u.apellido AS "apellido",
                   u.cuil AS "cuil",
                   u.rol AS "rol",
                   (u.fecha_baja IS NULL) AS "activo"
            FROM usuario u
            ORDER BY u.id_usuario ASC
            """, nativeQuery = true)
    List<UsuarioActividadBaseProjection> findUsuariosActividad();

    /**
     * Qué trae: por usuario, cantidad de órdenes de retiro activas, unidades retiradas y la
     * fecha de su última actividad de retiro dentro del período.
     * Cómo: SQL nativo desde {@code orden_retiro} con {@code LEFT JOIN} a sus líneas, filtrando
     * órdenes activas en el rango y agrupando por usuario con {@code COUNT DISTINCT},
     * {@code SUM} y {@code MAX}.
     */
    @Query(value = """
            SELECT o.id_usuario AS "idUsuario",
                   COUNT(DISTINCT o.id_orden_retiro) AS "cantidadRetiros",
                   COALESCE(SUM(l.cantidad), 0) AS "unidadesRetiradas",
                   MAX(o.fecha_hora) AS "ultimaActividad"
            FROM orden_retiro o
            LEFT JOIN linea_retiro l ON l.id_orden_retiro = o.id_orden_retiro
            WHERE o.fecha_baja IS NULL
              AND o.fecha_hora >= :from
              AND o.fecha_hora < :toExclusive
            GROUP BY o.id_usuario
            """, nativeQuery = true)
    List<RetirosUsuarioProjection> findRetirosPorUsuario(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Qué trae: por usuario, cantidad de movimientos físicos y la fecha de su último movimiento
     * dentro del período.
     * Cómo: SQL nativo que cuenta y toma el máximo de {@code movimiento_fisico} filtrando por
     * rango y agrupando por usuario.
     */
    @Query(value = """
            SELECT m.id_usuario AS "idUsuario",
                   COUNT(*) AS "cantidadMovimientos",
                   MAX(m.fecha_hora) AS "ultimaActividad"
            FROM movimiento_fisico m
            WHERE m.fecha_hora >= :from
              AND m.fecha_hora < :toExclusive
            GROUP BY m.id_usuario
            """, nativeQuery = true)
    List<MovimientosUsuarioProjection> findMovimientosPorUsuario(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Qué trae: por proveedor, cantidad de órdenes de compra por estado y unidades pedidas y
     * recibidas en el período. Incluye proveedores activos sin órdenes (con ceros) y proveedores
     * dados de baja que sí tuvieron órdenes en el período.
     * Cómo: SQL nativo desde {@code proveedor} con {@code LEFT JOIN} a órdenes activas del rango
     * y a sus líneas, usando {@code COUNT(DISTINCT ...) FILTER} por estado y {@code SUM ... FILTER}
     * para las unidades, agrupando por proveedor.
     */
    @Query(value = """
            SELECT pr.id_proveedor AS "idProveedor",
                   pr.razon_social AS "razonSocial",
                   (pr.fecha_baja IS NULL) AS "activo",
                   COUNT(DISTINCT o.id_orden_compra) FILTER (WHERE o.estado = 'PENDIENTE') AS "ordenesPendientes",
                   COUNT(DISTINCT o.id_orden_compra) FILTER (WHERE o.estado = 'RECIBIDA') AS "ordenesRecibidas",
                   COUNT(DISTINCT o.id_orden_compra) FILTER (WHERE o.estado = 'CANCELADA') AS "ordenesCanceladas",
                   COALESCE(SUM(l.cantidad) FILTER (WHERE o.estado IN ('PENDIENTE', 'RECIBIDA')), 0) AS "unidadesPedidas",
                   COALESCE(SUM(l.cantidad) FILTER (WHERE o.estado = 'RECIBIDA'), 0) AS "unidadesRecibidas"
            FROM proveedor pr
            LEFT JOIN orden_compra o
                   ON o.id_proveedor = pr.id_proveedor
                  AND o.fecha_baja IS NULL
                  AND o.fecha_hora >= :from
                  AND o.fecha_hora < :toExclusive
            LEFT JOIN linea_compra l ON l.id_orden_compra = o.id_orden_compra
            WHERE pr.fecha_baja IS NULL
               OR EXISTS (
                   SELECT 1
                   FROM orden_compra o2
                   WHERE o2.id_proveedor = pr.id_proveedor
                     AND o2.fecha_baja IS NULL
                     AND o2.fecha_hora >= :from
                     AND o2.fecha_hora < :toExclusive
               )
            GROUP BY pr.id_proveedor, pr.razon_social, pr.fecha_baja
            ORDER BY COALESCE(SUM(l.cantidad) FILTER (WHERE o.estado IN ('PENDIENTE', 'RECIBIDA')), 0) DESC,
                     pr.razon_social ASC,
                     pr.id_proveedor ASC
            """, nativeQuery = true)
    List<OrdenCompraResumenProjection> findOrdenesCompraResumen(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Qué trae: los proveedores que deben aparecer en el ranking: todos los activos y los dados
     * de baja solo si tuvieron valoraciones u órdenes recibidas en el período.
     * Cómo: SQL nativo sobre {@code proveedor} con dos {@code EXISTS}: uno en
     * {@code valoracion_proveedor} por fecha y otro en {@code orden_compra} activa y
     * {@code RECIBIDA} con {@code fecha_recepcion} en el rango.
     */
    @Query(value = """
            SELECT pr.id_proveedor AS "idProveedor",
                   pr.razon_social AS "razonSocial",
                   (pr.fecha_baja IS NULL) AS "activo"
            FROM proveedor pr
            WHERE pr.fecha_baja IS NULL
               OR EXISTS (
                   SELECT 1
                   FROM valoracion_proveedor v
                   WHERE v.id_proveedor = pr.id_proveedor
                     AND v.fecha_hora >= :from
                     AND v.fecha_hora < :toExclusive
               )
               OR EXISTS (
                   SELECT 1
                   FROM orden_compra o
                   WHERE o.id_proveedor = pr.id_proveedor
                     AND o.fecha_baja IS NULL
                     AND o.estado = 'RECIBIDA'
                     AND o.fecha_recepcion >= :from
                     AND o.fecha_recepcion < :toExclusive
               )
            ORDER BY pr.id_proveedor ASC
            """, nativeQuery = true)
    List<ProveedorRankingBaseProjection> findProveedoresRankingBase(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Qué trae: por proveedor, cantidad de valoraciones y promedio de tiempo de entrega
     * valorado en el período (el promedio ignora valores nulos).
     * Cómo: SQL nativo sobre {@code valoracion_proveedor} filtrando por {@code fecha_hora},
     * agrupando por proveedor con {@code COUNT} y {@code AVG}.
     */
    @Query(value = """
            SELECT v.id_proveedor AS "idProveedor",
                   COUNT(*) AS "cantidadValoraciones",
                   CAST(AVG(v.tiempo_entrega) AS double precision) AS "promedioTiempoEntregaValorado"
            FROM valoracion_proveedor v
            WHERE v.fecha_hora >= :from
              AND v.fecha_hora < :toExclusive
            GROUP BY v.id_proveedor
            """, nativeQuery = true)
    List<ValoracionesProveedorProjection> findValoracionesPorProveedor(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Qué trae: por proveedor, cantidad de órdenes {@code RECIBIDA} activas con recepción en el
     * período, promedio de días entre creación y recepción, y cantidad de órdenes recibidas sin
     * fecha de recepción (estas últimas sin filtro de período).
     * Cómo: SQL nativo sobre {@code orden_compra} filtrando estado y baja, con
     * {@code COUNT}/{@code AVG} condicionales ({@code FILTER}) y {@code EXTRACT(EPOCH ...)} para
     * calcular los días, agrupando por proveedor.
     */
    @Query(value = """
            SELECT o.id_proveedor AS "idProveedor",
                   COUNT(*) FILTER (
                       WHERE o.fecha_recepcion >= :from
                         AND o.fecha_recepcion < :toExclusive
                   ) AS "ordenesRecibidas",
                   CAST(AVG(EXTRACT(EPOCH FROM (o.fecha_recepcion - o.fecha_hora)) / 86400.0)
                       FILTER (
                           WHERE o.fecha_recepcion >= :from
                             AND o.fecha_recepcion < :toExclusive
                       ) AS double precision) AS "promedioDiasRecepcion",
                   COUNT(*) FILTER (WHERE o.fecha_recepcion IS NULL) AS "recibidasSinFecha"
            FROM orden_compra o
            WHERE o.fecha_baja IS NULL
              AND o.estado = 'RECIBIDA'
              AND (o.fecha_recepcion IS NULL
                   OR (o.fecha_recepcion >= :from AND o.fecha_recepcion < :toExclusive))
            GROUP BY o.id_proveedor
            """, nativeQuery = true)
    List<OrdenesRecibidasProveedorProjection> findOrdenesRecibidasPorProveedor(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Qué trae: las bajas lógicas de usuario, producto, proveedor, ubicación, orden de compra y
     * orden de retiro dentro del período, con entidad, id, descripción, fecha de baja, id del
     * usuario que la hizo y su nombre completo (null si no quedó registrado).
     * Cómo: SQL nativo con una CTE que hace {@code UNION ALL} de las seis tablas filtrando
     * {@code fecha_baja IS NOT NULL} y el rango; luego {@code LEFT JOIN} a {@code usuario} para
     * resolver el autor, filtra por {@code entidad} (o {@code 'TODAS'}) y ordena por fecha
     * descendente con {@code LIMIT}.
     */
    @Query(value = """
            WITH bajas AS (
                SELECT CAST('USUARIO' AS VARCHAR) AS entidad,
                       CAST(u.id_usuario AS BIGINT) AS id_entidad,
                       CAST(CONCAT(u.nombre, ' ', u.apellido, ' (', u.cuil, ')') AS VARCHAR) AS descripcion,
                       CAST(u.fecha_baja AS TIMESTAMP) AS fecha_baja,
                       CAST(u.id_usuario_baja AS BIGINT) AS id_usuario_baja
                FROM usuario u
                WHERE u.fecha_baja IS NOT NULL
                  AND u.fecha_baja >= :from
                  AND u.fecha_baja < :toExclusive
                UNION ALL
                SELECT CAST('PRODUCTO' AS VARCHAR),
                       CAST(p.id_producto AS BIGINT),
                       CAST(CONCAT(p.nombre_producto, ' (', p.codigo_barras, ')') AS VARCHAR),
                       CAST(p.fecha_baja AS TIMESTAMP),
                       CAST(p.id_usuario_baja AS BIGINT)
                FROM producto p
                WHERE p.fecha_baja IS NOT NULL
                  AND p.fecha_baja >= :from
                  AND p.fecha_baja < :toExclusive
                UNION ALL
                SELECT CAST('PROVEEDOR' AS VARCHAR),
                       CAST(pr.id_proveedor AS BIGINT),
                       CAST(CONCAT(pr.razon_social, ' (', pr.cuit, ')') AS VARCHAR),
                       CAST(pr.fecha_baja AS TIMESTAMP),
                       CAST(pr.id_usuario_baja AS BIGINT)
                FROM proveedor pr
                WHERE pr.fecha_baja IS NOT NULL
                  AND pr.fecha_baja >= :from
                  AND pr.fecha_baja < :toExclusive
                UNION ALL
                SELECT CAST('UBICACION' AS VARCHAR),
                       CAST(ub.id_ubicacion AS BIGINT),
                       CAST(ub.nombre_ubicacion AS VARCHAR),
                       CAST(ub.fecha_baja AS TIMESTAMP),
                       CAST(ub.id_usuario_baja AS BIGINT)
                FROM ubicacion ub
                WHERE ub.fecha_baja IS NOT NULL
                  AND ub.fecha_baja >= :from
                  AND ub.fecha_baja < :toExclusive
                UNION ALL
                SELECT CAST('ORDEN_COMPRA' AS VARCHAR),
                       CAST(oc.id_orden_compra AS BIGINT),
                       CAST(CONCAT('Orden de compra #', oc.id_orden_compra, ' — ', pr.razon_social) AS VARCHAR),
                       CAST(oc.fecha_baja AS TIMESTAMP),
                       CAST(oc.id_usuario_baja AS BIGINT)
                FROM orden_compra oc
                JOIN proveedor pr ON pr.id_proveedor = oc.id_proveedor
                WHERE oc.fecha_baja IS NOT NULL
                  AND oc.fecha_baja >= :from
                  AND oc.fecha_baja < :toExclusive
                UNION ALL
                SELECT CAST('ORDEN_RETIRO' AS VARCHAR),
                       CAST(or_ret.id_orden_retiro AS BIGINT),
                       CAST(CONCAT('Orden de retiro #', or_ret.id_orden_retiro) AS VARCHAR),
                       CAST(or_ret.fecha_baja AS TIMESTAMP),
                       CAST(or_ret.id_usuario_baja AS BIGINT)
                FROM orden_retiro or_ret
                WHERE or_ret.fecha_baja IS NOT NULL
                  AND or_ret.fecha_baja >= :from
                  AND or_ret.fecha_baja < :toExclusive
            )
            SELECT b.entidad AS "entidad",
                   b.id_entidad AS "idEntidad",
                   b.descripcion AS "descripcion",
                   b.fecha_baja AS "fechaBaja",
                   b.id_usuario_baja AS "idUsuarioBaja",
                   CASE WHEN u.id_usuario IS NULL THEN NULL
                        ELSE CONCAT(u.nombre, ' ', u.apellido)
                   END AS "usuarioBaja"
            FROM bajas b
            LEFT JOIN usuario u ON u.id_usuario = b.id_usuario_baja
            WHERE CAST(:entidad AS VARCHAR) = 'TODAS'
               OR b.entidad = CAST(:entidad AS VARCHAR)
            ORDER BY b.fecha_baja DESC, b.entidad ASC, b.id_entidad ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<BajaAuditoriaProjection> findBajasAuditoria(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("entidad") String entidad,
            @Param("limit") int limit);

    interface ProductoUbicacionActualProjection {
        Long getIdProducto();

        String getNombreProducto();

        String getCodigoBarras();

        Long getIdUbicacion();

        String getNombreUbicacion();

        LocalDateTime getFechaUltimoMovimiento();
    }

    interface UbicacionOcupacionProjection {
        Long getIdUbicacion();

        String getNombreUbicacion();

        Long getCantidadProductos();
    }

    interface ProductoMasRetiradoProjection {
        Long getIdProducto();

        String getNombreProducto();

        String getCodigoBarras();

        Boolean getActivo();

        Long getUnidadesRetiradas();

        Long getCantidadOrdenes();
    }

    interface RetiroPorPeriodoProjection {
        LocalDate getPeriodo();

        Long getCantidadOrdenes();

        Long getUnidadesRetiradas();
    }

    interface UsuarioActividadBaseProjection {
        Long getIdUsuario();

        String getNombre();

        String getApellido();

        String getCuil();

        String getRol();

        Boolean getActivo();
    }

    interface RetirosUsuarioProjection {
        Long getIdUsuario();

        Long getCantidadRetiros();

        Long getUnidadesRetiradas();

        LocalDateTime getUltimaActividad();
    }

    interface MovimientosUsuarioProjection {
        Long getIdUsuario();

        Long getCantidadMovimientos();

        LocalDateTime getUltimaActividad();
    }

    interface OrdenCompraResumenProjection {
        Long getIdProveedor();

        String getRazonSocial();

        Boolean getActivo();

        Long getOrdenesPendientes();

        Long getOrdenesRecibidas();

        Long getOrdenesCanceladas();

        Long getUnidadesPedidas();

        Long getUnidadesRecibidas();
    }

    interface ProveedorRankingBaseProjection {
        Long getIdProveedor();

        String getRazonSocial();

        Boolean getActivo();
    }

    interface ValoracionesProveedorProjection {
        Long getIdProveedor();

        Long getCantidadValoraciones();

        Double getPromedioTiempoEntregaValorado();
    }

    interface OrdenesRecibidasProveedorProjection {
        Long getIdProveedor();

        Long getOrdenesRecibidas();

        Double getPromedioDiasRecepcion();

        Long getRecibidasSinFecha();
    }

    interface BajaAuditoriaProjection {
        String getEntidad();

        Long getIdEntidad();

        String getDescripcion();

        LocalDateTime getFechaBaja();

        Long getIdUsuarioBaja();

        String getUsuarioBaja();
    }
}
