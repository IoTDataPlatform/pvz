package iot.data.platform.devices.infra;

import iot.data.platform.devices.api.RecentSummaryResponse;
import iot.data.platform.devices.core.DeviceState;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class RedisDeviceRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;

    public RedisDeviceRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    public List<DeviceState> findAllByTenant(String env, String tenantId) {
        String pattern = String.format("pvz:%s:%s:device:*:state", env, tenantId);
        Set<String> stateKeys = redisTemplate.keys(pattern);

        if (stateKeys.isEmpty()) {
            return List.of();
        }

        List<DeviceState> result = new ArrayList<>();
        for (String stateKey : stateKeys) {
            String deviceId = extractDeviceId(stateKey);
            if (deviceId == null) continue;

            DeviceState state = findById(env, tenantId, deviceId);
            if (state != null) {
                result.add(state);
            }
        }
        return result;
    }

    private String extractDeviceId(String stateKey) {
        int deviceIdx = stateKey.indexOf(":device:");
        int stateIdx = stateKey.lastIndexOf(":state");
        if (deviceIdx < 0 || stateIdx < 0 || stateIdx <= deviceIdx + 8) {
            return null;
        }
        return stateKey.substring(deviceIdx + 8, stateIdx);
    }

    public DeviceState findById(String env, String tenantId, String deviceId) {
        String stateKey = RedisKeys.deviceState(env, tenantId, deviceId);
        Map<String, String> map;
        try {
            map = hashOps.entries(stateKey);
        } catch (DataAccessException e) {
            return null;
        }
        if (map.isEmpty()) {
            return null;
        }
        return new DeviceState(
                deviceId,
                env,
                tenantId,
                map.get("lat"),
                map.get("lon"),
                map.get("h"),
                map.get("t"),
                map.get("ts_ht"),
                map.get("rssi"),
                map.get("snr"),
                map.get("bat"),
                map.get("online"),
                map.get("ts_state")
        );
    }

    public RecentSummaryResponse findRecentSummary(String env, String tenantId) {
        String redisKey = env + ":" + tenantId;

        Map<String, String> map;
        try {
            map = hashOps.entries(redisKey);
        } catch (DataAccessException e) {
            return emptySummary(env, tenantId);
        }

        return map.isEmpty() ? emptySummary(env, tenantId) : new RecentSummaryResponse(
                env,
                tenantId,
                600,
                parseInt(map.get("totalDevices")),
                parseInt(map.get("onlineDevices")),
                parseInt(map.get("offlineDevices")),
                parseDoubleOrNull(map.get("avgTemp")),
                parseDoubleOrNull(map.get("avgHumidity"))
        );
    }

    private RecentSummaryResponse emptySummary(String env, String tenantId) {
        return new RecentSummaryResponse(
                env,
                tenantId,
                600,
                0,
                0,
                0,
                null,
                null
        );
    }

    private int parseInt(String v) {
        if (v == null || v.isBlank()) return 0;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double parseDoubleOrNull(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
