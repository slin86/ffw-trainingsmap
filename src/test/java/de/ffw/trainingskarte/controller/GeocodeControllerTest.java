package de.ffw.trainingskarte.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeocodeControllerTest {

    private final GeocodeController controller = new GeocodeController();

    @Test
    void parseNominatimRealResponseSuccess() throws Exception {
        String json = "x{\"lat\":53.5921576,\"lng\":10.0823611,\"displayName\":\"133c, Stephanstraße, Hinschenfelde, Wandsbek, Hamburg, 22047, Deutschland\"}";

        GeocodeController.GeocodeResponse response = controller.parseNominatimResponse(json);

        assertNotNull(response, "Response should not be null: " + json);
        assertEquals(53.59216, response.lat(), "Lat should be correct: " + json);
        assertEquals(10.08236, response.lng(), "Lng should be correct: " + json);
    }

    @Test
    void parseNominatimResponseSuccess() throws Exception {
        String json = "[{\"lat\":\"53.5511\",\"lon\":\"9.9937\",\"display_name\":\"Rathausmarkt, 20095 Hamburg\"}]";

        GeocodeController.GeocodeResponse response = controller.parseNominatimResponse(json);

        assertNotNull(response, "Response should not be null: " + json);
    }

    @Test
    void parseNominatimResponseEmptyArrayReturnsNull() throws Exception {
        String json = "[]";

        GeocodeController.GeocodeResponse response = controller.parseNominatimResponse(json);

        assertNull(response);
    }

    @Test
    void parseNominatimResponseNoLatReturnsNull() throws Exception {
        String json = "[{\"lon\":\"9.9937\",\"display_name\":\"Rathausmarkt\"}]";

        GeocodeController.GeocodeResponse response = controller.parseNominatimResponse(json);

        assertNull(response);
    }
}
