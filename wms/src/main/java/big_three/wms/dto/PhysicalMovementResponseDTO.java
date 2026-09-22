package big_three.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalMovementResponseDTO {
    private Long idProduct;
    private LocalDateTime dateTime;
    private Long idLocationFrom;
    private Long idLocationTo;
    private Long idUser;
}