package big_three.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseOrderLineCreateDTO {
    @NotNull(message = "El producto no puede estar vacío")
    private Long idProduct;

    @NotNull(message = "La cantidad no puede estar vacía")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer amount;
}
