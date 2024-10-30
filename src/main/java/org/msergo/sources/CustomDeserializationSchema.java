package org.msergo.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.rabbitmq.RMQDeserializationSchema;
import org.msergo.models.StateVector;
import org.msergo.models.StateVectorsResponse;

import java.io.IOException;

public class CustomDeserializationSchema implements RMQDeserializationSchema<StateVector> {
    @Override
    public void deserialize(Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes, RMQCollector<StateVector> rmqCollector) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StateVectorsResponse stateVectorsResponse = mapper.readValue(bytes, StateVectorsResponse.class);

        for (StateVector stateVector : stateVectorsResponse.getStates()) {
            rmqCollector.collect(stateVector);
        }
    }

    @Override
    public boolean isEndOfStream(StateVector nextElement) {
        return false;
    }

    @Override
    public TypeInformation<StateVector> getProducedType() {
        return TypeInformation.of(StateVector.class);
    }
}
