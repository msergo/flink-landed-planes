package org.msergo.transforms;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.msergo.models.StateVector;
import org.msergo.models.StateVectorsResponse;

import java.util.Collection;

/*
    Deprecated class that is no longer used in the project.
    The main purpose of this class was to flatten the StateVectorsResponse object into a collection of StateVector objects.
    Currently implemented in the RabbitMQCustomSource class.
 */
public class StateVectorsFlatMapper implements FlatMapFunction<StateVectorsResponse, StateVector> {
    @Override
    public void flatMap(StateVectorsResponse stateVectorsResponse, Collector<StateVector> out) {
        Collection<StateVector> stateVectors = stateVectorsResponse.getStates();

        for (StateVector stateVector : stateVectors) {
            out.collect(stateVector);
        }
    }
}
