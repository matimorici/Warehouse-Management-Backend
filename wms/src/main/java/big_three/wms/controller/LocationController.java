package big_three.wms.controller;

import big_three.wms.dto.LocationCreateDTO;
import big_three.wms.dto.LocationResponseDTO;
import big_three.wms.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@CrossOrigin(origins = "http://localhost:4200")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }
    @PostMapping
    public ResponseEntity<LocationResponseDTO> create(@Valid @RequestBody LocationCreateDTO dto) {
        LocationResponseDTO response = locationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LocationResponseDTO>> list() {
        return ResponseEntity.ok(locationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponseDTO> search(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponseDTO> update(@PathVariable Long id, @Valid @RequestBody LocationCreateDTO dto) {
        return ResponseEntity.ok(locationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
