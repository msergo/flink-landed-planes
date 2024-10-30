package org.msergo.models;

import lombok.Getter;
import lombok.Setter;

public class StateVectorWithAirportId extends StateVector {
    @Getter
    @Setter
    private Double airportId;

    public StateVectorWithAirportId(String icao24) {
        super(icao24);
    }

    public StateVectorWithAirportId(StateVector stateVector, Double airportId) {
        super(stateVector.getIcao24());
        this.setCallsign(stateVector.getCallsign());
        this.setOriginCountry(stateVector.getOriginCountry());
        this.setLastPositionUpdate(stateVector.getLastPositionUpdate());
        this.setLastContact(stateVector.getLastContact());
        this.setLongitude(stateVector.getLongitude());
        this.setLatitude(stateVector.getLatitude());
        this.setBaroAltitude(stateVector.getBaroAltitude());
        this.setOnGround(stateVector.isOnGround());
        this.setVelocity(stateVector.getVelocity());
        this.setHeading(stateVector.getHeading());
        this.setVerticalRate(stateVector.getVerticalRate());
        this.setAirportId(airportId);
    }
}