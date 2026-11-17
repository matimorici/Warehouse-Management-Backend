package big_three.wms.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StockUpdateDTO {
    @Min(value = 0, message = "La cantidad disponible no puede ser negativa")
    private Integer cantidadDisponible;

    @Min(value = 0, message = "La cantidad pendiente no puede ser negativa")
    private Integer cantidadPendiente;
}
