package big_three.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ValoracionProveedorCreateDTO {
    @NotNull(message = "El proveedor es obligatorio")
    private Long idProveedor;

    @Min(value = 0, message = "El tiempo de entrega no puede ser negativo")
    private Integer tiempoEntrega;

    @Size(max = 100, message = "La forma de entrega no puede superar los 100 caracteres")
    private String formaEntrega;

    @Size(max = 100, message = "La relación precio-calidad no puede superar los 100 caracteres")
    private String relacionPrecioCalidad;
}
