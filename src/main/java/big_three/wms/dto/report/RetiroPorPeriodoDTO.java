package big_three.wms.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetiroPorPeriodoDTO {
    private LocalDate periodo;
    private Long cantidadOrdenes;
    private Long unidadesRetiradas;
}
