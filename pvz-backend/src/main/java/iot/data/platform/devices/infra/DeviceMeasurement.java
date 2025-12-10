package iot.data.platform.devices.infra;

import iot.data.platform.devices.core.DeviceStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "device_measurements")
public class DeviceMeasurement {

    private String env;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "device_id")
    private String deviceId;

    @Id
    @Column(name = "ts", nullable = false)
    private Instant ts;

    private Double temperature;

    private Double humidity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeviceStatus status;

    protected DeviceMeasurement() {
    }

    public DeviceMeasurement(
            String env,
            String tenantId,
            String deviceId,
            Instant ts,
            Double temperature,
            Double humidity,
            DeviceStatus status
    ) {
        this.env = env;
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.ts = ts;
        this.temperature = temperature;
        this.humidity = humidity;
        this.status = status;
    }

    public String getEnv() {
        return env;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Instant getTs() {
        return ts;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public DeviceStatus getStatus() {
        return status;
    }
}
