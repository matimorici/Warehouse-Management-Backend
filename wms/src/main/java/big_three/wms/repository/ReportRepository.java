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
}
