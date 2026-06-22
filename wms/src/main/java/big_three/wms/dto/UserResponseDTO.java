package big_three.wms.dto;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    UUID idUsuario;
    String nombre;
    String apellido;
    String cuil;
    String rol;
}
