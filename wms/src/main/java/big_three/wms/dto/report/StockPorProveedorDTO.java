package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPorProveedorDTO {
    private Long idProveedor;
    private String razonSocial;
    private Long cantidadProductos;
    private Long unidadesDisponibles;
    private Long unidadesPendientes;
}
