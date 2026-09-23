package big_three.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "linea_compra")
@IdClass(PurchaseOrderLine.PurchaseOrderLineId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLine {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderLineId implements Serializable {
        private Long idPurchaseOrder;
        private Long idProduct;
    }

    @Id
    @Column(name = "id_orden_compra", nullable = false)
    private Long idPurchaseOrder;

    @Id
    @Column(name = "id_producto", nullable = false)
    private Long idProduct;

    @Column(name = "cantidad", nullable = false)
    private Integer amount;
}