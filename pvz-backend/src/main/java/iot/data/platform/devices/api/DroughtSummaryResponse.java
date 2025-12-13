package iot.data.platform.devices.api;

public record DroughtSummaryResponse(
        String env,
        String tenantId,
        Double threshold,
        int devicesInDrought,
        Double maxStreakDays,
        String maxDeviceId
) {}
