package big_three.wms.controller;

import big_three.wms.dto.report.ActividadOperarioDTO;
import big_three.wms.dto.report.ProductoMasRetiradoDTO;
import big_three.wms.dto.report.ProductoSinRotacionDTO;
import big_three.wms.dto.report.ProductoUbicacionActualDTO;
import big_three.wms.dto.report.RetiroPorPeriodoDTO;
import big_three.wms.dto.report.StockBajoDTO;
import big_three.wms.dto.report.StockPorProveedorDTO;
import big_three.wms.dto.report.UbicacionOcupacionDTO;
import big_three.wms.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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

    @GetMapping("/productos/mas-retirados")
    public ResponseEntity<List<ProductoMasRetiradoDTO>> productosMasRetirados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(reportService.findProductosMasRetirados(from, to, limit));
    }

    @GetMapping("/productos/sin-rotacion")
    public ResponseEntity<List<ProductoSinRotacionDTO>> productosSinRotacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean soloConStock) {
        return ResponseEntity.ok(reportService.findProductosSinRotacion(from, to, soloConStock));
    }

    @GetMapping("/retiros/por-periodo")
    public ResponseEntity<List<RetiroPorPeriodoDTO>> retirosPorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String agrupacion) {
        return ResponseEntity.ok(reportService.findRetirosPorPeriodo(from, to, agrupacion));
    }

    @GetMapping("/operarios/actividad")
    public ResponseEntity<List<ActividadOperarioDTO>> operariosActividad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.findActividadOperarios(from, to));
    }
}
