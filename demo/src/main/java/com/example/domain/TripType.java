package com.example.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TripType {
    roadTrip("Road Trip"),
    businessTrip("Business Trip"),
    cityBreak("City Break"),
    allInclusive("All Inclusive"),
    cruise("Cruise"),
    groupTour("Group Tour"),
    camping("Camping"),
    beachHoliday("Beach Holiday"),
    hikingAdventure("Hiking Adventure"),
    girlsOrBoysTrip("Girls/Boys Trip"),
    familyVacation("Family Vacation"),
    skiTrip("Ski Trip");

    private final String displayName;

    TripType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static TripType fromDisplayName(String value) {
        for (TripType type : TripType.values()) {
            if (type.displayName.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown trip type: " + value);
    }
}
