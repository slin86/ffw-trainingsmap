package de.ffw.trainingskarte.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("STATION")
public class Station extends Location {

}
