package de.ffw.trainingskarte.repository;

import de.ffw.trainingskarte.entity.VehicleCheckin;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleCheckinRepository extends JpaRepository<VehicleCheckin, Long> {

    Optional<VehicleCheckin> findByUsername(String username);

    List<VehicleCheckin> findByVehicleId(Long vehicleId);
}
