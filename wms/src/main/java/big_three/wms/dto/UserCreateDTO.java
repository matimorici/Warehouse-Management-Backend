package big_three.wms.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class UserCreateDTO {
        //validaciones de forma
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        private String nombre;

        @NotBlank(message = "El apellido no puede estar vacío")
        @Size(min = 3, max = 150, message = "El apellido debe tener entre 3 y 150 caracteres")
        private String apellido;

        @NotBlank(message = "El CUIL no puede estar vacío")
        @Pattern(regexp = "\\d{2}-\\d{8}-\\d{1}|\\d{11}", message = "El CUIL no tiene un formato válido (ej: 20-12345678-9)")
        private String cuil;

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$", message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número")
        private String contrasena;


}
