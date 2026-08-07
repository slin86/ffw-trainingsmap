package de.ffw.trainingskarte.controller.dto;

public record LocationRequest(String type, String description, String name, double lat, double lng, Boolean active) {
}
