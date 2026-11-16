package big_three.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Período compartido por los reportes históricos.
 *
 * <p>{@code from} es inclusivo y {@code toExclusive} es exclusivo. El factory
 * {@link #of(LocalDate, LocalDate)} traduce las fechas de la API (inclusivas,
 * granularidad de día) a un rango de {@link LocalDateTime} listo para las
 * consultas: {@code to} se convierte en el inicio del día siguiente.</p>
 */
public record ReportPeriod(LocalDateTime from, LocalDateTime toExclusive) {

    private static final LocalDateTime FROM_POR_DEFECTO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime TO_EXCLUSIVO_POR_DEFECTO = LocalDateTime.of(9999, 1, 1, 0, 0);

    public static ReportPeriod of(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha 'from' no puede ser posterior a 'to'");
        }

        LocalDateTime fromResuelto = from != null
                ? from.atStartOfDay()
                : FROM_POR_DEFECTO;
        LocalDateTime toExclusivoResuelto = to != null
                ? to.plusDays(1).atStartOfDay()
                : TO_EXCLUSIVO_POR_DEFECTO;

        return new ReportPeriod(fromResuelto, toExclusivoResuelto);
    }
}
