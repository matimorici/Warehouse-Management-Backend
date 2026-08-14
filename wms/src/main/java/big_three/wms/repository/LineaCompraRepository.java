package big_three.wms.repository;

import big_three.wms.model.LineaCompra;
import big_three.wms.model.LineaCompra.LineaCompraId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LineaCompraRepository extends JpaRepository<LineaCompra, LineaCompraId> {

    List<LineaCompra> findByIdOrdenCompra(Long idOrdenCompra);
}
