package big_three.wms.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportPeriodTest {

    @Test
    void of_sinFechas_usaElRangoCompleto() {
        ReportPeriod periodo = ReportPeriod.of(null, null);

        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), periodo.from());
        assertEquals(LocalDateTime.of(9999, 1, 1, 0, 0), periodo.toExclusive());
    }

    @Test
    void of_soloFrom_usaElFinPorDefecto() {
        ReportPeriod periodo = ReportPeriod.of(LocalDate.of(2026, 3, 10), null);

        assertEquals(LocalDateTime.of(2026, 3, 10, 0, 0), periodo.from());
        assertEquals(LocalDateTime.of(9999, 1, 1, 0, 0), periodo.toExclusive());
    }

    @Test
    void of_soloTo_usaElInicioPorDefectoYElDiaSiguienteComoExclusivo() {
        ReportPeriod periodo = ReportPeriod.of(null, LocalDate.of(2026, 3, 10));

        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), periodo.from());
        assertEquals(LocalDateTime.of(2026, 3, 11, 0, 0), periodo.toExclusive());
    }

    @Test
    void of_rangoValido_dejaElToExclusivoEnElDiaSiguiente() {
        ReportPeriod periodo = ReportPeriod.of(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));

        assertEquals(LocalDateTime.of(2026, 3, 10, 0, 0), periodo.from());
        assertEquals(LocalDateTime.of(2026, 3, 13, 0, 0), periodo.toExclusive());
    }

    @Test
    void of_mismoDia_representaUnSoloDia() {
        ReportPeriod periodo = ReportPeriod.of(
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10));

        assertEquals(LocalDateTime.of(2026, 3, 10, 0, 0), periodo.from());
        assertEquals(LocalDateTime.of(2026, 3, 11, 0, 0), periodo.toExclusive());
    }

    @Test
    void of_fromPosteriorATo_lanzaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.of(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 10)));

        assertEquals("La fecha 'from' no puede ser posterior a 'to'", ex.getMessage());
    }
}
