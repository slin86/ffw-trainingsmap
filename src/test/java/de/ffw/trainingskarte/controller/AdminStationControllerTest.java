package de.ffw.trainingskarte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.ffw.trainingskarte.entity.Station;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.LocationRepository;
import de.ffw.trainingskarte.repository.StationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

@WebMvcTest(AdminStationController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class AdminStationControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private StationRepository stationRepository;

    @MockitoBean
    private LocationRepository<?> locationRepository;

    private MockMvc mockMvc;

    private final Station testStation = new Station();
    {
        testStation.setId(1L);
        testStation.setName("Feuerwache 1");
        testStation.setLat(53.55);
        testStation.setLng(9.99);
        testStation.setDescription("Beschreibung 1");
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/stations"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAsAdminReturnsOk() throws Exception {
        when(stationRepository.findAll()).thenReturn(java.util.List.of(testStation));

        mockMvc.perform(get("/admin/stations"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/stations"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newFormAsAdminReturnsOk() throws Exception {
        mockMvc.perform(get("/admin/stations/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/station-form"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editFormAsAdminReturnsOk() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        mockMvc.perform(get("/admin/stations/1/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/station-form"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editFormNotFoundReturns404() throws Exception {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/stations/999/edit"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void editFormAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/stations/1/edit"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsRedirect() throws Exception {
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/stations")
                .with(csrf())
                .param("name", "Feuerwache")
                .param("lat", "53.5")
                .param("lng", "9.9"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminWithDescriptionReturnsRedirect() throws Exception {
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/stations")
                .with(csrf())
                .param("name", "Feuerwache")
                .param("lat", "53.5")
                .param("lng", "9.9")
                .param("description", "Beschreibung"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/stations")
                .with(csrf())
                .param("name", "Feuerwache")
                .param("lat", "53.5")
                .param("lng", "9.9"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminReturnsRedirect() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/stations/1")
                .with(csrf())
                .param("name", "Neuer Name")
                .param("lat", "53.6")
                .param("lng", "10.0"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminWithDescriptionReturnsRedirect() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/stations/1")
                .with(csrf())
                .param("name", "Neuer Name")
                .param("lat", "53.6")
                .param("lng", "10.0")
                .param("description", "Updated Beschreibung"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNotFoundReturns404() throws Exception {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/stations/999")
                .with(csrf())
                .param("name", "Neuer Name")
                .param("lat", "53.6")
                .param("lng", "10.0"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/stations/1")
                .with(csrf())
                .param("name", "Neuer Name")
                .param("lat", "53.6")
                .param("lng", "10.0"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsRedirect() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        mockMvc.perform(post("/admin/stations/1/delete")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNotFoundReturns404() throws Exception {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/stations/999/delete")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/stations/1/delete")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }
}
