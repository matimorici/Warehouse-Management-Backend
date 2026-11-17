package big_three.wms.dto;

import big_three.wms.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String cuil;
    private Role rol;
    private LocalDateTime fechaBaja;

    public UserResponseDTO(Long idUsuario, String nombre, String apellido, String cuil, Role rol) {
        this(idUsuario, nombre, apellido, cuil, rol, null);
    }
}
