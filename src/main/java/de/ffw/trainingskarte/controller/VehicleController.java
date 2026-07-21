package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.controller.dto.PositionRequest;
import de.ffw.trainingskarte.controller.dto.VehicleRequest;
import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.VehicleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private static final double MIN_LAT = 53.3;
    private static final double MAX_LAT = 53.8;
    private static final double MIN_LNG = 9.6;
    private static final double MAX_LNG = 10.4;

    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public List<Vehicle> list() {
        return vehicleRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> create(@RequestBody VehicleRequest request) {
        Optional<Vehicle> existing = vehicleRepository.findByCallsign(request.callsign());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Callsign bereits vorhanden: " + request.callsign()));
        }
        Vehicle vehicle = new Vehicle(
            request.callsign(),
            request.type(),
            request.status(),
            53.5511,
            9.9937
        );
        vehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicle);
    }

    @PutMapping("/{id}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));

        Optional<Vehicle> existing = vehicleRepository.findByCallsign(request.callsign());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Callsign bereits vorhanden: " + request.callsign()));
        }

        vehicle.setCallsign(request.callsign());
        vehicle.setType(request.type());
        vehicle.setStatus(request.status());
        vehicle.setUpdatedAt(OffsetDateTime.now());
        vehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(vehicle);
    }

    @PutMapping("/{id}/position")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<?> updatePosition(@PathVariable Long id, @RequestBody PositionRequest request) {
        if (request.lat() < MIN_LAT || request.lat() > MAX_LAT
            || request.lng() < MIN_LNG || request.lng() > MAX_LNG) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Koordinaten muessen im Hamburger Stadtgebiet liegen: lat 53.3-53.8, lng 9.6-10.4"));
        }

        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));

        vehicle.setLat(request.lat());
        vehicle.setLng(request.lng());
        vehicle.setUpdatedAt(OffsetDateTime.now());
        vehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(vehicle);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured("ROLE_ADMIN")
    public void delete(@PathVariable Long id) {
        vehicleRepository.deleteById(id);
    }
}
