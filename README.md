# A Flink job that reads OpenSky flight states from RabbitMQ and writes to a DB when a plane is landed.

This project is an experiment to collect states when a specific moment happens, in this case, when a plane is landed. So
transition from "IN AIR" to "LANDED" is the moment we are interested in.

Source of data is RabbitMQ, where OpenSky vectors are fed every minute.
Stream data is enriched with airport information (many thanks
to [ip2location project](https://github.com/ip2location/ip2location-iata-icao/tree/master))

## Flink features used

* Custom RMQSource
* RichAsyncFunction
* Custom Watermark strategy
* State backend: RocksDB

Postgres is used as a database to store the data, but can be easily changed to any other database.
All the params are submitted as arguments

```shell
--RABBITMQ_HOST rabbitmq-service --RABBITMQ_USER admin --RABBITMQ_PASSWORD admin --DB_URL  "jdbc:postgresql://postgres-url/flink-db?currentSchema=public" --DB_DRIVER org.postgresql.Driver --DB_USER flink --DB_PASSWORD 12345 
```

## How to run

I used a Flink cluster running on Kubernetes, but it can run locally as well, including writes to a SQLIte database.

1. Prepare the db and rabbitmq

```shell
docker-compose up
```

2. Run from IDE with args

```shell
--RABBITMQ_HOST 0.0.0.0 --RABBITMQ_USER user --RABBITMQ_PASSWORD bitnami --DB_URL jdbc:sqlite:dev-db/dev-db.sqlite --DB_DRIVER org.sqlite.JDBC --IGNORE_ROCKSDB true
```