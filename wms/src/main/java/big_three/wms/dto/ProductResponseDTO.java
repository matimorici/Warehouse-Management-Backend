package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long idProducto;
    private String nombreProducto;
    private String descripcionProducto;
    private String codigoBarras;
    private Long idProveedor;
    private String origenCodigoBarras;
}
