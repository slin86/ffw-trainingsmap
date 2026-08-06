package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.controller.dto.LocationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final de.ffw.trainingskarte.repository.StationRepository stationRepository;

    public StationController(de.ffw.trainingskarte.repository.StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping
    public java.util.List<de.ffw.trainingskarte.entity.Station> findAll() {
        return stationRepository.findAll();
    }

    @GetMapping("/{id}")
    public de.ffw.trainingskarte.entity.Station getById(@PathVariable Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<de.ffw.trainingskarte.entity.Station> create(@RequestBody LocationRequest request) {

        de.ffw.trainingskarte.entity.Station station = new de.ffw.trainingskarte.entity.Station();
        station.setName(request.name());
        station.setLat(request.lat());
        station.setLng(request.lng());
        if (request.description() != null) {
            station.setDescription(request.description());
        }
        var location = stationRepository.save(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(location);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<de.ffw.trainingskarte.entity.Station> update(@PathVariable Long id, @RequestBody LocationRequest request) {
        de.ffw.trainingskarte.entity.Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found: " + id));

        station.setName(request.name());
        station.setLat(request.lat());
        station.setLng(request.lng());
        if (request.description() != null) {
            station.setDescription(request.description());
        }

        station = stationRepository.save(station);
        return ResponseEntity.ok(station);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        stationRepository.deleteById(id);
    }

}
