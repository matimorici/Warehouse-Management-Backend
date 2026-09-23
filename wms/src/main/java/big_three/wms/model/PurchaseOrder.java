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
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden_compra")
    private Long idPurchaseOrder;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "id_proveedor", nullable = false)
    private Long idSupplier;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDIENTE,
        RECIBIDA,
        CANCELADA
    }
}