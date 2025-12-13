package iot.data.platform.devices.api;

import iot.data.platform.devices.core.DeviceState;
import iot.data.platform.devices.core.DeviceMetricsService;
import iot.data.platform.devices.core.MetricsBucket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/{env}/{tenantId}/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final DeviceMetricsService deviceMetricsService;

    public DeviceController(DeviceService deviceService,
                            DeviceMetricsService deviceMetricsService) {
        this.deviceService = deviceService;
        this.deviceMetricsService = deviceMetricsService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceStateResponse>> getAllDevices(
            @PathVariable String env,
            @PathVariable String tenantId
    ) {
        List<DeviceState> states = deviceService.getAllDevices(env, tenantId);
        List<DeviceStateResponse> response = states.stream()
                .map(DeviceStateResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceStateResponse> getDevice(
            @PathVariable String env,
            @PathVariable String tenantId,
            @PathVariable String deviceId
    ) {
        DeviceState state = deviceService.getDevice(env, tenantId, deviceId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(DeviceStateResponse.from(state));
    }

    @GetMapping("/{deviceId}/metrics")
    public ResponseEntity<DeviceMetricsResponse> getDeviceMetrics(
            @PathVariable String env,
            @PathVariable String tenantId,
            @PathVariable String deviceId,
            @RequestParam(name = "bucket", defaultValue = "HOUR") MetricsBucket bucket,
            @RequestParam(name = "from", required = false) Long from,
            @RequestParam(name = "to", required = false) Long to
    ) {
        DeviceMetricsResponse response =
                deviceMetricsService.getMetrics(env, tenantId, deviceId, bucket, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/recent")
    public ResponseEntity<RecentSummaryResponse> getRecentSummary(
            @PathVariable String env,
            @PathVariable String tenantId
    ) {
        RecentSummaryResponse response =
                deviceService.getRecentSummary(env, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentDeviceSnapshotResponse>> getRecentSnapshots(
            @PathVariable String env,
            @PathVariable String tenantId
    ) {
        List<RecentDeviceSnapshotResponse> response =
                deviceService.getRecentSnapshots(env, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{deviceId}/drought")
    public ResponseEntity<DroughtStreakResponse> getDroughtStreak(
            @PathVariable String env,
            @PathVariable String tenantId,
            @PathVariable String deviceId
    ) {
        DroughtStreakResponse resp = deviceService.getDroughtStreak(env, tenantId, deviceId);
        return resp == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(resp);
    }

    @GetMapping("/summary/drought")
    public ResponseEntity<DroughtSummaryResponse> getDroughtSummary(
            @PathVariable String env,
            @PathVariable String tenantId
    ) {
        return ResponseEntity.ok(deviceService.getDroughtSummary(env, tenantId));
    }
}
