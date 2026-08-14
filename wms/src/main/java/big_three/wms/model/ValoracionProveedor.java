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
public class ValoracionProveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valoracion")
    private Long idValoracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "tiempo_entrega")
    private Integer tiempoEntrega;

    @Column(name = "forma_entrega", length = 100)
    private String formaEntrega;

    @Column(name = "relacion_precio_calidad", length = 100)
    private String relacionPrecioCalidad;
}
