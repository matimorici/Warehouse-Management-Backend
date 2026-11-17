package big_three.wms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProveedorCreateDTO {
    @NotBlank(message = "El CUIT no puede estar vacío")
    @Pattern(regexp = "\\d{2}-\\d{8}-\\d{1}|\\d{11}", message = "El CUIT no tiene un formato válido (ej: 20-12345678-9)")
    private String cuit;

    @NotBlank(message = "La razón social no puede estar vacía")
    @Size(min = 3, max = 150, message = "La razón social debe tener entre 3 y 150 caracteres")
    private String razonSocial;

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefono;

    @Email(message = "El mail no tiene un formato válido")
    @Size(max = 150, message = "El mail no puede superar los 150 caracteres")
    private String mail;

    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
    private String direccion;
}
