package de.ffw.trainingskarte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.ffw.trainingskarte.entity.Incident;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.IncidentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

@WebMvcTest(IncidentController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class IncidentControllerTest {

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
        testIncident.setDescription("Beschreibung 1");
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsOkWithActiveOnly() throws Exception {
        when(incidentRepository.findByActiveTrue()).thenReturn(List.of(testIncident));

        mockMvc.perform(get("/api/incidents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Einsatzort 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsAllWhenAllTrue() throws Exception {
        when(incidentRepository.findAll()).thenReturn(List.of(testIncident));

        mockMvc.perform(get("/api/incidents?all=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Einsatzort 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getByIdAsViewerReturnsOk() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));

        mockMvc.perform(get("/api/incidents/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Einsatzort 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/incidents")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Einsatzort\",\"lat\":53.5,\"lng\":9.9}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsCreated() throws Exception {
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/incidents")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Einsatzort\",\"lat\":53.5,\"lng\":9.9}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminWithDescriptionReturnsCreated() throws Exception {
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/incidents")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Einsatzort\",\"lat\":53.5,\"lng\":9.9,\"description\":\"Beschreibung\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminReturnsOk() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/incidents/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Neuer Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminWithDescriptionReturnsOk() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/incidents/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0,\"description\":\"Updated Beschreibung\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Neuer Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNotFoundReturns404() throws Exception {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/incidents/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/incidents/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void setActiveAsAdminReturnsOk() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(patch("/api/incidents/1/active")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void setActiveMissingFieldReturnsBadRequest() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));

        mockMvc.perform(patch("/api/incidents/1/active")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"foo\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Missing 'active' field in request body"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void setActiveAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/incidents/1/active")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/incidents/1")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsNoContent() throws Exception {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(testIncident));

        mockMvc.perform(delete("/api/incidents/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
