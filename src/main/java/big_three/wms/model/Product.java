package big_three.wms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long idProducto;

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String nombreProducto;

    @Column(name = "descripcion_producto", length = 500)
    private String descripcionProducto;

    @Column(name = "codigo_barras", unique = true, nullable = false, length = 50)
    private String codigoBarras;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "origen_codigo_barras", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrigenCodigoBarras origenCodigoBarras;

    @Column(name = "fecha_baja")
    private LocalDateTime fechaBaja;

    @Column(name = "id_usuario_baja")
    private Long idUsuarioBaja;

    public enum OrigenCodigoBarras {
        FABRICANTE,
        INTERNO
    }
}
