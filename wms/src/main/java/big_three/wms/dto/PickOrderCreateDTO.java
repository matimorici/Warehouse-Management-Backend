package big_three.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PickOrderCreateDTO {

    @NotNull(message = "El usuario no puede estar vacío")
    private Long idUsuario;

    @Valid
    @NotNull(message = "La orden debe tener al menos una línea")
    private List<PickOrderLineCreateDTO> lineasRetiro;
}
