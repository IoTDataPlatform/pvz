package iot.data.platform.devices.api;

import iot.data.platform.devices.core.DeviceState;
import iot.data.platform.devices.infra.RedisDeviceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceService {
    private final RedisDeviceRepository redisDeviceRepository;

    public DeviceService(RedisDeviceRepository redisDeviceRepository) {
        this.redisDeviceRepository = redisDeviceRepository;
    }

    public List<DeviceState> getAllDevices(String env, String tenantId) {
        return redisDeviceRepository.findAllByTenant(env, tenantId);
    }

    public DeviceState getDevice(String env, String tenantId, String deviceId) {
        return redisDeviceRepository.findById(env, tenantId, deviceId);
    }

    public RecentSummaryResponse getRecentSummary(String env, String tenantId) {
        return redisDeviceRepository.findRecentSummary(env, tenantId);
    }

    public List<RecentDeviceSnapshotResponse> getRecentSnapshots(
            String env,
            String tenantId,
            int windowSeconds
    ) {
        List<DeviceState> states = redisDeviceRepository.findAllByTenant(env, tenantId);
        Instant now = Instant.now();
        long threshold = now.getEpochSecond() - windowSeconds;

        List<RecentDeviceSnapshotResponse> result = new ArrayList<>();

        for (DeviceState s : states) {
            Long tsHt = parseLong(s.tsHt());
            Long tsState = parseLong(s.tsState());
            long lastSeen = 0L;
            if (tsHt != null) {
                lastSeen = tsHt;
            }
            if (tsState != null) {
                lastSeen = Math.max(lastSeen, tsState);
            }

            if (lastSeen < threshold) {
                continue;
            }

            result.add(new RecentDeviceSnapshotResponse(
                    s.deviceId(),
                    lastSeen,
                    parseDouble(s.t()),
                    parseDouble(s.h()),
                    parseBool(s.online()),
                    parseInt(s.rssi()),
                    parseDouble(s.snr()),
                    parseDouble(s.bat())
            ));
        }

        return result;
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
