package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BajaAuditoriaDTO {
    private String entidad;
    private Long idEntidad;
    private String descripcion;
    private LocalDateTime fechaBaja;
    private Long idUsuarioBaja;
    private String usuarioBaja;
}
