package de.ffw.trainingskarte.seeder;

import de.ffw.trainingskarte.entity.AppUser;
import de.ffw.trainingskarte.entity.Vehicle;
import de.ffw.trainingskarte.repository.AppUserRepository;
import de.ffw.trainingskarte.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Profile("dev")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final VehicleRepository vehicleRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(VehicleRepository vehicleRepository, AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.vehicleRepository = vehicleRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            log.info("Seedete Daten fuer Entwicklungsumgebung...");

            if (appUserRepository.findByUsername("admin").isEmpty()) {
                AppUser admin = new AppUser(
                    "admin",
                    passwordEncoder.encode("admin"),
                    "ADMIN",
                    true
                );
                appUserRepository.save(admin);
                log.info("Admin-Nutzer angelegt.");
            }

            if (vehicleRepository.count() == 0) {
                vehicleRepository.save(new Vehicle("DLA 1", "Drehleiter", 2, 53.587, 10.044));
                vehicleRepository.save(new Vehicle("CT 2/1", "C-Tragkraftspritzenlohnwagen", 2, 53.594, 9.990));
                vehicleRepository.save(new Vehicle("TGL-B 4", "Tankgruppenlöschgruppenfahrzeug", 2, 53.552, 9.935));
                vehicleRepository.save(new Vehicle("GW 7", "Gerätewagen", 1, 53.460, 9.983));
                log.info("4 Fahrzeuge fuer Hamburger Feuerwachen angelegt.");
            }
        };
    }
}
