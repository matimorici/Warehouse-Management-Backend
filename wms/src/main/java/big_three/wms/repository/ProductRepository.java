package big_three.wms.repository;

import big_three.wms.model.Product;
import org.springframework.data.jpa.repository.JpaRepository; // incluye save(), findAll(), findById(), deleteById() básicos, no conoce los campos específicos del objeto para armar el resto de posibles consultas
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Con la convención de nombres "existsBy" + "NombreCampo", Spring Boot crea la consulta a la base de datos automáticamente
    boolean existsByCodigoBarras(String codigoBarras);

     Optional<Product> findBycodigoBarras(String codigoBarras);
//     List<User> findByRol(String rol);
}
