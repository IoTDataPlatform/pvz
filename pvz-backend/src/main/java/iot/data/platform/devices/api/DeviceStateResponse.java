package iot.data.platform.devices.api;

import iot.data.platform.devices.core.DeviceState;

public record DeviceStateResponse(
        String deviceId,
        String env,
        String tenantId,
        Double lat,
        Double lon,
        Double h,
        Double t,
        Long tsHt,
        Integer rssi,
        Double snr,
        Double bat,
        Boolean online,
        Long tsState
) {
    public static DeviceStateResponse from(DeviceState state) {
        return new DeviceStateResponse(
                state.deviceId(),
                state.env(),
                state.tenantId(),
                state.lat(),
                state.lon(),
                state.h(),
                state.t(),
                state.tsHt(),
                state.rssi() == null ? null : state.rssi().intValue(),
                state.snr(),
                state.bat(),
                state.online(),
                state.tsState()
        );
    }
}
