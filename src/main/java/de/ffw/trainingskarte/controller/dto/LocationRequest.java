package de.ffw.trainingskarte.controller.dto;

public record LocationRequest(String type, String name, double lat, double lng, Boolean active, String description) {
}
