package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.VehicleCheckinRepository;
import de.ffw.trainingskarte.repository.VehicleRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckInController {

    private final VehicleRepository vehicleRepository;
    private final VehicleCheckinRepository vehicleCheckinRepository;

    public CheckInController(VehicleRepository vehicleRepository, VehicleCheckinRepository vehicleCheckinRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleCheckinRepository = vehicleCheckinRepository;
    }

    @PostMapping("/vehicles/{id}/checkin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkIn(@PathVariable Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found: " + id));

        String username = getCurrentUsername();
        
        vehicleCheckinRepository.findByUsername(username)
            .ifPresent(vehicleCheckinRepository::delete);
        
        de.ffw.trainingskarte.entity.VehicleCheckin checkin = new de.ffw.trainingskarte.entity.VehicleCheckin();
        checkin.setVehicle(vehicle);
        checkin.setUsername(username);
        checkin.setCheckedInAt(OffsetDateTime.now());
        vehicleCheckinRepository.save(checkin);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("vehicleId", id));
    }

    @PostMapping("/vehicles/{id}/checkout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void checkOut(@PathVariable Long id) {
        String username = getCurrentUsername();
        
        vehicleCheckinRepository.findByUsername(username)
            .ifPresent(checkin -> {
                if (checkin.getVehicle().getId().equals(id)) {
                    vehicleCheckinRepository.delete(checkin);
                }
            });
    }

    @GetMapping("/checkin/me")
    public ResponseEntity<?> getMyCheckin() {
        String username = getCurrentUsername();
        
        return vehicleCheckinRepository.findByUsername(username)
            .map(checkin -> ResponseEntity.ok(Map.of("vehicleId", checkin.getVehicle().getId())))
            .orElse(ResponseEntity.noContent().build());
    }

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
