package big_three.wms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MovimientoFisicoCreateDTO {

    @NotNull(message = "El producto no puede estar vacío")
    private Long idProducto;

    private Long idUbicacionDesde;

    @NotNull(message = "La ubicación de destino no puede estar vacía")
    private Long idUbicacionHasta;

    @NotNull(message = "El usuario no puede estar vacío")
    private Long idUsuario;
}
