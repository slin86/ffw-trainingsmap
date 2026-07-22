package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.AppUser;
import de.ffw.trainingskarte.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private final AppUser testUser = new AppUser("viewer", "$2a$encoded", "VIEWER", true);

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/users/create")
                .with(csrf())
                .param("username", "newuser")
                .param("password", "pass123")
                .param("role", "VIEWER"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void toggleAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/users/2/toggle")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/admin/users/2")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAsAdminReturnsOk() throws Exception {
        when(appUserRepository.findAll()).thenReturn(java.util.List.of(testUser));

        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/users"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createAsAdminReturnsRedirect() throws Exception {
        when(appUserRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(eq("pass123"))).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/users/create")
                .with(csrf())
                .param("username", "newuser")
                .param("password", "pass123")
                .param("role", "VIEWER"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createDuplicateUsernameReturnsRedirect() throws Exception {
        when(appUserRepository.findByUsername("newuser")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/admin/users/create")
                .with(csrf())
                .param("username", "newuser")
                .param("password", "pass123")
                .param("role", "VIEWER"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void toggleAsAdminReturnsRedirect() throws Exception {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/users/2/toggle")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void toggleSelfAsViewerReturnsForbidden() throws Exception {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/admin/users/2/toggle")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "ADMIN")
    void toggleSelfReturnsRedirectWithError() throws Exception {
        AppUser selfUser = new AppUser("viewer", "$2a$encoded", "ADMIN", true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(selfUser));

        mockMvc.perform(post("/admin/users/2/toggle")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteAsAdminReturnsRedirect() throws Exception {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(delete("/admin/users/2")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "ADMIN")
    void deleteSelfReturnsRedirectWithError() throws Exception {
        AppUser selfUser = new AppUser("viewer", "$2a$encoded", "ADMIN", true);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(selfUser));

        mockMvc.perform(delete("/admin/users/2")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
