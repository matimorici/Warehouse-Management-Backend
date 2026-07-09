package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PickOrderLineResponseDTO {

    private Long idProducto;
    private Integer cantidad;
}
