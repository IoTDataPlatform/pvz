package iot.data.platform.devices.core;

public record DeviceState(
        String deviceId,
        String env,
        String tenantId,
        String lat,
        String lon,
        String h,
        String t,
        String tsHt,
        String rssi,
        String snr,
        String bat,
        String online,
        String tsState
) {
}
