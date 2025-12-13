import { env } from '../config/env';
import type {
    DeviceState,
    DeviceMetricsResponse,
    MetricsBucket,
    DeviceMetricsPoint,
    RecentSummary, DroughtSummary, DroughtStreak,
} from './types';

export async function fetchDevices(
    envName: string,
    tenantId: string
): Promise<DeviceState[]> {
    const res = await fetch(
        `${env.apiBaseUrl}/${encodeURIComponent(envName)}/${encodeURIComponent(
            tenantId
        )}/devices`
    );

    if (!res.ok) {
        throw new Error(`Failed to load devices: ${res.status}`);
    }

    return res.json();
}

type BackendMetricsBucket = 'HOUR' | 'DAY' | 'WEEK';

type BackendDeviceMetricsResponse = {
    deviceId: string;
    bucket: BackendMetricsBucket;
    points: {
        ts: number;
        tAvg: number | null;
        hAvg: number | null;
    }[];
};

export async function fetchDeviceMetrics(
    envName: string,
    tenantId: string,
    deviceId: string,
    bucket: MetricsBucket,
    fromTs?: number,
    toTs?: number
): Promise<DeviceMetricsResponse> {
    const params = new URLSearchParams();
    params.set('bucket', bucket.toUpperCase());

    if (fromTs != null) {
        params.set('from', String(fromTs));
    }
    if (toTs != null) {
        params.set('to', String(toTs));
    }

    const res = await fetch(
        `${env.apiBaseUrl}/${encodeURIComponent(envName)}/${encodeURIComponent(
            tenantId
        )}/devices/${encodeURIComponent(deviceId)}/metrics?${params.toString()}`
    );

    if (!res.ok) {
        throw new Error(
            `Failed to load metrics for device ${deviceId}: ${res.status}`
        );
    }

    const backend: BackendDeviceMetricsResponse = await res.json();

    const points: DeviceMetricsPoint[] =
        backend.points?.map((p) => ({
            ts: p.ts,
            tAvg: p.tAvg,
            hAvg: p.hAvg,
        })) ?? [];

    const normalizedBucket = backend.bucket.toLowerCase() as MetricsBucket;

    return {
        deviceId: backend.deviceId,
        bucket: normalizedBucket,
        points,
    };
}

export async function fetchRecentSummary(
    envName: string,
    tenantId: string
): Promise<RecentSummary> {
    const res = await fetch(
        `${env.apiBaseUrl}/${encodeURIComponent(envName)}/${encodeURIComponent(
            tenantId
        )}/devices/summary/recent`
    );

    if (!res.ok) {
        throw new Error(`Failed to load recent summary: ${res.status}`);
    }

    return res.json();
}

export async function fetchDroughtSummary(
    envName: string,
    tenantId: string
): Promise<DroughtSummary> {
    const res = await fetch(
        `${env.apiBaseUrl}/${encodeURIComponent(envName)}/${encodeURIComponent(
            tenantId
        )}/devices/summary/drought`
    );

    if (!res.ok) {
        throw new Error(`Failed to load drought summary: ${res.status}`);
    }

    return res.json();
}

export async function fetchDeviceDroughtStreak(
    envName: string,
    tenantId: string,
    deviceId: string
): Promise<DroughtStreak> {
    const res = await fetch(
        `${env.apiBaseUrl}/${encodeURIComponent(envName)}/${encodeURIComponent(
            tenantId
        )}/devices/${encodeURIComponent(deviceId)}/drought`
    );

    if (!res.ok) {
        throw new Error(`Failed to load drought streak for ${deviceId}: ${res.status}`);
    }

    return res.json();
}

