package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorRankingDTO {
    private Long idProveedor;
    private String razonSocial;
    private Boolean activo;
    private Long cantidadValoraciones;
    private Double promedioTiempoEntregaValorado;
    private Long ordenesRecibidas;
    private Double promedioDiasRecepcion;
    private Long recibidasSinFecha;
}
