package big_three.wms.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequestDTO {

    @NotBlank(message = "El CUIL no puede estar vacío")
    private String cuil;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String contrasena;
}
