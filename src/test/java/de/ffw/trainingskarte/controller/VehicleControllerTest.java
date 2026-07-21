package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.VehicleRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class VehicleControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    private MockMvc mockMvc;

    private final Vehicle testVehicle = new Vehicle("TEST 1", "TLF 3000", 1, 53.55, 9.99);

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsOk() throws Exception {
        when(vehicleRepository.findAll()).thenReturn(java.util.List.of(testVehicle));

        mockMvc.perform(get("/api/vehicles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].callsign").value("TEST 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"callsign\":\"NEW 1\",\"type\":\"HLF 20\",\"status\":1}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsCreated() throws Exception {
        when(vehicleRepository.findByCallsign("NEW 1")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/vehicles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"callsign\":\"NEW 1\",\"type\":\"HLF 20\",\"status\":1}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePositionWithInvalidLatReturnsBadRequest() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));

        mockMvc.perform(put("/api/vehicles/1/position")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":54.0,\"lng\":9.99}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePositionWithInvalidLngReturnsBadRequest() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));

        mockMvc.perform(put("/api/vehicles/1/position")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":53.55,\"lng\":11.0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePositionValidRangeWorks() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/api/vehicles/1/position")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":53.55,\"lng\":9.99}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/vehicles/1")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateAsViewerReturnsForbidden() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));

        mockMvc.perform(put("/api/vehicles/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"callsign\":\"NEW 1\",\"type\":\"HLF 20\",\"status\":1}"))
            .andExpect(status().isForbidden());
    }
}
