package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickOrderResponseDTO {

    private Long idOrdenRetiro;
    private LocalDateTime fechaHora;
    private Long idUsuario;
    private List<PickOrderLineResponseDTO> lineasRetiro;
}
