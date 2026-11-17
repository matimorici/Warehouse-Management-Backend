package big_three.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRatingCreateDTO {
    @NotNull(message = "El proveedor no puede estar vacío")
    private Long idSupplier;

    @Min(value = 0, message = "El tiempo de entrega no puede ser negativo")
    private Integer deliveryTime;

    @Size(max = 100, message = "La forma de entrega no puede superar los 100 caracteres")
    private String deliveryMethod;

    @Size(max = 100, message = "La relación precio/calidad no puede superar los 100 caracteres")
    private String priceQualityRatio;
}