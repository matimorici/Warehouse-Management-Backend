package big_three.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UbicacionCreateDTO {
    @NotBlank(message = "El nombre de la ubicación no puede estar vacío")
    @Size(min = 3, max = 100, message = "El nombre de la ubicación debe tener entre 3 y 100 caracteres")
    private String nombreUbicacion;
}
