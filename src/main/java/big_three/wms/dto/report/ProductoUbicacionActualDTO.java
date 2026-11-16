package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUbicacionActualDTO {
    private Long idProducto;
    private String nombreProducto;
    private String codigoBarras;
    private Long idUbicacion;
    private String nombreUbicacion;
    private LocalDateTime fechaUltimoMovimiento;
}
