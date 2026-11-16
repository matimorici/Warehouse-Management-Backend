package big_three.wms.repository;

import big_three.wms.model.PhysicalMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PhysicalMovementRepository extends JpaRepository<PhysicalMovement, PhysicalMovement.PhysicalMovementId> {
    List<PhysicalMovement> findByIdProductOrderByDateTimeDesc(Long idProduct);

    List<PhysicalMovement> findByIdUserOrderByDateTimeDesc(Long idUser);

    List<PhysicalMovement> findByDateTimeBetweenOrderByIdProductAscDateTimeAsc(LocalDateTime from, LocalDateTime to);
}