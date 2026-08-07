package de.ffw.trainingskarte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.IncidentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

@WebMvcTest(AdminIncidentController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class AdminIncidentControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private IncidentRepository incidentRepository;

    private MockMvc mockMvc;

    private final Incident testIncident = new Incident();
    {
        testIncident.setId(1L);
        testIncident.setName("Einsatzort 1");
        testIncident.setLat(53.55);
        testIncident.setLng(9.99);
        testIncident.setActive(true);
        testIncident.setDescription("Test description");
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/incidents"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAsAdminReturnsOk() throws Exception {
        when(incidentRepository.findByActiveTrue()).thenReturn(java.util.List.of(testIncident));

        mockMvc.perform(get("/admin/incidents"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/incidents"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAllAsAdminReturnsOk() throws Exception {
        when(incidentRepository.findAll()).thenReturn(java.util.List.of(testIncident));

        mockMvc.perform(get("/admin/incidents?all=true"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/incidents"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newIncidentReturnsOk() throws Exception {
        mockMvc.perform(get("/admin/incidents/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/incident-form"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editIncidentReturnsOk() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));

        mockMvc.perform(get("/admin/incidents/1/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/incident-form"))
            .andExpect(model().attribute("incident", testIncident));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editIncidentNotFoundReturns404() throws Exception {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/incidents/999/edit"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsRedirect() throws Exception {
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/incidents")
                .with(csrf())
                .param("name", "Einsatzort")
                .param("lat", "53.5")
                .param("lng", "9.9")
                .param("description", "Test description"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/incidents")
                .with(csrf())
                .param("name", "Einsatzort")
                .param("lat", "53.5")
                .param("lng", "9.9")
                .param("description", "Test description"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminReturnsRedirect() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/incidents/1")
                .with(csrf())
                .param("name", "Neuer Name")
                .param("lat", "53.6")
                .param("lng", "10.0")
                .param("description", "Updated description")
                .param("active", "false"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/incidents/1")
                .with(csrf())
                .param("name", "Neuer Name")
                .param("lat", "53.6")
                .param("lng", "10.0")
                .param("description", "Updated description")
                .param("active", "false"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleAsAdminReturnsRedirect() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/incidents/1/toggle")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void toggleAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/incidents/1/toggle")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsRedirect() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));

        mockMvc.perform(post("/admin/incidents/1/delete")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNotFoundReturns404() throws Exception {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/incidents/999/delete")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/incidents/1/delete")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }
}
