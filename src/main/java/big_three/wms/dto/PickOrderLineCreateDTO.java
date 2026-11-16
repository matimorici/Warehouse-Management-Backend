package big_three.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PickOrderLineCreateDTO {

    @NotNull(message = "El producto no puede estar vacío")
    private Long idProducto;

    @NotNull(message = "La cantidad no puede estar vacía")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
}
