package big_three.wms.repository;

import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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
}
