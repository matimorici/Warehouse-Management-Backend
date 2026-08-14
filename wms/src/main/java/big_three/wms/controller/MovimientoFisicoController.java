package big_three.wms.controller;

import big_three.wms.dto.MovimientoFisicoCreateDTO;
import big_three.wms.dto.MovimientoFisicoResponseDTO;
import big_three.wms.service.MovimientoFisicoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos-fisicos")
@CrossOrigin(origins = "http://localhost:4200")
public class MovimientoFisicoController {

    private final MovimientoFisicoService movimientoFisicoService;

    public MovimientoFisicoController(MovimientoFisicoService movimientoFisicoService) {
        this.movimientoFisicoService = movimientoFisicoService;
    }

    @PostMapping
    public ResponseEntity<MovimientoFisicoResponseDTO> create(@Valid @RequestBody MovimientoFisicoCreateDTO dto) {
        MovimientoFisicoResponseDTO response = movimientoFisicoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MovimientoFisicoResponseDTO>> list() {
        return ResponseEntity.ok(movimientoFisicoService.findAll());
    }

    @GetMapping("/producto/{idProducto}")
    public ResponseEntity<List<MovimientoFisicoResponseDTO>> listByProducto(@PathVariable Long idProducto) {
        return ResponseEntity.ok(movimientoFisicoService.findByProducto(idProducto));
    }

    @GetMapping("/producto/{idProducto}/fecha/{fechaHora}")
    public ResponseEntity<MovimientoFisicoResponseDTO> search(@PathVariable Long idProducto,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {
        return ResponseEntity.ok(movimientoFisicoService.findByIdProductoAndFechaHora(idProducto, fechaHora));
    }
}
