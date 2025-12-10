import os
import time
import json
import random
import itertools
import math
from datetime import datetime, timezone
import hashlib

import paho.mqtt.client as mqtt

MQTT_HOST = os.getenv("MQTT_HOST", "mqtt-broker")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))

ENVIRONMENTS = [x.strip() for x in os.getenv("ENVIRONMENTS", "dev").split(",") if x.strip()]
TENANTS = [x.strip() for x in os.getenv("TENANTS", "tenant-a").split(",") if x.strip()]

DEVICES = [
    x.strip()
    for x in os.getenv(
        "DEVICES",
        "device-001,device-002,device-003,device-004,device-005"
    ).split(",")
    if x.strip()
]

DRYNESS_LEVEL = float(os.getenv("DRYNESS_LEVEL", "0.0"))

REALTIME_INTERVAL_SEC = float(os.getenv("REALTIME_INTERVAL_SEC", "60"))

BACKFILL_FROM_YEAR = int(os.getenv("BACKFILL_FROM_YEAR", "2020"))
BACKFILL_INTERVAL_SEC = float(os.getenv("BACKFILL_INTERVAL_SEC", "60"))

CLIENT_ID = os.getenv("CLIENT_ID", "sensor-emulator")

BASE_LAT = float(os.getenv("BASE_LAT", "54.8433"))
BASE_LON = float(os.getenv("BASE_LON", "83.0931"))

MONTHLY_HUMIDITY = [
    88,  # Jan
    86,  # Feb
    80,  # Mar
    68,  # Apr
    58,  # May
    64,  # Jun
    69,  # Jul
    70,  # Aug
    72,  # Sep
    76,  # Oct
    82,  # Nov
    85,  # Dec
]

print("MQTT_HOST:", MQTT_HOST)
print("MQTT_PORT:", MQTT_PORT)
print("ENVIRONMENTS:", ENVIRONMENTS)
print("TENANTS:", TENANTS)
print("DEVICES:", DEVICES)
print("REALTIME_INTERVAL_SEC:", REALTIME_INTERVAL_SEC)
print("BACKFILL_FROM_YEAR:", BACKFILL_FROM_YEAR)
print("BACKFILL_INTERVAL_SEC:", BACKFILL_INTERVAL_SEC)
print("BASE_LAT / BASE_LON:", BASE_LAT, BASE_LON)

def _month_of_year(ts_ms: int) -> int:
    dt = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc)
    return dt.month

def make_sensor_topic(env: str, tenant: str, device: str, suffix: str) -> str:
    return f"{env}/{tenant}/sensors/{device}/{suffix}"


def make_device_topic(env: str, tenant: str, device: str, suffix: str) -> str:
    return f"{env}/{tenant}/devices/{device}/{suffix}"


def on_connect(client, userdata, flags, reason_code, properties=None):
    print("Connected to MQTT broker, rc:", reason_code)
    topic_filter = "+/+/devices/+/command"
    client.subscribe(topic_filter, qos=0)
    print("Subscribed to commands:", topic_filter)


def on_message(client, userdata, msg):
    print("Command received:", msg.topic, msg.payload)
    try:
        payload = json.loads(msg.payload.decode("utf-8"))
    except Exception:
        payload = {}

    parts = msg.topic.split("/")
    if len(parts) < 5:
        print("Unexpected topic format for command, skip")
        return

    env, tenant, kind, device, suffix = parts[0], parts[1], parts[2], parts[3], parts[4]

    cmd_id = payload.get("cmd_id", "unknown")
    ack_topic = make_device_topic(env, tenant, device, "ack")

    ack_payload = {
        "cmd_id": cmd_id,
        "ts": int(time.time() * 1000),
        "status": "ok",
        "details": "Command accepted by emulator"
    }

    client.publish(ack_topic, json.dumps(ack_payload), qos=0, retain=False)
    print("Sent ACK:", ack_topic, ack_payload)


client = mqtt.Client(client_id=CLIENT_ID, clean_session=True)
client.on_connect = on_connect
client.on_message = on_message


def connect_mqtt():
    while True:
        try:
            client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
            print("MQTT connect success")
            return
        except Exception as e:
            print("MQTT connect failed, retry in 3s:", e)
            time.sleep(3)


