package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "linea_retiro")
@IdClass(PickOrderLine.PickOrderLineId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickOrderLine {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PickOrderLineId implements Serializable {
        private Long idOrdenRetiro;
        private Long idProducto;
    }

    @Id
    @Column(name = "id_orden_retiro", nullable = false)
    private Long idOrdenRetiro;

    @Id
    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(nullable = false)
    private Integer cantidad;
}
