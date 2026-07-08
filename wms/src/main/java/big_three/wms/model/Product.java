package big_three.wms.model;

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
@Table(name = "producto")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //genera el ID AI
    @Column(name="id_producto")
    private Long idProducto;

    @Column (name="nombre_producto", nullable = false, length = 150)
    private String nombreProducto;

    @Column (name="descripcion_producto", nullable = false, length = 500)
    private String descripcionProducto;

    @Column(name = "codigo_barras", unique = true, nullable = false, length = 50)
    private String codigoBarras;
    
    @Column(name="id_proveedor")
    private Long idProveedor;

    @Column(name = "origen_codigo", nullable = false)
    @Enumerated(EnumType.STRING) // hace que se guarde el texto literal del valor seleccionado en la base de datos en lugar de valores como 1 y 0
    private OrigenCodigoBarras origenCodigoBarras; // FABRICANTE o INTERNO


    public enum OrigenCodigoBarras {
        FABRICANTE, // EAN-13/UPC-A que ya trae el producto
        INTERNO     // Code128 generado por el sistema
    }
}
