package org.msergo.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;

/**
 * Represents states of vehicles at a given time.
 *
 * @author Markus Fuchs, fuchs@opensky-network.org
 */
@JsonDeserialize(using = StateVectorResponseDeserializer.class)
public class StateVectorsResponse {
    /**
     * -- GETTER --
     *
     * @return The point in time for which states are stored
     */
    @Setter
    @Getter
    private int time;
    private Collection<StateVector> flightStates;

    /**
     * @return Actual states for this point in time
     */
    public Collection<StateVector> getStates() {
        if (flightStates == null || flightStates.isEmpty()) return Collections.emptyList();
        return this.flightStates;
    }

    public void setStates(Collection<StateVector> states) {
        this.flightStates = states;
    }
}