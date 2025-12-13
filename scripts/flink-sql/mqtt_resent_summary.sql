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
    redis_key STRING,

    rt AS TO_TIMESTAMP_LTZ(ts_ht, 3),
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

CREATE TABLE mqtt_low_humidity_streak
(
    redis_key  STRING,
    env        STRING,
    tenantId   STRING,
    device_id  STRING,
    threshold DOUBLE,
    last_ts    BIGINT,
    last_ok_ts BIGINT,
    streak_days DOUBLE,
    last_h DOUBLE,
    PRIMARY KEY (redis_key) NOT ENFORCED
) WITH (
      'connector' = 'upsert-kafka',
      'topic' = 'mqtt_low_humidity_streak',
      'properties.bootstrap.servers' = 'kafka:9092',
      'key.format' = 'raw',
      'value.format' = 'avro-confluent',
      'value.avro-confluent.schema-registry.url' = 'http://kafka-schema-registry:8081'
      );

BEGIN STATEMENT
SET;

INSERT INTO mqtt_recent_summary
SELECT CONCAT(env, ':', tenant)                                         AS redis_key,
       env,
       tenant                                                           AS tenantId,
       window_start,
       window_end,
       CAST(600 AS BIGINT)                                              AS windowSeconds,

       COUNT(DISTINCT d)                                                AS totalDevices,
       COUNT(DISTINCT CASE WHEN COALESCE(online, FALSE) THEN d END)     AS onlineDevices,
       COUNT(DISTINCT CASE WHEN NOT COALESCE(online, FALSE) THEN d END) AS offlineDevices,

       AVG(t)                                                           AS avgTemp,
       AVG(h)                                                           AS avgHumidity
FROM TABLE(
        HOP(
            TABLE mqtt_enriched, DESCRIPTOR(rt), INTERVAL '10' SECOND,
                                                 INTERVAL '10' MINUTE
        )
     )
GROUP BY env, tenant, window_start, window_end;

INSERT INTO mqtt_low_humidity_streak
WITH agg AS (SELECT env,
                    tenant,
                    d,
                    MAX(ts_ht)                              AS last_ts,
                    MAX(CASE WHEN h >= 50.0 THEN ts_ht END) AS last_ok_ts,
                    MIN(ts_ht)                              AS first_ts
             FROM mqtt_enriched
             GROUP BY env, tenant, d),
     latest AS (SELECT env, tenant, d, ts_ht AS last_ts, h AS last_h
                FROM (SELECT env,
                             tenant,
                             d,
                             ts_ht,
                             h,
                             ROW_NUMBER() OVER (
                PARTITION BY env, tenant, d
                ORDER BY ts_ht DESC
            ) AS rn
                      FROM mqtt_enriched)
                WHERE rn = 1)
SELECT CONCAT('pvz:', a.env, ':', a.tenant, ':device:', a.d, ':humidity_low_streak') AS redis_key,
       a.env,
       a.tenant                                                                      AS tenantId,
       a.d                                                                           AS device_id,
       50.0                                                                          AS threshold,
       a.last_ts,
       a.last_ok_ts,
       (a.last_ts - COALESCE(a.last_ok_ts, a.first_ts)) / 86400000.0                 AS streak_days,
       l.last_h
FROM agg a
         LEFT JOIN latest l
                   ON a.env = l.env
                       AND a.tenant = l.tenant
                       AND a.d = l.d
                       AND a.last_ts = l.last_ts;

END;