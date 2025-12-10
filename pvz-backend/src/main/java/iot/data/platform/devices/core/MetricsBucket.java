package iot.data.platform.devices.core;

public enum MetricsBucket {
    HOUR("hour"),
    DAY("day"),
    WEEK("week");

    private final String postgresUnit;

    MetricsBucket(String postgresUnit) {
        this.postgresUnit = postgresUnit;
    }

    public String postgresUnit() {
        return postgresUnit;
    }
}
