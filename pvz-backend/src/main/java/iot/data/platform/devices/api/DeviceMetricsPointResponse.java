package iot.data.platform.devices.api;

public record DeviceMetricsPointResponse(
        long ts,
        Double tAvg,
        Double hAvg
) {
}
