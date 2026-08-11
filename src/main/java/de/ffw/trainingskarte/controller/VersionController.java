package de.ffw.trainingskarte.controller;

import de.ffw.trainingskarte.controller.dto.VersionResponse;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    private String version;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/version.txt")) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                version = new String(bytes).trim();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read version.txt", e);
        }
    }

    @GetMapping
    public VersionResponse getVersion() {
        return new VersionResponse(version);
    }
}
