package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.AppUser;
import de.ffw.trainingskarte.repository.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    static final String[] ROLES = new String[]{"ADMIN", "VIEWER"};

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String list(Model model, HttpSession session, Authentication authentication) {
        List<AppUser> users = appUserRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("roles", ROLES);
        model.addAttribute("currentUsername", authentication.getName());
        String flashMsg = (String) session.getAttribute("flashMessage");
        if (flashMsg != null) {
            model.addAttribute("flashMessage", flashMsg);
            session.removeAttribute("flashMessage");
        }
        return "admin/users";
    }

    @PostMapping("/create")
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String role,
                         HttpSession session) {
        boolean exists = appUserRepository.findByUsername(username).isPresent();
        if (exists) {
            session.setAttribute("flashError", "Benutzername '" + username + "' existiert bereits");
            return "redirect:/admin/users";
        }
        AppUser user = new AppUser(username, passwordEncoder.encode(password), role, true);
        appUserRepository.save(user);
        session.setAttribute("flashMessage", "Nutzer '" + username + "' angelegt");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id,
                         HttpSession session,
                         Authentication authentication) {
        AppUser user = appUserRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Nutzer nicht gefunden: " + id));

        if (user.getUsername().equals(authentication.getName())) {
            session.setAttribute("flashError", "Sie konnen Ihr eigenes Konto nicht deaktivieren");
            return "redirect:/admin/users";
        }

        user.setEnabled(!user.isEnabled());
        appUserRepository.save(user);
        String status = user.isEnabled() ? "aktiviert" : "deaktiviert";
        session.setAttribute("flashMessage", "Nutzer '" + user.getUsername() + "'" + status);
        return "redirect:/admin/users";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         HttpSession session,
                         Authentication authentication) {
        AppUser user = appUserRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Nutzer nicht gefunden: " + id));

        if (user.getUsername().equals(authentication.getName())) {
            session.setAttribute("flashError", "Sie konnen Ihr eigenes Konto nicht loeschen");
            return "redirect:/admin/users";
        }

        appUserRepository.deleteById(id);
        session.setAttribute("flashMessage", "Nutzer '" + user.getUsername() + "' geloscht");
        return "redirect:/admin/users";
    }
}
