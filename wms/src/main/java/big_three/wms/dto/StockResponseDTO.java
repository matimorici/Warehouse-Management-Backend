package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockResponseDTO {
    private Long idProducto;
    private LocalDateTime fechaHora;
    private Integer cantidadDisponible;
    private Integer cantidadPendiente;
}
