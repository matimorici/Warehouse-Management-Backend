package big_three.wms.repository;

import big_three.wms.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    boolean existsByCuit(String cuit);
}
