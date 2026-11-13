package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMasRetiradoDTO {
    private Long idProducto;
    private String nombreProducto;
    private String codigoBarras;
    private Boolean activo;
    private Long unidadesRetiradas;
    private Long cantidadOrdenes;
}
