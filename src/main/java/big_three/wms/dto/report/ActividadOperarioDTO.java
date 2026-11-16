package big_three.wms.dto.report;

import big_three.wms.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActividadOperarioDTO {
    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String cuil;
    private Role rol;
    private Boolean activo;
    private Long cantidadRetiros;
    private Long unidadesRetiradas;
    private Long cantidadMovimientos;
    private LocalDateTime ultimaActividad;
}
