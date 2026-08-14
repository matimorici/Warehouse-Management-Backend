package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponseDTO {

    private Long idOrdenCompra;
    private LocalDateTime fechaHora;
    private Long idProveedor;
    private String estado;
    private List<LineaCompraResponseDTO> lineasCompra;
}
