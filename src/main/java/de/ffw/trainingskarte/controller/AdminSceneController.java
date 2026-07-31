package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Scene;
import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.VehicleRepository;
import de.ffw.trainingskarte.service.SceneService;
import jakarta.persistence.EntityNotFoundException;
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
@RequestMapping("/admin/scenes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSceneController {

    private final SceneService sceneService;
    private final VehicleRepository vehicleRepository;

    public AdminSceneController(SceneService sceneService, VehicleRepository vehicleRepository) {
        this.sceneService = sceneService;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public String list(Model model, HttpSession session) {
        List<Scene> scenes = sceneService.findAll();
        model.addAttribute("scenes", scenes);
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
        return "admin/scenes";
    }

    @PostMapping("/create")
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         @RequestParam(required = false) Long vehicleId,
                         HttpSession session) {
        try {
            var vid = (vehicleId != null && vehicleId > 0L) ? vehicleId : null;
            sceneService.create(title, description == null || description.isBlank() ? null : description, lat, lng, vid);
            session.setAttribute("flashMessage", "Einsatzszenario '" + title + "' angelegt");
        } catch (RuntimeException e) {
            session.setAttribute("flashError", e.getMessage());
            return "redirect:/admin/scenes";
        }
        return "redirect:/admin/scenes";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, HttpSession session) {
        Scene scene;
        try {
            scene = sceneService.findById(id);
        } catch (EntityNotFoundException e) {
            return "redirect:/admin/scenes";
        }

        List<Vehicle> vehicles = vehicleRepository.findAll();
        model.addAttribute("scene", scene);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("editMode", true);

        String flashMsg = (String) session.getAttribute("flashMessage");
        if (flashMsg != null) {
            model.addAttribute("flashError", flashMsg);
            session.removeAttribute("flashMessage");
        }

        return "admin/scene-edit";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        model.addAttribute("scene", null);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("editMode", false);
        model.addAttribute("defaultLat", 53.5511);
        model.addAttribute("defaultLng", 9.9937);
        return "admin/scene-edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam double lat,
                         @RequestParam double lng,
                         @RequestParam(required = false) Long vehicleId,
                         HttpSession session) {
        try {
            var vid = (vehicleId != null && vehicleId > 0L) ? vehicleId : null;
            sceneService.update(id, title, description == null || description.isBlank() ? null : description, lat, lng, vid);
            session.setAttribute("flashMessage", "Einsatzszenario '" + title + "' aktualisiert");
        } catch (RuntimeException e) {
            session.setAttribute("flashError", e.getMessage());
            return "redirect:/admin/scenes";
        }
        return "redirect:/admin/scenes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        Scene scene;
        try {
            scene = sceneService.findById(id);
        } catch (RuntimeException e) {
            session.setAttribute("flashError", "Szenario nicht gefunden");
            return "redirect:/admin/scenes";
        }
        String title = scene.getTitle();
        try {
            sceneService.delete(id);
        } catch (RuntimeException e) {
            session.setAttribute("flashError", e.getMessage());
            return "redirect:/admin/scenes";
        }
        session.setAttribute("flashMessage", "Einsatzszenario '" + title + "' geloscht");
        return "redirect:/admin/scenes";
    }
}
