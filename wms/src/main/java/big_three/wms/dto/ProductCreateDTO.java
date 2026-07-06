package big_three.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductCreateDTO {
        //validaciones de forma
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        private String nombreProducto;

        @NotBlank(message = "El apellido no puede estar vacío")
        @Size(min = 3, max = 150, message = "El apellido debe tener entre 3 y 150 caracteres")
        private String descripcionProducto;

        @NotBlank(message = "El CUIL no puede estar vacío")
        @Pattern(regexp = "\\d{2}-\\d{8}-\\d{1}|\\d{11}", message = "El CUIL no tiene un formato válido (ej: 20-12345678-9)")
        private String codigoBarras;

        @NotBlank(message = "El CUIL no puede estar vacío")
        @Pattern(regexp = "\\d{2}-\\d{8}-\\d{1}|\\d{11}", message = "El CUIL no tiene un formato válido (ej: 20-12345678-9)")
        private String idProveedor;



        @NotBlank(message = "El CUIL no puede estar vacío")
        @Pattern(regexp = "\\d{2}-\\d{8}-\\d{1}|\\d{11}", message = "El CUIL no tiene un formato válido (ej: 20-12345678-9)")
        private String origenCodigoBarras;


}
