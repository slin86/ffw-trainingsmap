package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Location;
import de.ffw.trainingskarte.entity.Station;
import de.ffw.trainingskarte.repository.LocationRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/stations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStationController {

    private final LocationRepository locationRepository;

    public AdminStationController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping
    public String list(Model model, HttpSession session) {
        List<Station> stations = locationRepository.findAll()
                .stream()
                .filter(l -> l instanceof Station)
                .map(Station.class::cast)
                .toList();

        model.addAttribute("stations", stations);

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

        return "admin/stations";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         HttpSession session) {
        Station station = new Station();
        station.setName(name);
        station.setLat(lat);
        station.setLng(lng);
        locationRepository.save(station);

        session.setAttribute("flashMessage", "Feuerwache '" + name + "' angelegt");
        return "redirect:/admin/stations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Optional<Location> locationOpt = (Optional<Location>) locationRepository.findById(id);
        Location location = locationOpt.orElseThrow(() -> new IllegalArgumentException("Ort nicht gefunden: " + id));

        if (!(location instanceof Station station)) {
            session.setAttribute("flashError", "Keine Feuerwache gefunden");
            return "redirect:/admin/stations";
        }

        String name = station.getName();
        locationRepository.deleteById(id);

        session.setAttribute("flashMessage", "Feuerwache '" + name + "' geloscht");
        return "redirect:/admin/stations";
    }
}
