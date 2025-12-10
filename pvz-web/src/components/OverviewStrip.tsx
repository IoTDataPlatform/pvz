import { useEffect, useState } from 'react';
import { fetchRecentSummary } from '../api/client';
import type { RecentSummary } from '../api/types';
import './OverviewStrip.css';

type Props = {
    envName: string;
    tenantId: string;
};

function formatNumber(value: number | null, digits = 1): string {
    if (value == null) return '—';
    return value.toFixed(digits);
}

const OverviewStrip = ({ envName, tenantId }: Props) => {
    const [summary, setSummary] = useState<RecentSummary | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError(null);
                const data = await fetchRecentSummary(envName, tenantId);
                if (!cancelled) {
                    setSummary(data);
                }
            } catch (e: any) {
                if (!cancelled) {
                    setError(e?.message ?? 'Ошибка загрузки обзора');
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        load();

        const id = setInterval(load, 30_000);

        return () => {
            cancelled = true;
            clearInterval(id);
        };
    }, [envName, tenantId]);

    return (
        <div className="overview-strip">
            {loading && !summary && <span>Обновление данных за последние 10 минут…</span>}
            {error && !summary && <span className="overview-error">Ошибка: {error}</span>}
            {summary && (
                <div className="overview-content">
                    <span className="overview-main">
                        За последние 10 минут:
                    </span>
                    <span className="overview-pill">
                        {summary.totalDevices} датчика
                    </span>
                    <span className="overview-pill overview-pill-online">
                        {summary.onlineDevices} онлайн
                    </span>
                    <span className="overview-pill overview-pill-offline">
                        {summary.offlineDevices} оффлайн
                    </span>
                    <span className="overview-pill">
                        ср. T {formatNumber(summary.avgTemp, 1)}°C
                    </span>
                    <span className="overview-pill">
                        ср. H {formatNumber(summary.avgHumidity, 0)}%
                    </span>
                </div>
            )}
        </div>
    );
};

export default OverviewStrip;
