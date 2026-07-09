package big_three.wms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "cuit", unique = true, nullable = false, length = 20)
    private String cuit;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "mail", length = 150)
    private String mail;

    @Column(name = "direccion", length = 200)
    private String direccion;
}
