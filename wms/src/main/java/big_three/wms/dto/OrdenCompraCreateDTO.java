package big_three.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class OrdenCompraCreateDTO {

    @NotNull(message = "El proveedor no puede estar vacío")
    private Long idProveedor;

    @Valid
    @NotNull(message = "La orden debe tener al menos una línea")
    @Size(min = 1, message = "La orden debe tener al menos una línea")
    private List<LineaCompraCreateDTO> lineasCompra;
}
