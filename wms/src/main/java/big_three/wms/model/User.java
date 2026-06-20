package big_three.wms.model;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/*
import java.util.List;
@ToString(exclude = "PickUpOrder")        // exclude relation fields
@EqualsAndHashCode(exclude = "PickUpOrder")
*/
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class User {
	   @Id
	   @Column
	   private UUID id_usuario;
	   /*
	   User u = new User();
	   u.setIdUsuario(UUID.randomUUID());
	   */
	   @Column(nullable = false, length = 150)
	   private String nombre;
	   
	   @Column(nullable = false, length = 150)
	   private String apellido;
	   
	   @Column(nullable = false)
	   private String cuil;
	   
	   @Column(nullable = false)
	   private String rol;
	   /*
	   @OneToMany(mappedBy = "operario", fetch = FetchType.LAZY)
	   private List<Move> moves;

	   @OneToMany(mappedBy = "operario", fetch = FetchType.LAZY)
	   private List<PickUpOrder> pickUpOrders;
	    */
		
}
