package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "valoracion_proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valoracion")
    private Long id;

    @Column(name = "id_proveedor", nullable = false)
    private Long idSupplier;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "tiempo_entrega")
    private Integer deliveryTime;

    @Column(name = "forma_entrega")
    private String deliveryMethod;

    @Column(name = "relacion_precio_calidad")
    private String priceQualityRatio;
}