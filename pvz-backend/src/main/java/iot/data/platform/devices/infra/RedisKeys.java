package iot.data.platform.devices.infra;

public final class RedisKeys {
    private RedisKeys() {
    }

    public static String devicesSet(String env, String tenantId) {
        return String.format("pvz:%s:%s:devices", env, tenantId);
    }

    public static String deviceState(String env, String tenantId, String deviceId) {
        return String.format("pvz:%s:%s:device:%s:state", env, tenantId, deviceId);
    }

    public static String deviceHumidityLowStreak(String env, String tenantId, String deviceId) {
        return String.format("pvz:%s:%s:device:%s:humidity_low_streak", env, tenantId, deviceId);
    }

    public static String deviceHumidityLowStreakPattern(String env, String tenantId) {
        return String.format("pvz:%s:%s:device:*:humidity_low_streak", env, tenantId);
    }

}