def device_location(device: str):
    h = int(hashlib.sha1(device.encode()).hexdigest()[:8], 16)

    lat_range = 0.01
    lon_range = 0.02

    lat_seed = (h % 65536) / 65535.0
    lon_seed = ((h // 65536) % 65536) / 65535.0

    lat_off = (lat_seed - 0.5) * lat_range
    lon_off = (lon_seed - 0.5) * lon_range

    lat = BASE_LAT + lat_off
    lon = BASE_LON + lon_off

    lat += random.gauss(0, 0.0001)
    lon += random.gauss(0, 0.0001)

    return lat, lon


def _day_of_year(ts_ms: int) -> int:
    dt = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc)
    return dt.timetuple().tm_yday


def seasonal_temperature(ts_ms: int) -> float:
    day = _day_of_year(ts_ms)
    phase_shift_days = 200
    angle = 2.0 * math.pi * ((day - 1 - phase_shift_days) / 365.0)

    avg = 5.0
    amp = 25.0
    noise = random.gauss(0, 1.0)

    return avg + amp * math.sin(angle) + noise


def seasonal_humidity(ts_ms: int) -> float:
    month = _month_of_year(ts_ms)

    base_h = MONTHLY_HUMIDITY[month - 1]

    noise = random.gauss(0, 3.0)

    h = base_h + noise - DRYNESS_LEVEL
    return max(0.0, min(100.0, h))


def publish_for_timestamp(ts_ms: int):
    for env, tenant, device in itertools.product(ENVIRONMENTS, TENANTS, DEVICES):
        base = {"d": device, "ts": ts_ms}

        temp = seasonal_temperature(ts_ms)
        humidity = seasonal_humidity(ts_ms)

        humidity_payload = dict(base)
        humidity_payload["h"] = round(humidity, 1)
        humidity_payload["t"] = round(temp, 1)

        humidity_topic = make_sensor_topic(env, tenant, device, "humidity")
        client.publish(humidity_topic, json.dumps(humidity_payload), qos=0, retain=False)

        lat, lon = device_location(device)

        location_payload = dict(base)
        location_payload["lat"] = round(lat, 6)
        location_payload["lon"] = round(lon, 6)

        location_topic = make_sensor_topic(env, tenant, device, "location")
        client.publish(location_topic, json.dumps(location_payload), qos=0, retain=False)

        rssi = random.randint(-100, -50)
        snr = round(random.uniform(-10.0, 10.0), 1)
        bat = round(random.uniform(20.0, 100.0), 1)
        online = True

        state_payload = dict(base)
        state_payload.update({
            "rssi": rssi,
            "snr": snr,
            "bat": bat,
            "online": online,
        })

        state_topic = make_sensor_topic(env, tenant, device, "state")
        client.publish(state_topic, json.dumps(state_payload), qos=0, retain=False)


def backfill_history():
    """Быстро заливаем историю с BACKFILL_FROM_YEAR до момента старта."""
    start_dt = datetime(BACKFILL_FROM_YEAR, 1, 1, tzinfo=timezone.utc)
    start_ms = int(start_dt.timestamp() * 1000)
    end_ms = int(time.time() * 1000)

    step_ms = int(BACKFILL_INTERVAL_SEC * 1000)
    if step_ms <= 0:
        raise ValueError("BACKFILL_INTERVAL_SEC must be > 0")

    print(f"Starting backfill from {start_dt.isoformat()} to current time, "
          f"step = {BACKFILL_INTERVAL_SEC} sec")

    ts_ms = start_ms
    batch = 0
    while ts_ms < end_ms:
        publish_for_timestamp(ts_ms)
        batch += 1
        if batch % 500 == 0:
            dt = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc)
            print(f"Backfill progress: {dt.isoformat()} (batch {batch})")
        ts_ms += step_ms

    print("Backfill finished, total batches:", batch)


def publish_loop():
    backfill_history()

    print("Switching to realtime mode, interval =", REALTIME_INTERVAL_SEC, "sec")

    while True:
        now_ms = int(time.time() * 1000)
        publish_for_timestamp(now_ms)
        time.sleep(REALTIME_INTERVAL_SEC)


if __name__ == "__main__":
    connect_mqtt()
    client.loop_start()
    publish_loop()
