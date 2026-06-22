package big_three.wms.model;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/*
import java.util.List;
@ToString(exclude = "PickUpOrder") // exclude relation fields
@EqualsAndHashCode(exclude = "PickUpOrder")
*/
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class User {
	    @Id
	    @GeneratedValue(strategy = GenerationType.UUID) //genera el UUID
	    @Column
	    private UUID id_usuario;

	    @Column(nullable = false, length = 150)
	    private String nombre;
	   
	    @Column(nullable = false, length = 150)
	    private String apellido;
	   
	    @Column(nullable = false)
	    private String cuil;
	   
	    @Column(nullable = false)
	    private String rol = "Operario";

	    @Column(nullable = false, length = 255)
		private String contrasena;
	   /*
	   @OneToMany(mappedBy = "operario", fetch = FetchType.LAZY)
	   private List<Move> moves;

	   @OneToMany(mappedBy = "operario", fetch = FetchType.LAZY)
	   private List<PickUpOrder> pickUpOrders;
	    */
		
}
