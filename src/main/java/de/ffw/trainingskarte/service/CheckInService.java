package de.ffw.trainingskarte.service;

import de.ffw.trainingskarte.repository.VehicleCheckinRepository;
import org.springframework.stereotype.Service;

@Service
public class CheckInService {

    private final VehicleCheckinRepository vehicleCheckinRepository;

    public CheckInService(VehicleCheckinRepository vehicleCheckinRepository) {
        this.vehicleCheckinRepository = vehicleCheckinRepository;
    }

    public boolean isCheckedIn(Long vehicleId, String username) {
        return vehicleCheckinRepository.findByUsername(username)
            .map(checkin -> checkin.getVehicle().getId().equals(vehicleId))
            .orElse(false);
    }
}
