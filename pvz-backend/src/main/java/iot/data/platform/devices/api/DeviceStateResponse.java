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
                parseDouble(state.lat()),
                parseDouble(state.lon()),
                parseDouble(state.h()),
                parseDouble(state.t()),
                parseLong(state.tsHt()),
                parseInt(state.rssi()),
                parseDouble(state.snr()),
                parseDouble(state.bat()),
                parseBool(state.online()),
                parseLong(state.tsState())
        );
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBool(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.toLowerCase()) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> null;
        };
    }
}
