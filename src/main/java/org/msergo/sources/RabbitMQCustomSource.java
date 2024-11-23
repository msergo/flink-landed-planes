package org.msergo.sources;

import com.rabbitmq.client.AMQP;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.msergo.models.StateVector;

import java.io.IOException;
import java.util.Map;

public class RabbitMQCustomSource extends RMQSource<StateVector> {
    private final String exchangeName;

    public RabbitMQCustomSource(RMQConnectionConfig rmqConnectionConfig, String exchangeName, String queueName) {
        super(rmqConnectionConfig, queueName, new CustomDeserializationSchema());

        this.exchangeName = exchangeName;
    }

    @Override
    protected void setupQueue() throws IOException {
        channel.exchangeDeclare(this.exchangeName, "topic", true);
        Map<String, Object> arguments = new java.util.HashMap<>();
        arguments.put("x-message-ttl", 3600000); // 1 hour

        AMQP.Queue.DeclareOk queueDeclareOk = channel.queueDeclare(this.queueName, true, false, false, arguments);
        channel.queueBind(queueDeclareOk.getQueue(), this.exchangeName, "state-vectors.#");
    }
}
