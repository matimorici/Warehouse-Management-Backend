package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    Long idProducto;
    String nombreProducto;
    String descripcionProducto;
    String codigoBarras;
    Long idProveedor;
    String origenCodigoBarras;
}
