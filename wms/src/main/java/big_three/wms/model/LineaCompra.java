package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Entity
@Table(name = "linea_compra")
@IdClass(LineaCompra.LineaCompraId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineaCompra {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineaCompraId implements Serializable {
        private Long idOrdenCompra;
        private Long idProducto;
    }

    @Id
    @Column(name = "id_orden_compra", nullable = false)
    private Long idOrdenCompra;

    @Id
    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(nullable = false)
    private Integer cantidad;
}
