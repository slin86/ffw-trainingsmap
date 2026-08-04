package de.ffw.trainingskarte.repository;

import de.ffw.trainingskarte.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocationRepository<L extends Location> extends JpaRepository<L, Long> {

    @Query("SELECT l FROM Location l LEFT JOIN FETCH l.vehicles")
    List<L> findAllWithVehicles();

}
