package big_three.wms.controller;

import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<StockBajoDTO>> stockBajo(
            @RequestParam(required = false) Integer umbral) {
        return ResponseEntity.ok(reportService.findStockBajo(umbral));
    }

    @GetMapping("/stock-por-proveedor")
    public ResponseEntity<List<StockPorProveedorDTO>> stockPorProveedor() {
        return ResponseEntity.ok(reportService.findStockPorProveedor());
    }

    @GetMapping("/productos/ubicacion-actual")
    public ResponseEntity<List<ProductoUbicacionActualDTO>> productosUbicacionActual(
            @RequestParam(required = false) Boolean soloSinUbicar) {
        return ResponseEntity.ok(reportService.findProductosUbicacionActual(soloSinUbicar));
    }

    @GetMapping("/ubicaciones/ocupacion")
    public ResponseEntity<List<UbicacionOcupacionDTO>> ubicacionesOcupacion() {
        return ResponseEntity.ok(reportService.findUbicacionesOcupacion());
    }
}
