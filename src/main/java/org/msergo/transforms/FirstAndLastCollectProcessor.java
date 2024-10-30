package org.msergo.transforms;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.msergo.models.StateVector;

public class FirstAndLastCollectProcessor extends ProcessWindowFunction<StateVector, StateVector, String, TimeWindow> {
    @Override
    public void process(String s, ProcessWindowFunction<StateVector, StateVector, String, TimeWindow>.Context context, Iterable<StateVector> iterable, Collector<StateVector> collector) throws Exception {
        StateVector first = null;
        StateVector second = null;
        System.err.println("iterable: " + iterable);

        for (StateVector stateVector : iterable) {
            if (first == null) {
                if (stateVector.isOnGround()) {
                    continue;
                }

                first = stateVector;
                continue;
            }

            if (stateVector.isOnGround()) {
                second = stateVector;

                break;
            }
        }

        if (first != null && second != null) {
            collector.collect(second);
        }
    }
}