package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.controller.dto.LocationRequest;
import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.entity.Location;
import de.ffw.trainingskarte.entity.Station;
import de.ffw.trainingskarte.repository.IncidentRepository;
import de.ffw.trainingskarte.repository.LocationRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationRepository locationRepository;
    private final IncidentRepository incidentRepository;

    public LocationController(LocationRepository locationRepository, IncidentRepository incidentRepository) {
        this.locationRepository = locationRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean all) {

        if (type != null && "STATION".equalsIgnoreCase(type)) {
            List<Location> stations = locationRepository.findAllWithVehicles()
                    .stream()
                    .filter(s -> s instanceof Station)
                    .toList();
            return ResponseEntity.ok(stations);
        }

        if ("INCIDENT".equals(type)) {
            List<Incident> incidents = all == true
                    ? incidentRepository.findAll()
                    : incidentRepository.findByActiveTrue();
            return ResponseEntity.ok(incidents);
        }

        List<Location> stations = locationRepository.findAllWithVehicles()
                .stream()
                .filter(s -> s instanceof Station)
                .toList();
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getById(@PathVariable Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
        return ResponseEntity.ok(location);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Location> create(@RequestBody LocationRequest request) {
        Location location;
        if ("INCIDENT".equalsIgnoreCase(request.type())) {
            Incident incident = new Incident();
            incident.setName(request.name());
            incident.setLat(request.lat());
            incident.setLng(request.lng());
            incident.setActive(request.active() != null ? request.active() : true);
            location = incidentRepository.save(incident);
        } else {
            Station station = new Station();
            station.setName(request.name());
            station.setLat(request.lat());
            station.setLng(request.lng());
            location = locationRepository.save(station);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(location);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody LocationRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));

        if (location instanceof Station && request.type() != null && !"STATION".equalsIgnoreCase(request.type())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Station type cannot be changed"));
        }

        location.setName(request.name());
        location.setLat(request.lat());
        location.setLng(request.lng());

        if (location instanceof Incident incident) {
            if (request.active() != null) {
                incident.setActive(request.active());
            }
        }

        location = locationRepository.save(location);
        return ResponseEntity.ok(location);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        locationRepository.deleteById(id);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));

        if (location instanceof Station station) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Station cannot have active status"));
        }

        if (!payload.containsKey("active")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'active' field in request body"));
        }

        Incident incident = (Incident) location;
        incident.setActive(payload.get("active"));
        locationRepository.save(incident);

        return ResponseEntity.ok(Map.of("id", String.valueOf(incident.getId()), "active", incident.isActive()));
    }
}
