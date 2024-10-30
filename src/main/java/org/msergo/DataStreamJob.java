package org.msergo;

import org.apache.flink.api.common.eventtime.Watermark;
import org.apache.flink.api.common.eventtime.WatermarkGenerator;
import org.apache.flink.api.common.eventtime.WatermarkOutput;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.StateBackendOptions;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.msergo.models.StateVector;
import org.msergo.models.StateVectorWithAirportId;
import org.msergo.sources.RabbitMQCustomSource;
import org.msergo.transforms.AirportLookupAsyncFn;
import org.msergo.transforms.FirstAndLastCollectProcessor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DataStreamJob {

    public static void main(String[] args) throws Exception {
        ParameterTool config = ParameterTool.fromArgs(args);

        final long SESSION_TIMEOUT = 3 * 60 * 1000; // 3 minutes
        final long MAX_OUT_OF_ORDERNESS = 60 * 1000;

        final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
                .setHost(config.get("RABBITMQ_HOST"))
                .setVirtualHost("/")
                .setUserName(config.get("RABBITMQ_USER"))
                .setPassword(config.get("RABBITMQ_PASSWORD"))
                .setPort(5672)
                .setPrefetchCount(30_000)
                .build();

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(config);

        Configuration jobConfig = new Configuration();
        jobConfig.set(StateBackendOptions.STATE_BACKEND, "rocksdb");
        jobConfig.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");
        jobConfig.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "file:///flink-data");
        env.configure(jobConfig);

        final DataStream<StateVector> stream = env
                .addSource(new RabbitMQCustomSource(connectionConfig, "core-api", "flink.open-sky-vectors"))
                .setParallelism(1)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .forGenerator(context -> new WatermarkGenerator<StateVector>() {
                                    private long latestTimestamp = 0;

                                    @Override
                                    public void onEvent(StateVector stateVector, long eventTimestamp, WatermarkOutput watermarkOutput) {
                                        // Take the latest timestamp from the event only if it's in the past
                                        this.latestTimestamp = eventTimestamp;
                                    }

                                    @Override
                                    public void onPeriodicEmit(WatermarkOutput watermarkOutput) {
                                        if (latestTimestamp == 0) {
                                            return;
                                        }

                                        // Wait MAX_OUT_OF_ORDERNESS ms more to understand that the stream is idle
                                        long maxOutOfOrderness = SESSION_TIMEOUT + MAX_OUT_OF_ORDERNESS;
                                        long expectedTimestamp = this.latestTimestamp + maxOutOfOrderness;
                                        // If enough time already passed since the last event, advance the watermark and emit it
                                        if (expectedTimestamp < System.currentTimeMillis()) {
                                            System.err.println("Watermark: " + expectedTimestamp);
                                            latestTimestamp = 0;
                                            watermarkOutput.emitWatermark(new Watermark(expectedTimestamp));
                                        }
                                    }
                                })
                                .withTimestampAssigner((event, timestamp) -> {
                                    if (event.getLastContact() != null) {
                                        return (long) (event.getLastContact() * 1000);
                                    }

                                    return System.currentTimeMillis();
                                }));

        DataStream<StateVector> stateVectorsSessioned = stream
                .keyBy(StateVector::getIcao24)
                .window(EventTimeSessionWindows.withGap(Time.milliseconds(SESSION_TIMEOUT)))// Session window with SESSION_TIMEOUT-ms interval
                .process(new FirstAndLastCollectProcessor())
                .filter(Objects::nonNull);

        DataStream<StateVectorWithAirportId> stateVectorsWithAirportStream = AsyncDataStream.unorderedWait(
                stateVectorsSessioned,
                new AirportLookupAsyncFn(config.get("DB_URL"), config.get("DB_USER"), config.get("DB_PASSWORD")),
                10000,
                TimeUnit.MILLISECONDS
        );

        // Sink to PostgresSQL
        stateVectorsWithAirportStream
                .addSink(
                        JdbcSink.sink(
                                "insert into flight_data (icao24, callsign, category, origin_country, longitude, latitude, last_contact, airport_id) values (?, ?, ?, ?, ?, ?, ?, ?)",
                                (statement, stateVector) -> {
                                    statement.setString(1, stateVector.getIcao24());
                                    statement.setString(2, stateVector.getCallsign());
                                    statement.setString(3, stateVector.getSerials() != null ? stateVector.getSerials().toString() : null);
                                    statement.setString(4, stateVector.getOriginCountry());
                                    statement.setDouble(5, stateVector.getLongitude() != null ? stateVector.getLongitude() : 0);
                                    statement.setDouble(6, stateVector.getLatitude() != null ? stateVector.getLatitude() : 0);
                                    statement.setDouble(7, stateVector.getLastContact());
                                    statement.setDouble(8, stateVector.getAirportId() != null ? stateVector.getAirportId() : -1);
                                },
                                JdbcExecutionOptions.builder()
                                        .withBatchSize(10)
                                        .withBatchIntervalMs(2000)
                                        .withMaxRetries(5)
                                        .build(),
                                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                        .withUrl(config.get("DB_URL"))
                                        .withDriverName(config.get("DB_DRIVER"))
                                        .withUsername(config.get("DB_USER"))
                                        .withPassword(config.get("DB_PASSWORD"))
                                        .build()
                        ));

        env.execute("Collect landed flights");
    }
}
