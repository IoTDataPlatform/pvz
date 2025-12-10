package iot.data.platform.devices.infra;

import iot.data.platform.devices.core.AggregatedPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DeviceMeasurementRepository extends JpaRepository<DeviceMeasurement, Long> {
    @Query("""
            select new iot.data.platform.devices.core.AggregatedPoint(
                function('date_trunc', :bucket, m.ts),
                avg(m.temperature),
                avg(m.humidity),
                count(m.status),
                sum(case when m.status = 'OK' then 1 else 0 end),
                sum(case when m.status = 'ERROR' then 1 else 0 end)
            )
            from DeviceMeasurement m
            where m.env = :env
              and m.tenantId = :tenantId
              and m.deviceId = :deviceId
              and m.ts between :fromTs and :toTs
            group by 1
            order by 1
            """)
    List<AggregatedPoint> aggregateByTime(
            @Param("env") String env,
            @Param("tenantId") String tenantId,
            @Param("deviceId") String deviceId,
            @Param("bucket") String bucket,
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs
    );

}
