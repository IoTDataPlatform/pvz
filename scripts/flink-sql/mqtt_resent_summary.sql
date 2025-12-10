CREATE TABLE mqtt_enriched
(
    d         STRING,
    env       STRING,
    tenant    STRING,
    lat       STRING,
    lon       STRING,
    h         STRING,
    t         STRING,
    ts_ht     STRING,
    rssi      STRING,
    snr       STRING,
    bat       STRING,
    online    BOOLEAN,
    ts_state  STRING,
    redis_key STRING,
    rt AS TO_TIMESTAMP_LTZ(CAST(ts_ht AS BIGINT), 3),
    WATERMARK FOR rt AS rt - INTERVAL '5' SECOND
) WITH (
      'connector' = 'kafka',
      'topic' = 'mqtt_enriched',
      'properties.bootstrap.servers' = 'kafka:9092',
      'properties.group.id' = 'flink-mqtt-recent-summary',
      'scan.startup.mode' = 'earliest-offset',
      'format' = 'avro-confluent',
      'avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

CREATE TABLE mqtt_recent_summary
(
    redis_key      STRING,
    env            STRING,
    tenantId       STRING,
    window_start   TIMESTAMP(3),
    window_end     TIMESTAMP(3),
    windowSeconds  BIGINT,
    totalDevices   BIGINT,
    onlineDevices  BIGINT,
    offlineDevices BIGINT,
    avgTemp DOUBLE,
    avgHumidity DOUBLE,
    PRIMARY KEY (redis_key) NOT ENFORCED
) WITH (
      'connector' = 'upsert-kafka',
      'topic' = 'mqtt_recent_summary',
      'properties.bootstrap.servers' = 'kafka:9092',
      'key.format' = 'raw',
      'value.format' = 'avro-confluent',
      'value.avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

INSERT INTO mqtt_recent_summary
SELECT CONCAT(env, ':', tenant)                                        AS redis_key,
       env,
       tenant                                                          AS tenantId,
       window_start,
       window_end,
       600                                                             AS windowSeconds,
       COUNT(DISTINCT d)                                               AS totalDevices,
       COUNT(DISTINCT CASE WHEN online THEN d END)                     AS onlineDevices,
       COUNT(DISTINCT d) - COUNT(DISTINCT CASE WHEN online THEN d END) AS offlineDevices,
       AVG(CAST(t AS DOUBLE))                                          AS avgTemp,
       AVG(CAST(h AS DOUBLE))                                          AS avgHumidity
FROM TABLE(
        HOP(
            TABLE mqtt_enriched, DESCRIPTOR (rt), INTERVAL '10' SECOND, INTERVAL '10' MINUTE
        )
     )
GROUP BY env,
         tenant,
         window_start,
         window_end;
