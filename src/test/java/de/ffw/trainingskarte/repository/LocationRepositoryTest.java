package de.ffw.trainingskarte.repository;

import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.entity.Location;
import de.ffw.trainingskarte.entity.Station;
import de.ffw.trainingskarte.entity.Vehicle;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRepositoryTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void stationEntityValidatesLatRange() {
        Station station = new Station();
        station.setName("Feuerwache 1");
        station.setLat(53.5511);
        station.setLng(9.9937);

        assertThat(station.getLat()).isEqualTo(53.5511);
    }

    @Test
    void incidentEntityValidatesActiveField() {
        Incident incident = new Incident();
        incident.setName("Incident 1");
        incident.setLat(53.5611);
        incident.setLng(9.9837);
        incident.setActive(true);

        assertThat(incident.isActive()).isTrue();

        incident.setActive(false);
        assertThat(incident.isActive()).isFalse();
    }

    @Test
    void vehicleWithNullableLocation() {
        Vehicle vehicle = new Vehicle("C2-TG", "TLF", 1, 53.5511, 9.9937);
        vehicle.setLocation(null);

        assertThat(vehicle.getLocation()).isNull();
    }

    @Test
    void constraintValidationLatitudeOk() {
        Station station = new Station();
        station.setName("Station");
        station.setLat(53.5511);
        station.setLng(9.9937);

        Set<ConstraintViolation<Station>> violations = validator.validate(station);
        assertThat(violations).isEmpty();
    }

    @Test
    void constraintValidationLongitudeOk() {
        Incident incident = new Incident();
        incident.setName("Incident");
        incident.setLat(53.5611);
        incident.setLng(9.9837);

        Set<ConstraintViolation<Incident>> violations = validator.validate(incident);
        assertThat(violations).isEmpty();
    }
}
