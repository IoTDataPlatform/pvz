export interface DeviceState {
    deviceId: string;
    env: string;
    tenantId: string;
    lat: number | null;
    lon: number | null;
    h: number | null;
    t: number | null;
    tsHt: number | null;
    rssi: number | null;
    snr: number | null;
    bat: number | null;
    online: boolean | null;
    tsState: number | null;
}

export type MetricsBucket = 'hour' | 'day' | 'week';

export interface DeviceMetricsPoint {
    ts: number;
    tAvg: number | null;
    hAvg: number | null;
}

export interface DeviceMetricsResponse {
    deviceId: string;
    bucket: MetricsBucket;
    points: DeviceMetricsPoint[];
}

export interface RecentSummary {
    env: string;
    tenantId: string;
    windowSeconds: number;
    generatedAt: number;
    totalDevices: number;
    onlineDevices: number;
    offlineDevices: number;
    avgTemp: number | null;
    avgHumidity: number | null;
}

export interface DroughtSummary {
    env: string;
    tenantId: string;
    threshold: number | null;
    devicesInDrought: number;
    maxStreakDays: number | null;
    maxDeviceId: string | null;
}

export interface DroughtStreak {
    env: string;
    tenantId: string;
    deviceId: string;
    threshold: number | null;
    lastTs: number | null;
    lastOkTs: number | null;
    streakDays: number | null;
    lastH: number | null;
}

