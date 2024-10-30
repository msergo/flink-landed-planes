package org.msergo.transforms;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.msergo.models.StateVector;
import org.msergo.models.StateVectorWithAirportId;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class AirportLookupAsyncFn extends RichAsyncFunction<StateVector, StateVectorWithAirportId> {
    private final String jdbcUser;
    private final String jdbcPassword;
    private final String jdbcUrl;
    // TODO: Use PG geographic functions to find the nearest airport

    private transient Connection connection;

    public AirportLookupAsyncFn(String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }

    public void close() throws Exception {
        connection.close();
    }

    @Override
    public void asyncInvoke(StateVector stateVector, ResultFuture<StateVectorWithAirportId> resultFuture) throws Exception {
        String sql = "SELECT id FROM airports a WHERE TRUNC(a.latitude::numeric, 1) = TRUNC(?::numeric, 1) AND TRUNC(a.longitude::numeric, 1) = TRUNC(?::numeric, 1) LIMIT 1;";
        StateVectorWithAirportId stateVectorWithAirportId = new StateVectorWithAirportId(stateVector, null);
        PreparedStatement statement = connection.prepareStatement(sql);

        if (stateVector.getLatitude() == null || stateVector.getLongitude() == null) {
            resultFuture.complete(Collections.singleton(stateVectorWithAirportId));

            return;
        }

        statement.setDouble(1, stateVector.getLatitude());
        statement.setDouble(2, stateVector.getLongitude());

        ResultSet resultSet = statement.executeQuery();
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            try {
                if (resultSet.next()) {
                    Double airportId = resultSet.getDouble("id");
                    stateVectorWithAirportId.setAirportId(airportId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Collections.singleton(stateVectorWithAirportId);
        }).thenAccept(resultFuture::complete);

        future.get();
    }
}
