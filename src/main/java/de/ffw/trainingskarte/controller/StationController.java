package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.controller.dto.LocationRequest;
import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.entity.Location;
import de.ffw.trainingskarte.entity.Station;

import java.util.List;
import java.util.Map;

import de.ffw.trainingskarte.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationRepository stationRepository;

    public StationController(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping
    public List<Station> findAllWithVehicles() {
        return stationRepository.findAll();
    }

    @GetMapping("/{id}")
    public Station getById(@PathVariable Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Station> create(@RequestBody LocationRequest request) {

        Station station = new Station();
        station.setName(request.name());
        station.setLat(request.lat());
        station.setLng(request.lng());
        station.setDescription(request.description());
        var location = stationRepository.save(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(location);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Station> update(@PathVariable Long id, @RequestBody LocationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found: " + id));

        station.setName(request.name());
        station.setLat(request.lat());
        station.setLng(request.lng());
        station.setDescription(request.description());

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
