package de.ffw.trainingskarte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import de.ffw.trainingskarte.entity.Location;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;

@WebMvcTest(StationController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class StationControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private StationRepository stationRepository;

    @MockitoBean
    private LocationRepository<Location> locationRepository;

    private MockMvc mockMvc;

    private final Station testStation = new Station();
    {
        testStation.setId(1L);
        testStation.setName("Feuerwache 1");
        testStation.setLat(53.55);
        testStation.setLng(9.99);
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsOk() throws Exception {
        when(stationRepository.findAll()).thenReturn(java.util.List.of(testStation));

        mockMvc.perform(get("/api/stations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Feuerwache 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getByIdAsViewerReturnsOk() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        mockMvc.perform(get("/api/stations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Feuerwache 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getByIdNotFoundReturns404() throws Exception {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/stations/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/stations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Feuerwache\",\"lat\":53.5,\"lng\":9.9}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsCreated() throws Exception {
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/stations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Feuerwache\",\"lat\":53.5,\"lng\":9.9}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminWithDescriptionReturnsCreated() throws Exception {
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/stations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Feuerwache\",\"lat\":53.5,\"lng\":9.9,\"description\":\"Beschreibungstext\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminReturnsOk() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/stations/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Neuer Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminWithDescriptionReturnsOk() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(stationRepository.save(any(Station.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/stations/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0,\"description\":\"Beschreibungstext\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Neuer Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateNotFoundReturns404() throws Exception {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/stations/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/stations/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Neuer Name\",\"lat\":53.6,\"lng\":10.0}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/stations/1")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsNoContent() throws Exception {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        mockMvc.perform(delete("/api/stations/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
