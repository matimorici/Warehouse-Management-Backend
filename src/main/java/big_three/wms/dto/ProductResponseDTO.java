package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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
    private Integer cantidadDisponible;
    private Integer cantidadPendiente;
    private LocalDateTime stockFechaHora;
}
