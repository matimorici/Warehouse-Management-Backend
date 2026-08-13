package big_three.wms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class User {
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY) //genera el ID AI
	    @Column(name="id_usuario") //nombre de la columna con snake_case en la db
	    private Long idUsuario;// nombre en camel case en el objeto

	    @Column(nullable = false, length = 150)
	    private String nombre;
	   
	    @Column(nullable = false, length = 150)
	    private String apellido;
	   
	    @Column(nullable = false, length = 20)
	    private String cuil;
	   
	    @Column(nullable = false)
	    private String rol = "OPERARIO";

	    @Column(nullable = false, length = 255)
		private String contrasena;
}
