package big_three.wms.repository;

import big_three.wms.model.Role;
import big_three.wms.model.User;
import org.springframework.data.jpa.repository.JpaRepository; // incluye save(), findAll(), findById(), deleteById() básicos, no conoce los campos específicos del objeto para armar el resto de posibles consultas

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Con la convención de nombres "existsBy" + "NombreCampo", Spring Boot crea la consulta a la base de datos automáticamente
    boolean existsByCuil(String cuil);

     Optional<User> findByCuil(String cuil);

    // Solo activos
    List<User> findByFechaBajaIsNull();
    Optional<User> findByIdUsuarioAndFechaBajaIsNull(Long id);
    Optional<User> findByCuilAndFechaBajaIsNull(String cuil);
    boolean existsByIdUsuarioAndFechaBajaIsNull(Long id);
    long countByRolAndFechaBajaIsNull(Role rol);
}
