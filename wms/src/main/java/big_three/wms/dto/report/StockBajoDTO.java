package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBajoDTO {
    private Long idProducto;
    private String nombreProducto;
    private String codigoBarras;
    private Long idProveedor;
    private String razonSocial;
    private Integer cantidadDisponible;
    private Integer cantidadPendiente;
    private LocalDateTime stockFechaHora;
}
