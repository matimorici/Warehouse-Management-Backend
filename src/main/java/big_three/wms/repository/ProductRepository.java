package big_three.wms.repository;

import big_three.wms.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Con la convención de nombres "existsBy" + "NombreCampo", Spring Boot crea la consulta a la base de datos automáticamente
    boolean existsByCodigoBarras(String codigoBarras);

     @Query(value = "SELECT nextval('codigo_interno_seq')", nativeQuery = true)
     Long obtenerSiguienteSecuencia();

    // Solo activos
    List<Product> findByFechaBajaIsNull();
    Optional<Product> findByIdProductoAndFechaBajaIsNull(Long id);
    boolean existsByIdProductoAndFechaBajaIsNull(Long id);
    boolean existsByProveedor_IdProveedorAndFechaBajaIsNull(Long idProveedor);
}
