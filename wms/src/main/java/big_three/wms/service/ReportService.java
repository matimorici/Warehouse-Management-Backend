package big_three.wms.service;

import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    private static final int UMBRAL_POR_DEFECTO = 10;

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<StockBajoDTO> findStockBajo(Integer umbral) {
        int umbralResuelto = umbral != null ? umbral : UMBRAL_POR_DEFECTO;
        if (umbralResuelto < 0) {
            throw new IllegalArgumentException("El umbral no puede ser negativo");
        }
        return reportRepository.findStockBajo(umbralResuelto);
    }

    public List<StockPorProveedorDTO> findStockPorProveedor() {
        return reportRepository.findStockPorProveedor();
    }

    public List<ProductoUbicacionActualDTO> findProductosUbicacionActual(Boolean soloSinUbicar) {
        boolean filtro = Boolean.TRUE.equals(soloSinUbicar);
        return reportRepository.findProductosUbicacionActual(filtro).stream()
                .map(this::convertirProductoUbicacion)
                .toList();
    }

    public List<UbicacionOcupacionDTO> findUbicacionesOcupacion() {
        return reportRepository.findUbicacionesOcupacion().stream()
                .map(this::convertirUbicacionOcupacion)
                .toList();
    }

    private ProductoUbicacionActualDTO convertirProductoUbicacion(
            ReportRepository.ProductoUbicacionActualProjection fila) {
        return new ProductoUbicacionActualDTO(
                fila.getIdProducto(),
                fila.getNombreProducto(),
                fila.getCodigoBarras(),
                fila.getIdUbicacion(),
                fila.getNombreUbicacion(),
                fila.getFechaUltimoMovimiento());
    }

    private UbicacionOcupacionDTO convertirUbicacionOcupacion(
            ReportRepository.UbicacionOcupacionProjection fila) {
        return new UbicacionOcupacionDTO(
                fila.getIdUbicacion(),
                fila.getNombreUbicacion(),
                fila.getCantidadProductos());
    }
}
