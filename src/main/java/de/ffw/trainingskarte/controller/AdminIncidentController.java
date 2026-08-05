package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.repository.IncidentRepository;
import jakarta.servlet.http.HttpSession;
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
@RequestMapping("/admin/incidents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIncidentController {

    private final IncidentRepository incidentRepository;

    public AdminIncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Boolean all, Model model, HttpSession session) {
        List<Incident> incidents;
        if (Boolean.TRUE.equals(all)) {
            incidents = incidentRepository.findAll();
        } else {
            incidents = incidentRepository.findByActiveTrue();
        }
        model.addAttribute("incidents", incidents);
        model.addAttribute("showAll", Boolean.TRUE.equals(all));

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

        return "admin/incidents";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("location", null);
        model.addAttribute("locationType", "INCIDENT");
        model.addAttribute("formAction", "/admin/incidents");
        return "admin/location-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Einsatzort nicht gefunden: " + id));

        model.addAttribute("location", incident);
        model.addAttribute("locationType", "INCIDENT");
        model.addAttribute("formAction", "/admin/incidents/" + id);
        return "admin/location-form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         @RequestParam(required = false) String description,
                         HttpSession session) {
        Incident incident = new Incident();
        incident.setName(name);
        incident.setLat(lat);
        incident.setLng(lng);
        incident.setDescription(description);
        incident.setActive(true);
        incidentRepository.save(incident);

        session.setAttribute("flashMessage", "Einsatzort '" + name + "' angelegt");
        return "redirect:/admin/incidents";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) Boolean active,
                         HttpSession session) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Einsatzort nicht gefunden: " + id));

        incident.setName(name);
        incident.setLat(lat);
        incident.setLng(lng);
        incident.setDescription(description);
        incident.setActive(active != null && active);
        incidentRepository.save(incident);

        session.setAttribute("flashMessage", "Einsatzort '" + name + "' aktualisiert");
        return "redirect:/admin/incidents";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, HttpSession session) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Einsatzort nicht gefunden: " + id));

        incident.setActive(!incident.isActive());
        incidentRepository.save(incident);

        String msg = incident.isActive() ? "Einsatzort aktiviert" : "Einsatzort deaktiviert";
        session.setAttribute("flashMessage", msg);
        return "redirect:/admin/incidents";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Einsatzort nicht gefunden: " + id));

        String name = incident.getName();
        incidentRepository.deleteById(id);

        session.setAttribute("flashMessage", "Einsatzort '" + name + "' geloscht");
        return "redirect:/admin/incidents";
    }
}
