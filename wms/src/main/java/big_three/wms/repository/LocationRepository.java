package big_three.wms.repository;
import big_three.wms.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository  extends JpaRepository<Location, Long>{
}
