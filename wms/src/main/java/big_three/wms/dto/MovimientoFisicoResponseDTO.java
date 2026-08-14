package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoFisicoResponseDTO {

    private Long idProducto;
    private LocalDateTime fechaHora;
    private Long idUbicacionDesde;
    private Long idUbicacionHasta;
    private Long idUsuario;
}
