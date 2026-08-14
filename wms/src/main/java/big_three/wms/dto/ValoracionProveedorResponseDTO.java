package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValoracionProveedorResponseDTO {
    private Long idValoracion;
    private Long idProveedor;
    private LocalDateTime fechaHora;
    private Integer tiempoEntrega;
    private String formaEntrega;
    private String relacionPrecioCalidad;
}
