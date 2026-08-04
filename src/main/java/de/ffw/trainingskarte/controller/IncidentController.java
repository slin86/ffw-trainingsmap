package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.controller.dto.LocationRequest;
import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.entity.Location;
import de.ffw.trainingskarte.repository.IncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;

    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public List<Incident> list(@RequestParam(required = false) Boolean all) {
        if (Boolean.TRUE.equals(all)) {
            return incidentRepository.findAll();
        }
        return incidentRepository.findByActiveTrue();
    }

    @GetMapping("/{id}")
    public Incident getById(@PathVariable Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Location> create(@RequestBody LocationRequest request) {
        Incident incident = new Incident();
        incident.setName(request.name());
        incident.setLat(request.lat());
        incident.setLng(request.lng());
        incident.setActive(request.active() != null ? request.active() : true);
        var location = incidentRepository.save(incident);
        return ResponseEntity.status(HttpStatus.CREATED).body(location);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody LocationRequest request) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("incident not found: " + id));

        incident.setName(request.name());
        incident.setLat(request.lat());
        incident.setLng(request.lng());

        if (request.active() != null) {
            incident.setActive(request.active());
        }

        incident = incidentRepository.save(incident);
        return ResponseEntity.ok(incident);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        incidentRepository.deleteById(id);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("incident not found: " + id));

        if (!payload.containsKey("active")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing 'active' field in request body"));
        }

        incident.setActive(payload.get("active"));
        incidentRepository.save(incident);

        return ResponseEntity.ok(Map.of("id", String.valueOf(incident.getId()), "active", incident.isActive()));
    }
}
