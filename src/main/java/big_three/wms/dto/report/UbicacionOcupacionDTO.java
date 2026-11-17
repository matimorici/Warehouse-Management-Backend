package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionOcupacionDTO {
    private Long idUbicacion;
    private String nombreUbicacion;
    private Long cantidadProductos;
}
