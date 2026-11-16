package big_three.wms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhysicalMovementCreateDTO {
    @NotNull(message = "Se requiere ID de producto")
    private Long idProduct;

    private Long idLocationFrom;

    @NotNull(message = "Lugar de Destino es requerido")
    private Long idLocationTo;

    @NotNull(message = "Se requiere ID de usuario")
    private Long idUser;
}