package de.ffw.trainingskarte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.entity.VehicleCheckin;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.VehicleCheckinRepository;
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

@WebMvcTest(CheckInController.class)
@Import(de.ffw.trainingskarte.config.SecurityConfig.class)
class CheckInControllerTest {

    @Autowired
    private DefaultMockMvcBuilder mockMvcBuilder;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    @MockitoBean
    private VehicleCheckinRepository vehicleCheckinRepository;

    @MockitoBean
    private AppUserRepository appUserRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = mockMvcBuilder.apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void checkinAsViewerReturnsCreated() throws Exception {
        Vehicle vehicle = new Vehicle("TEST 1", "TLF 3000", 1, 53.55, 9.99);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleCheckinRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(vehicleCheckinRepository.save(any(VehicleCheckin.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/vehicles/1/checkin")
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.vehicleId").value(1));
    }

    @Test
    void checkinWithoutAuthRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/api/vehicles/1/checkin")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void checkinForNonExistentVehicleReturnsNotFound() throws Exception {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/vehicles/999/checkin")
                .with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void checkinWithPreviousCheckinDeletesOldAndCreatesNew() throws Exception {
        Vehicle vehicle = new Vehicle("TEST 1", "TLF 3000", 1, 53.55, 9.99);
        vehicle.setId(1L);

        Vehicle oldVehicle = new Vehicle("OLD 1", "HLF 20", 1, 53.56, 9.98);
        oldVehicle.setId(2L);

        VehicleCheckin oldCheckin = new VehicleCheckin();
        oldCheckin.setVehicle(oldVehicle);
        oldCheckin.setUsername("user");
        oldCheckin.setCheckedInAt(OffsetDateTime.now());

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleCheckinRepository.findByUsername("user")).thenReturn(Optional.of(oldCheckin));
        when(vehicleCheckinRepository.save(any(VehicleCheckin.class))).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/vehicles/1/checkin")
                .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.vehicleId").value(1));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void checkoutAsViewerReturnsNoContent() throws Exception {
        Vehicle vehicle = new Vehicle("TEST 1", "TLF 3000", 1, 53.55, 9.99);
        vehicle.setId(1L);

        VehicleCheckin checkin = new VehicleCheckin();
        checkin.setVehicle(vehicle);
        checkin.setUsername("user");
        checkin.setCheckedInAt(OffsetDateTime.now());

        when(vehicleCheckinRepository.findByUsername("user")).thenReturn(Optional.of(checkin));

        mockMvc.perform(post("/api/vehicles/1/checkout")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    void checkoutWithoutAuthRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/api/vehicles/1/checkout")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void checkoutForNonExistentVehicleStillReturnsNoContent() throws Exception {
        Vehicle vehicle = new Vehicle("TEST 1", "TLF 3000", 1, 53.55, 9.99);
        vehicle.setId(2L);

        VehicleCheckin checkin = new VehicleCheckin();
        checkin.setVehicle(vehicle);
        checkin.setUsername("user");
        checkin.setCheckedInAt(OffsetDateTime.now());

        when(vehicleCheckinRepository.findByUsername("user")).thenReturn(Optional.of(checkin));

        mockMvc.perform(post("/api/vehicles/1/checkout")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getMyCheckinReturnsVehicleIdWhenCheckedIn() throws Exception {
        Vehicle vehicle = new Vehicle("TEST 1", "TLF 3000", 1, 53.55, 9.99);
        vehicle.setId(1L);

        VehicleCheckin checkin = new VehicleCheckin();
        checkin.setVehicle(vehicle);
        checkin.setUsername("user");
        checkin.setCheckedInAt(OffsetDateTime.now());

        when(vehicleCheckinRepository.findByUsername("user")).thenReturn(Optional.of(checkin));

        mockMvc.perform(get("/api/checkin/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vehicleId").value(1));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getMyCheckinReturnsNoContentWhenNotCheckedIn() throws Exception {
        when(vehicleCheckinRepository.findByUsername("user")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/checkin/me"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getMyCheckinWithoutAuthRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/api/checkin/me"))
            .andExpect(status().is3xxRedirection());
    }
}
