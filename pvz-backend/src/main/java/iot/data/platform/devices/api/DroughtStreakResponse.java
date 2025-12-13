package iot.data.platform.devices.api;

public record DroughtStreakResponse(
        String env,
        String tenantId,
        String deviceId,
        Double threshold,
        Long lastTs,
        Long lastOkTs,
        Double streakDays,
        Double lastH
) {
}
