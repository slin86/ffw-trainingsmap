package de.ffw.trainingskarte.repository;

import de.ffw.trainingskarte.entity.Vehicle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByCallsign(String callsign);
}
