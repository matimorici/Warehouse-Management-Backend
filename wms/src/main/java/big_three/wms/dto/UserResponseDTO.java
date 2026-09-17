package big_three.wms.dto;

import big_three.wms.model.Role;
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
    private Role rol;
}
