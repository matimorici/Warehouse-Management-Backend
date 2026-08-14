package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_compra")
    private Long idOrdenCompra;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "id_proveedor", nullable = false)
    // Misma decisión deliberada que PickOrder.idUsuario: se guarda como Long crudo y NO se mapea
    // @ManyToOne a Proveedor. La FK existe a nivel de BD (V1__create_schema.sql); mapearla en JPA
    // obligaría a cargar el proveedor (lazy) al armar las respuestas, inviable con open-in-view=false.
    private Long idProveedor;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoOrdenCompra estado;

    public enum EstadoOrdenCompra {
        PENDIENTE,
        RECIBIDA,
        CANCELADA
    }
}
