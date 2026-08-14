package big_three.wms.repository;

import big_three.wms.model.MovimientoFisico;
import big_three.wms.model.MovimientoFisico.MovimientoFisicoId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MovimientoFisicoRepository extends JpaRepository<MovimientoFisico, MovimientoFisicoId> {

    List<MovimientoFisico> findByIdProducto(Long idProducto);
}
