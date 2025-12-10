CONNECT_HOST ?= localhost
CONNECT_PORT ?= 8083
CONNECT_URL  := http://$(CONNECT_HOST):$(CONNECT_PORT)

CONFIG ?= ./configs/mqtt-sensors-source.json
CONNECTOR := $(basename $(CONFIG:.json=))

FLINK_JOBMANAGER ?= flink-jobmanager
FLINK_BIN        := docker exec -i $(FLINK_JOBMANAGER) /opt/flink/bin
FLINK_SQL_ENRICHER_PATH   ?= /opt/flink/sql/mqtt_enriched_temporal_join.sql
FLINK_SQL_RESENT_SUMMARY_PATH   ?= /opt/flink/sql/mqtt_resent_summary.sql
JOB_ID           ?=
KAFKA_BIN := docker compose exec -i kafka /usr/bin

kafka-topics-init:
	@$(KAFKA_BIN)/kafka-topics --create --if-not-exists \
	  --bootstrap-server kafka:9092 \
	  --topic mqtt_humidity \
	  --partitions 1 --replication-factor 1 || true
	@$(KAFKA_BIN)/kafka-topics --create --if-not-exists \
	  --bootstrap-server kafka:9092 \
	  --topic mqtt_location \
	  --partitions 1 --replication-factor 1 || true
	@$(KAFKA_BIN)/kafka-topics --create --if-not-exists \
	  --bootstrap-server kafka:9092 \
	  --topic mqtt_state \
	  --partitions 1 --replication-factor 1 || true
	@$(KAFKA_BIN)/kafka-topics --create --if-not-exists \
	  --bootstrap-server kafka:9092 \
	  --topic mqtt_enriched \
	  --partitions 1 --replication-factor 1 || true
	@$(KAFKA_BIN)/kafka-topics --create --if-not-exists \
	  --bootstrap-server kafka:9092 \
	  --topic mqtt_recent_summary \
	  --partitions 1 --replication-factor 1 || true
	@echo "Kafka topics ready."

.PHONY: connector-up connector-down connect-connectors \
        mqtt-sensors-source-up mqtt-sensors-source-down \
        mqtt-postgres-sink-up mqtt-postgres-sink-down \
        mqtt-enriched-redis-sink-up mqtt-enriched-redis-sink-down \
        flink-job-up flink-job-down flink-job-list

connector-up:
	@curl -sS -X POST "$(CONNECT_URL)/connectors" \
	  -H "Content-Type: application/json" \
	  --data @$(CONFIG) \
	  -o /dev/null \
	  && echo "Done." \
	  || echo "Request failed"

connector-down:
	@curl -sS -X DELETE "$(CONNECT_URL)/connectors/$(CONNECTOR)" \
	  -o /dev/null \
	  && echo "Done." \
	  || echo "Request failed"

mqtt-sensors-source-up:
	$(MAKE) connector-up CONFIG=./configs/mqtt-sensors-source.json

mqtt-sensors-source-down:
	$(MAKE) connector-down CONFIG=./configs/mqtt-sensors-source.json

mqtt-postgres-sink-up:
	$(MAKE) connector-up CONFIG=./configs/mqtt-postgres-sink.json

mqtt-postgres-sink-down:
	$(MAKE) connector-down CONFIG=./configs/mqtt-postgres-sink.json

mqtt-enriched-redis-sink-up:
	$(MAKE) connector-up CONFIG=./configs/mqtt-enriched-redis-sink.json

mqtt-enriched-redis-sink-down:
	$(MAKE) connector-down CONFIG=./configs/mqtt-enriched-redis-sink.json

connect-connectors:
	@curl -sS "$(CONNECT_URL)/connectors" \
	  -o /dev/null \
	  && echo "Done." \
	  || echo "Request failed"

flink-jobs-up: kafka-topics-init
	@$(FLINK_BIN)/sql-client.sh -f $(FLINK_SQL_ENRICHER_PATH) \
	  >/dev/null 2>&1 \
	  && echo "Enricher job started." \
	  || echo "Enricher job failed"
	@$(FLINK_BIN)/sql-client.sh -f $(FLINK_SQL_RESENT_SUMMARY_PATH) \
	  >/dev/null 2>&1 \
	  && echo "Summary job started." \
	  || echo "Summary job failed"

flink-jobs-down:
	@jobs=`$(FLINK_BIN)/flink list | awk '/RUNNING/ {print $$4}'`; \
	if [ -z "$$jobs" ]; then \
	  echo "No running Flink jobs found"; \
	else \
	  for job in $$jobs; do \
	    echo "Cancelling Flink job $$job..."; \
	    $(FLINK_BIN)/flink cancel $$job >/dev/null 2>&1 || echo "Failed to cancel $$job"; \
	  done; \
	  echo "Done."; \
	fi

flink-job-list:
	@$(FLINK_BIN)/flink list
