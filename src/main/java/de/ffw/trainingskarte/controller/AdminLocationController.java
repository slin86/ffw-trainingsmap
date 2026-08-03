package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.entity.Location;
import de.ffw.trainingskarte.entity.Station;
import de.ffw.trainingskarte.repository.IncidentRepository;
import de.ffw.trainingskarte.repository.LocationRepository;
import jakarta.servlet.http.HttpSession;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/locations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLocationController {

    private final LocationRepository locationRepository;
    private final IncidentRepository incidentRepository;

    public AdminLocationController(LocationRepository locationRepository, IncidentRepository incidentRepository) {
        this.locationRepository = locationRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String type, Model model, HttpSession session) {
        if ("STATION".equals(type)) {
            List<Station> stations = locationRepository.findAll()
                    .stream()
                    .filter(l -> l instanceof Station)
                    .map(Station.class::cast)
                    .toList();
            model.addAttribute("stations", stations);
            model.addAttribute("incidents", null);
        } else if ("INCIDENT".equals(type)) {
            List<Incident> incidents = incidentRepository.findByActiveTrue();
            model.addAttribute("locations", incidents);
        } else {
            List<Station> stations = locationRepository.findAll()
                    .stream()
                    .filter(l -> l instanceof Station)
                    .map(Station.class::cast)
                    .toList();
            List<Incident> incidents = incidentRepository.findByActiveTrue();
            model.addAttribute("stations", stations);
            model.addAttribute("incidents", incidents);
        }

        String flashMsg = (String) session.getAttribute("flashMessage");
        if (flashMsg != null) {
            model.addAttribute("flashMessage", flashMsg);
            session.removeAttribute("flashMessage");
        }
        String flashErr = (String) session.getAttribute("flashError");
        if (flashErr != null) {
            model.addAttribute("flashError", flashErr);
            session.removeAttribute("flashError");
        }

        return "admin/locations";
    }

    @PostMapping
    public String create(@RequestParam String type,
                         @RequestParam String name,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         HttpSession session) {
        Location location;
        if ("STATION".equals(type)) {
            Station station = new Station();
            station.setName(name);
            station.setLat(lat);
            station.setLng(lng);
            location = locationRepository.save(station);
        } else {
            Incident incident = new Incident();
            incident.setName(name);
            incident.setLat(lat);
            incident.setLng(lng);
            incident.setActive(true);
            location = incidentRepository.save(incident);
        }

        session.setAttribute("flashMessage", "Ort '" + name + "' angelegt");
        return "redirect:/admin/locations";
    }

    @PostMapping("/toggle/{id}")
    public String toggleActive(@PathVariable Long id, HttpSession session) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ort nicht gefunden: " + id));

        if (location instanceof Station station) {
            session.setAttribute("flashError", "Feuerwachen haben keinen aktiv/inaktiv Status");
            return "redirect:/admin/locations";
        }

        Incident incident = (Incident) location;
        incident.setActive(!incident.isActive());
        locationRepository.save(incident);

        String msg = incident.isActive() ? "Einsatzort aktiviert" : "Einsatzort deaktiviert";
        session.setAttribute("flashMessage", msg);
        return "redirect:/admin/locations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ort nicht gefunden: " + id));

        String name = location.getName();
        locationRepository.deleteById(id);

        session.setAttribute("flashMessage", "Ort '" + name + "' geloscht");
        return "redirect:/admin/locations";
    }
}
