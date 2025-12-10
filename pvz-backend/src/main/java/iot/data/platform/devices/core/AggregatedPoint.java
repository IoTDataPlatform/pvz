package iot.data.platform.devices.core;

import java.sql.Timestamp;
import java.time.Instant;

public record AggregatedPoint(
        Instant bucketStart,
        Double tAvg,
        Double hAvg,
        Long totalCount,
        Long okCount,
        Long errorCount
) {
    public AggregatedPoint(
            Object bucketStart,
            Double tAvg,
            Double hAvg,
            Long totalCount,
            Long okCount,
            Long errorCount
    ) {
        this(
                convertToInstant(bucketStart),
                tAvg,
                hAvg,
                totalCount,
                okCount,
                errorCount
        );
    }
    
    private static Instant convertToInstant(Object value) {
        if (value instanceof Instant i) {
            return i;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        throw new IllegalArgumentException(
                "Unsupported bucketStart type: " + value + " (" + value.getClass() + ")"
        );
    }
}
