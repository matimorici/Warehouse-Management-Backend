package big_three.wms.repository;

import big_three.wms.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // Solo activas
    List<Location> findByDeletedDateTimeIsNull();
    Optional<Location> findByIdAndDeletedDateTimeIsNull(Long id);
    boolean existsByIdAndDeletedDateTimeIsNull(Long id);

    // ¿Hay algún producto activo cuya ubicación actual sea esta?
    // La ubicación actual es id_ubicacion_hasta del último movimiento_fisico del producto.
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM movimiento_fisico mf
                JOIN producto p ON p.id_producto = mf.id_producto
                WHERE p.fecha_baja IS NULL
                  AND mf.id_ubicacion_hasta = :idUbicacion
                  AND mf.fecha_hora = (
                      SELECT MAX(mf2.fecha_hora)
                      FROM movimiento_fisico mf2
                      WHERE mf2.id_producto = mf.id_producto
                  )
            )
            """, nativeQuery = true)
    boolean existsActiveProductCurrentlyInLocation(@Param("idUbicacion") Long idUbicacion);
}
