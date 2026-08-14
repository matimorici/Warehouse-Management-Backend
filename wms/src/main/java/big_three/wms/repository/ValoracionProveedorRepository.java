package big_three.wms.repository;

import big_three.wms.model.ValoracionProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ValoracionProveedorRepository extends JpaRepository<ValoracionProveedor, Long> {
    List<ValoracionProveedor> findByIdProveedor(Long idProveedor);
}
