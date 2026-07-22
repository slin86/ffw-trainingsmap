package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.VehicleRepository;
import java.time.OffsetDateTime;
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

@WebMvcTest(AdminVehicleController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class AdminVehicleControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    private MockMvc mockMvc;

    private final Vehicle testVehicle = new Vehicle("C1-TG", "TLF", 2, 53.5511, 9.9937);

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/vehicles"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/vehicles/create")
                .with(csrf())
                .param("callsign", "NEW-1")
                .param("type", "HLF")
                .param("status", "1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void editAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/vehicles/1/edit"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void updateAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/vehicles/1")
                .with(csrf())
                .param("callsign", "C1-TG")
                .param("type", "TLF")
                .param("status", "2")
                .param("lat", "53.5511")
                .param("lng", "9.9937"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteAsViewerReturnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/vehicles/1/delete")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAsAdminReturnsOk() throws Exception {
        when(vehicleRepository.findAll()).thenReturn(java.util.List.of(testVehicle));

        mockMvc.perform(get("/admin/vehicles"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/vehicles"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsAdminReturnsRedirect() throws Exception {
        when(vehicleRepository.findByCallsign("NEW-1")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/vehicles/create")
                .with(csrf())
                .param("callsign", "NEW-1")
                .param("type", "HLF")
                .param("status", "1"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDuplicateCallsignReturnsRedirectWithError() throws Exception {
        when(vehicleRepository.findByCallsign("C1-TG")).thenReturn(Optional.of(testVehicle));

        mockMvc.perform(post("/admin/vehicles/create")
                .with(csrf())
                .param("callsign", "C1-TG")
                .param("type", "HLF")
                .param("status", "1"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editAsAdminReturnsOk() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));

        mockMvc.perform(get("/admin/vehicles/1/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/vehicle-edit"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAsAdminReturnsRedirect() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(vehicleRepository.findByCallsign("C1-TG-UPD")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/admin/vehicles/1")
                .with(csrf())
                .param("callsign", "C1-TG-UPD")
                .param("type", "TLF")
                .param("status", "2")
                .param("lat", "53.5511")
                .param("lng", "9.9937"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAsAdminReturnsRedirect() throws Exception {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));

        mockMvc.perform(post("/admin/vehicles/1/delete")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
