package de.ffw.trainingskarte.service;

import de.ffw.trainingskarte.entity.Scene;
import de.ffw.trainingskarte.repository.SceneRepository;
import de.ffw.trainingskarte.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SceneService {

    private static final double MIN_LAT = 53.3;
    private static final double MAX_LAT = 53.8;
    private static final double MIN_LNG = 9.6;
    private static final double MAX_LNG = 10.4;

    private final SceneRepository sceneRepository;
    private final VehicleRepository vehicleRepository;

    public SceneService(SceneRepository sceneRepository, VehicleRepository vehicleRepository) {
        this.sceneRepository = sceneRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<Scene> findAll() {
        return sceneRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Scene findById(Long id) {
        return sceneRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Szenario nicht gefunden: " + id));
    }

    private boolean titleExists(String title) {
        return findAll().stream().anyMatch(s -> s.getTitle().equals(title));
    }

    public Scene create(String title, String description, double lat, double lng, Long vehicleId) {
        validateLocation(lat, lng);

        if (titleExists(title)) {
            throw new RuntimeException("Szenario-Titel '" + title + "' existiert bereits");
        }

        var existingScene = sceneRepository.findAll().stream()
            .filter(s -> s.getTitle().equals(title))
            .findFirst();
        if (existingScene.isPresent()) {
            throw new RuntimeException("Szenario-Titel '" + title + "' existiert bereits");
        }

        Scene scene;
        if (vehicleId != null && vehicleId > 0L) {
            var vehicle = vehicleRepository.findById(vehicleId);
            if (vehicle.isEmpty()) {
                throw new EntityNotFoundException("Fahrzeug nicht gefunden: " + vehicleId);
            }
            scene = new Scene(title, description, lat, lng, vehicle.get());
        } else {
            scene = new Scene(title, description, lat, lng, null);
        }

        return sceneRepository.save(scene);
    }

    public void update(Long id, String title, String description, double lat, double lng, Long vehicleId) {
        validateLocation(lat, lng);

        var existingScene = findById(id);
        if (!existingScene.getTitle().equals(title)) {
            if (findAll().stream().anyMatch(s -> s.getTitle().equals(title))) {
                throw new RuntimeException("Szenario-Titel '" + title + "' existiert bereits");
            }
        }

        existingScene.setTitle(title);
        existingScene.setDescription(description);
        existingScene.setLat(lat);
        existingScene.setLng(lng);

        if (vehicleId != null && vehicleId > 0L) {
            var vehicle = vehicleRepository.findById(vehicleId).orElse(null);
            existingScene.setVehicle(vehicle);
        } else {
            existingScene.setVehicle(null);
        }

        sceneRepository.save(existingScene);
    }

    public void delete(Long id) {
        findById(id);
        sceneRepository.deleteById(id);
    }

    private void validateLocation(double lat, double lng) {
        if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
            throw new RuntimeException("Koordinaten muessen im Hamburger Stadtgebiet liegen: lat 53.3-53.8, lng 9.6-10.4");
        }
    }
}
