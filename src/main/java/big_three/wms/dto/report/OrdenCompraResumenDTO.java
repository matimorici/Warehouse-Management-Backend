package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResumenDTO {
    private Long idProveedor;
    private String razonSocial;
    private Boolean activo;
    private Long ordenesPendientes;
    private Long ordenesRecibidas;
    private Long ordenesCanceladas;
    private Long unidadesPedidas;
    private Long unidadesRecibidas;
}
