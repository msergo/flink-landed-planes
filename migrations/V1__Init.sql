CREATE TABLE "airports" (
"id" serial NOT NULL,
"country_code" character(2) NULL,
"region_name" character varying(255) NULL,
"iata" character(3) NULL,
"icao" character(4) NULL,
"airport" character varying(255) NULL,
"latitude" double precision NULL,
"longitude" double precision NULL,
PRIMARY KEY ("id"));

CREATE INDEX "airports_latitude_idx" ON "airports" ("latitude");
CREATE INDEX "airports_longitude_idx" ON "airports" ("longitude");

CREATE TABLE "flight_data" (
"id" serial NOT NULL,
"icao24" character varying(6) NOT NULL,
"callsign" character varying(10) NULL,
"category" character varying(255) NULL,
"origin_country" character varying(50) NULL,
"latitude" double precision NULL,
"longitude" double precision NULL,
"last_contact" double precision NULL,
"airport_id" integer NULL,
"created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY ("id"));

CREATE INDEX "flight_data_airport_id_idx" ON "flight_data" ("airport_id");
CREATE INDEX "flight_data_latitude_idx" ON "flight_data" ("latitude");
CREATE INDEX "flight_data_longitude_idx" ON "flight_data" ("longitude");
