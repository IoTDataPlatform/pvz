package iot.data.platform.devices.api;

public record RecentSummaryResponse(
        String env,
        String tenantId,
        int windowSeconds,
        int totalDevices,
        int onlineDevices,
        int offlineDevices,
        Double avgTemp,
        Double avgHumidity
) {
}
