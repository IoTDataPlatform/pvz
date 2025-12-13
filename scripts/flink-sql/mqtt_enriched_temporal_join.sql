SET
'execution.checkpointing.interval' = '10 s';

DROP TABLE IF EXISTS mqtt_humidity;
DROP TABLE IF EXISTS mqtt_location_dim;
DROP TABLE IF EXISTS mqtt_state_dim;
DROP TABLE IF EXISTS mqtt_enriched;

CREATE TABLE mqtt_humidity
(
    d  STRING,
    ts BIGINT,
    h DOUBLE,
    t DOUBLE,
    rt AS TO_TIMESTAMP_LTZ(ts, 3),
    WATERMARK FOR rt AS rt - INTERVAL '5' SECOND
) WITH (
      'connector' = 'kafka',
      'topic' = 'mqtt_humidity',
      'properties.bootstrap.servers' = 'kafka:9092',
      'properties.group.id' = 'flink-mqtt-humidity',
      'scan.startup.mode' = 'earliest-offset',
      'format' = 'avro-confluent',
      'avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

CREATE TABLE mqtt_location_dim
(
    d  STRING,
    ts BIGINT,
    lat DOUBLE,
    lon DOUBLE,
    rt AS TO_TIMESTAMP_LTZ(ts, 3),
    WATERMARK FOR rt AS rt - INTERVAL '5' SECOND,
    PRIMARY KEY (d) NOT ENFORCED
) WITH (
      'connector' = 'upsert-kafka',
      'topic' = 'mqtt_location',
      'properties.bootstrap.servers' = 'kafka:9092',
      'key.format' = 'raw',
      'value.format' = 'avro-confluent',
      'value.avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

CREATE TABLE mqtt_state_dim
(
    d      STRING,
    ts     BIGINT,
    rssi   BIGINT,
    snr DOUBLE,
    bat DOUBLE,
    online BOOLEAN,
    rt AS TO_TIMESTAMP_LTZ(ts, 3),
    WATERMARK FOR rt AS rt - INTERVAL '5' SECOND,
    PRIMARY KEY (d) NOT ENFORCED
) WITH (
      'connector' = 'upsert-kafka',
      'topic' = 'mqtt_state',
      'properties.bootstrap.servers' = 'kafka:9092',
      'key.format' = 'raw',
      'value.format' = 'avro-confluent',
      'value.avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

CREATE TABLE mqtt_enriched
(
    d         STRING,
    env       STRING,
    tenant    STRING,
    lat DOUBLE,
    lon DOUBLE,
    h DOUBLE,
    t DOUBLE,
    ts_ht     BIGINT,
    rssi      BIGINT,
    snr DOUBLE,
    bat DOUBLE,
    online    BOOLEAN,
    ts_state  BIGINT,
    redis_key STRING
) WITH (
      'connector' = 'kafka',
      'topic' = 'mqtt_enriched',
      'properties.bootstrap.servers' = 'kafka:9092',
      'format' = 'avro-confluent',
      'avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

CREATE TABLE device_measurements
(
    env       STRING,
    tenant_id STRING,
    device_id STRING,
    ts        TIMESTAMP(3),
    temperature DOUBLE,
    humidity DOUBLE,
    status    STRING
) WITH (
      'connector' = 'kafka',
      'topic' = 'device_measurements',
      'properties.bootstrap.servers' = 'kafka:9092',
      'format' = 'avro-confluent',
      'avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

BEGIN STATEMENT
SET;

INSERT INTO mqtt_enriched
SELECT h.d                                                AS d,
       'prod'                                             AS env,
       'tenant-1'                                         AS tenant,
       l.lat                                              AS lat,
       l.lon                                              AS lon,
       h.h                                                AS h,
       h.t                                                AS t,
       h.ts                                               AS ts_ht,
       s.rssi                                             AS rssi,
       s.snr                                              AS snr,
       s.bat                                              AS bat,
       s.online                                           AS online,
       s.ts                                               AS ts_state,
       CONCAT('pvz:prod:tenant-1:device:', h.d, ':state') AS redis_key
FROM mqtt_humidity h
         LEFT JOIN mqtt_location_dim FOR SYSTEM_TIME AS OF h.rt AS l ON h.d = l.d
         LEFT JOIN mqtt_state_dim FOR SYSTEM_TIME AS OF h.rt AS s ON h.d = s.d;

INSERT INTO device_measurements
SELECT 'prod'                                          AS env,
       'tenant-1'                                      AS tenant_id,
       h.d                                             AS device_id,
       CAST(TO_TIMESTAMP_LTZ(h.ts, 3) AS TIMESTAMP(3)) AS ts,
       h.t                                             AS temperature,
       h.h                                             AS humidity,
       CASE
           WHEN s.online THEN 'OK'
           ELSE 'ERROR'
           END                                         AS status
FROM mqtt_humidity h
         LEFT JOIN mqtt_state_dim FOR SYSTEM_TIME AS OF h.rt AS s ON h.d = s.d;

END;