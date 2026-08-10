package de.ffw.trainingskarte.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GeocodeController {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "ffw-trainingskarte/1.0 (privates Hobby-Projekt)";

    @GetMapping("/api/geocode")
    public ResponseEntity<GeocodeResponse> geocode(
            @RequestParam String address) {

        if (address == null || address.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address parameter is required");
        }

        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = String.format("%s?format=json&limit=1&viewbox=9.6,53.3,10.4,53.8&bounded=1&q=%s",
                    NOMINATIM_URL, encodedAddress);

            try (HttpClient client = HttpClient.newHttpClient()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    GeocodeResponse result = parseNominatimResponse(response.body());

                    if (result != null) {
                        return ResponseEntity.ok(result);
                    } else {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No address found for: " + address);
                    }
                } else if (response.statusCode() == 429) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
                } else {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Geocoding service unavailable");
                }
            }

        } catch (HttpTimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Geocoding service timeout");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to geocode address: " + e.getMessage());
        }
    }

    GeocodeResponse parseNominatimResponse(String json) throws Exception {
        String trimmed = json.trim();

        int firstBracket = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '[' || c == '{') {
                firstBracket = i;
                break;
            }
        }

        if (firstBracket < 0) {
            return null;
        }

        String bracketedContent = trimmed.substring(firstBracket);

        int braceCount = 0;
        int endIdx = -1;
        for (int i = 0; i < bracketedContent.length(); i++) {
            char c = bracketedContent.charAt(i);
            if (c == '[' || c == '{') braceCount++;
            else if (c == ']' || c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIdx = i;
                    break;
                }
            }
        }

        if (endIdx < 0 || braceCount != 0) {
            return null;
        }

        String firstObject = bracketedContent.substring(0, endIdx + 1).trim();

        double lat = extractDouble(firstObject, "\"lat\"");
        double lng = extractDouble(firstObject, "\"lng\"", "\"lon\"");
        String displayName = extractString(firstObject, "\"displayName\"", "\"display_name\"");

        if (Double.isNaN(lat) || Double.isNaN(lng) || displayName == null) {
            return null;
        }

        return new GeocodeResponse(Math.round(lat * 100000.0) / 100000.0, Math.round(lng * 100000.0) / 100000.0, displayName);
    }

    double extractDouble(String json, String key1, String... extraKeys) throws Exception {
        int idx = json.indexOf(key1);
        for (String key : extraKeys) {
            if (idx < 0) {
                idx = json.indexOf(key);
            }
        }
        if (idx < 0) {
            return Double.NaN;
        }

        String afterKey = json.substring(idx + key1.length());
        afterKey = afterKey.trim();

        int colonIdx = afterKey.indexOf(':');
        if (colonIdx < 0) {
            return Double.NaN;
        }

        String valueStr = afterKey.substring(colonIdx + 1).trim();

        if (valueStr.startsWith("\"")) {
            int endQuote = -1;
            for (int i = 1; i < valueStr.length(); i++) {
                char c = valueStr.charAt(i);
                if (c == '"' && valueStr.charAt(i - 1) != '\\') {
                    endQuote = i;
                    break;
                }
            }

            if (endQuote < 0) {
                return Double.NaN;
            }

            try {
                return Double.parseDouble(valueStr.substring(1, endQuote));
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        } else {
            int commaIdx = valueStr.indexOf(',');
            int braceIdx = valueStr.indexOf('}');

            int endIdxVal = -1;
            if (commaIdx >= 0 && braceIdx >= 0) {
                endIdxVal = Math.min(commaIdx, braceIdx);
            } else if (commaIdx >= 0) {
                endIdxVal = commaIdx;
            } else if (braceIdx >= 0) {
                endIdxVal = braceIdx;
            } else {
                endIdxVal = valueStr.length();
            }

            String numStr = valueStr.substring(0, endIdxVal).trim();
            try {
                return Double.parseDouble(numStr);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
    }

    String extractString(String json, String key1, String... extraKeys) throws Exception {
        int idx = json.indexOf(key1);
        for (String key : extraKeys) {
            if (idx < 0) {
                idx = json.indexOf(key);
            }
        }
        if (idx < 0) {
            return null;
        }

        String afterKey = json.substring(idx + key1.length());
        afterKey = afterKey.trim();

        int colonIdx = afterKey.indexOf(':');
        if (colonIdx < 0) {
            return null;
        }

        String valueStr = afterKey.substring(colonIdx + 1).trim();

        if (!valueStr.startsWith("\"")) {
            return null;
        }

        int endQuote = -1;
        for (int i = 1; i < valueStr.length(); i++) {
            char c = valueStr.charAt(i);
            if (c == '"' && valueStr.charAt(i - 1) != '\\') {
                endQuote = i;
                break;
            }
        }

        if (endQuote < 0) {
            return null;
        }

        return valueStr.substring(1, endQuote);
    }

    public record GeocodeResponse(double lat, double lng, String displayName) {

    }
}
