package big_three.wms.repository;

import big_three.wms.model.SupplierRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRatingRepository extends JpaRepository<SupplierRating, Long> {
    List<SupplierRating> findByIdSupplier(Long idSupplier);
}
