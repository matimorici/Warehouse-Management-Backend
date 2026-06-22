package big_three.wms.repository;

import big_three.wms.model.User;
import org.springframework.data.jpa.repository.JpaRepository; // incluye save(), findAll(), findById(), deleteById() básicos, no conoce los campos específicos del objeto para armar el resto de posibles consultas
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Con la convención de nombres "existsBy" + "NombreCampo", Spring Boot crea la consulta a la base de datos automáticamente
    boolean existsByCuil(String cuil);

     Optional<User> findByCuil(String cuil);
     List<User> findByRol(String rol);
}
