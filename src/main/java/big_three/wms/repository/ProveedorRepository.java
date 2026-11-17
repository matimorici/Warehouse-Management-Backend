package big_three.wms.repository;

import big_three.wms.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    boolean existsByCuit(String cuit);

    // Solo activos
    List<Proveedor> findByFechaBajaIsNull();
    Optional<Proveedor> findByIdProveedorAndFechaBajaIsNull(Long id);
    boolean existsByIdProveedorAndFechaBajaIsNull(Long id);
}
