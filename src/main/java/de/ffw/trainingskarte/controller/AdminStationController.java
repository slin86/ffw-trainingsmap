package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Station;
import de.ffw.trainingskarte.repository.StationRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/stations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStationController {

    private final StationRepository stationRepository;

    public AdminStationController(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @GetMapping
    public String list(Model model, HttpSession session) {
        List<Station> stations = stationRepository.findAll()
                .stream()
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

    @GetMapping("/new")
    public String newStation(Model model) {
        model.addAttribute("station", null);
        return "admin/station-form";
    }

    @GetMapping("/{id}/edit")
    public String editStation(@PathVariable Long id, Model model) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ort nicht gefunden: " + id));
        model.addAttribute("station", station);
        return "admin/station-form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         @RequestParam(required = false) String description,
                         HttpSession session) {
        Station station = new Station();
        station.setName(name);
        station.setLat(lat);
        station.setLng(lng);
        station.setDescription(description);
        stationRepository.save(station);

        session.setAttribute("flashMessage", "Feuerwache '" + name + "' angelegt");
        return "redirect:/admin/stations";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         @RequestParam(required = false) String description,
                         HttpSession session) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ort nicht gefunden: " + id));

        station.setName(name);
        station.setLat(lat);
        station.setLng(lng);
        station.setDescription(description);
        stationRepository.save(station);

        session.setAttribute("flashMessage", "Feuerwache '" + name + "' aktualisiert");
        return "redirect:/admin/stations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ort nicht gefunden: " + id));

        String name = station.getName();
        stationRepository.deleteById(id);

        session.setAttribute("flashMessage", "Feuerwache '" + name + "' gelöscht");
        return "redirect:/admin/stations";
    }
}
