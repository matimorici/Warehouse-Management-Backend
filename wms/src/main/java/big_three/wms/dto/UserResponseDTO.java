package big_three.wms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String cuil;
    private String rol;
}
