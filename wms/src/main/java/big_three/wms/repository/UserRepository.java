package big_three.wms.repository;

import big_three.wms.model.User;
import org.springframework.data.jpa.repository.JpaRepository; // incluye save(), findAll(), findById(), deleteById().
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
