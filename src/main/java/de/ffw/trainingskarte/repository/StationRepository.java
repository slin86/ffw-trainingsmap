package de.ffw.trainingskarte.repository;

import de.ffw.trainingskarte.entity.Station;
import org.springframework.stereotype.Repository;

@Repository
public interface StationRepository extends LocationRepository<Station> {

}
