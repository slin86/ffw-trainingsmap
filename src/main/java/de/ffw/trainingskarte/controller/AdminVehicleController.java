package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.VehicleRepository;
import jakarta.servlet.http.HttpSession;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/vehicles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleController {

    static final List<String> VEHICLE_TYPES = List.of("HLF", "DLK", "TLF", "MTW", "RW", "ELW");

    static final int[] STATUSES = new int[]{1, 2, 3, 4, 6};

    private final VehicleRepository vehicleRepository;
    private final AppUserRepository appUserRepository;

    public AdminVehicleController(VehicleRepository vehicleRepository, AppUserRepository appUserRepository) {
        this.vehicleRepository = vehicleRepository;
        this.appUserRepository = appUserRepository;
    }

    String getStatusLabel(int status) {
        return switch (status) {
            case 1 -> "Frei uber Funk";
            case 2 -> "Frei auf Wache";
            case 3 -> "Einsatz ubernommen";
            case 4 -> "Am Einsatzort";
            case 6 -> "Ausser Dienst";
            default -> String.valueOf(status);
        };
    }

    @GetMapping
    public String list(Model model, HttpSession session) {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("vehicleTypes", VEHICLE_TYPES);
        model.addAttribute("statusLabels", getStatusMap());
        String flashMsg = (String) session.getAttribute("flashMessage");
        if (flashMsg != null) {
            model.addAttribute("flashMessage", flashMsg);
            session.removeAttribute("flashMessage");
        }
        return "admin/vehicles";
    }

    @PostMapping("/create")
    public String create(@RequestParam String callsign,
                         @RequestParam String type,
                         @RequestParam int status,
                         HttpSession session) {
        boolean exists = vehicleRepository.findByCallsign(callsign).isPresent();
        if (exists) {
            session.setAttribute("flashError", "Funkrufname '" + callsign + "' existiert bereits");
            return "redirect:/admin/vehicles";
        }
        Vehicle vehicle = new Vehicle(callsign, type, status, 53.5511, 9.9937);
        vehicleRepository.save(vehicle);
        session.setAttribute("flashMessage", "Fahrzeug '" + callsign + "' angelegt");
        return "redirect:/admin/vehicles";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fahrzeug nicht gefunden: " + id));
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("vehicleTypes", VEHICLE_TYPES);
        model.addAttribute("statusLabels", getStatusMap());
        return "admin/vehicle-edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String callsign,
                         @RequestParam String type,
                         @RequestParam int status,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         HttpSession session) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fahrzeug nicht gefunden: " + id));

        var existing = vehicleRepository.findByCallsign(callsign);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            session.setAttribute("flashError", "Funkrufname '" + callsign + "' existiert bereits");
            return "redirect:/admin/vehicles";
        }

        vehicle.setCallsign(callsign);
        vehicle.setType(type);
        vehicle.setStatus(status);
        vehicle.setLat(lat);
        vehicle.setLng(lng);
        vehicle.setUpdatedAt(OffsetDateTime.now());
        vehicleRepository.save(vehicle);

        session.setAttribute("flashMessage", "Fahrzeug '" + callsign + "' aktualisiert");
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Fahrzeug nicht gefunden: " + id));
        String callsign = vehicle.getCallsign();
        vehicleRepository.deleteById(id);
        session.setAttribute("flashMessage", "Fahrzeug '" + callsign + "' geloscht");
        return "redirect:/admin/vehicles";
    }

    private java.util.Map<Integer, String> getStatusMap() {
        java.util.LinkedHashMap<Integer, String> map = new java.util.LinkedHashMap<>();
        map.put(1, "Frei uber Funk");
        map.put(2, "Frei auf Wache");
        map.put(3, "Einsatz ubernommen");
        map.put(4, "Am Einsatzort");
        map.put(6, "Ausser Dienst");
        return map;
    }
}
