package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorResponseDTO {
    private Long idProveedor;
    private String cuit;
    private String razonSocial;
    private String telefono;
    private String mail;
    private String direccion;
}
