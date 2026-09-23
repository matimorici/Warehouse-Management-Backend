package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_fisico")
@IdClass(PhysicalMovement.PhysicalMovementId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalMovement {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhysicalMovementId implements Serializable {
        private Long idProduct;
        private LocalDateTime dateTime;
    }

    @Id
    @Column(name = "id_producto", nullable = false)
    private Long idProduct;

    @Id
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "id_ubicacion_desde")
    private Long idLocationFrom;

    @Column(name = "id_ubicacion_hasta", nullable = false)
    private Long idLocationTo;

    @Column(name = "id_usuario", nullable = false)
    // Stored as a raw Long (no @ManyToOne): the FK exists only at the DB level, and
    // mapping it in JPA would force lazy-loading when building responses, which
    // conflicts with open-in-view=false. Same convention as PickOrder.idUsuario.
    private Long idUser;
}