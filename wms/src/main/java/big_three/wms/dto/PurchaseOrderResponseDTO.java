package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponseDTO {
    private Long idPurchaseOrder;
    private LocalDateTime dateTime;
    private Long idSupplier;
    private String status;
    private List<PurchaseOrderLineResponseDTO> lines;
}
