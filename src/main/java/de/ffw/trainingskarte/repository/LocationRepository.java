package de.ffw.trainingskarte.repository;

import de.ffw.trainingskarte.entity.Location;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT l FROM Location l LEFT JOIN FETCH l.vehicles")
    List<Location> findAllWithVehicles();

}
