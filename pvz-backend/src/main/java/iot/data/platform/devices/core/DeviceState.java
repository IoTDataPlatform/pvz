package iot.data.platform.devices.core;

public record DeviceState(
        String deviceId,
        String env,
        String tenantId,
        Double lat,
        Double lon,
        Double h,
        Double t,
        Long tsHt,
        Long rssi,
        Double snr,
        Double bat,
        Boolean online,
        Long tsState
) {
}
