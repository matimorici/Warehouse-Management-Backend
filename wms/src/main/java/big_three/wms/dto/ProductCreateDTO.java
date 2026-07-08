package big_three.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductCreateDTO {

        @NotBlank(message = "El nombre del producto no puede estar vacío")
        @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
        private String nombreProducto;

        @NotBlank(message = "La descripción no puede estar vacía")
        @Size(min = 3, max = 500, message = "La descripción debe tener entre 3 y 500 caracteres")
        private String descripcionProducto;

        @Size(max = 50, message = "El código de barras no puede superar los 50 caracteres")
        private String codigoBarras;

        private Long idProveedor;

        @NotBlank(message = "El origen del código de barras es obligatorio")
        @Pattern(regexp = "FABRICANTE|INTERNO", message = "El origen debe ser FABRICANTE o INTERNO")
        private String origenCodigoBarras;

}
