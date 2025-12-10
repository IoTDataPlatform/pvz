package iot.data.platform.devices.api;

import iot.data.platform.devices.core.MetricsBucket;

import java.util.List;

public record DeviceMetricsResponse(
        String deviceId,
        MetricsBucket bucket,
        List<DeviceMetricsPointResponse> points
) {
}
