package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_fisico")
@IdClass(MovimientoFisico.MovimientoFisicoId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoFisico {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovimientoFisicoId implements Serializable {
        private Long idProducto;
        private LocalDateTime fechaHora;
    }

    @Id
    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Id
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "id_ubicacion_desde")
    private Long idUbicacionDesde;

    @Column(name = "id_ubicacion_hasta", nullable = false)
    private Long idUbicacionHasta;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;
}
