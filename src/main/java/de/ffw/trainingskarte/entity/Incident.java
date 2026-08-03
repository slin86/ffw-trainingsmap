package de.ffw.trainingskarte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("INCIDENT")
public class Incident extends Location {

    @Column(nullable = false)
    private boolean active = true;

    @Override
    public String getLocationType() {
        return "INCIDENT";
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
