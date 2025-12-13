import {useEffect, useState} from 'react';
import {fetchDroughtSummary, fetchRecentSummary} from '../api/client';
import type {DroughtSummary, RecentSummary} from '../api/types';
import './OverviewStrip.css';

type Props = {
    envName: string;
    tenantId: string;
};

function formatNumber(value: number | null, digits = 1): string {
    if (value == null) return '—';
    return value.toFixed(digits);
}

const OverviewStrip = ({envName, tenantId}: Props) => {
    const [summary, setSummary] = useState<RecentSummary | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [drought, setDrought] = useState<DroughtSummary | null>(null);

    const DROUGHT_DAYS_RED = 10;

    function getDroughtLevel(drought: DroughtSummary | null): 'ok' | 'warn' | 'bad' {
        if (!drought || drought.devicesInDrought <= 0) return 'ok';
        const maxDays = drought.maxStreakDays ?? 0;
        if (maxDays >= DROUGHT_DAYS_RED) return 'bad';
        return 'warn';
    }

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError(null);

                const [summaryData, droughtData] = await Promise.all([
                    fetchRecentSummary(envName, tenantId),
                    fetchDroughtSummary(envName, tenantId),
                ]);

                if (!cancelled) {
                    setSummary(summaryData);
                    setDrought(droughtData);
                }
            } catch (e: any) {
                if (!cancelled) setError(e?.message ?? 'Ошибка загрузки обзора');
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        const id = setInterval(load, 30_000);
        return () => {
            cancelled = true;
            clearInterval(id);
        };
    }, [envName, tenantId]);

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
                    {(() => {
                        const level = getDroughtLevel(drought);
                        const droughtPillClass = `overview-pill overview-pill-drought overview-pill-drought-${level}`;
                        return (
                            <>
                                <span className={droughtPillClass}>
                                    в засухе {drought?.devicesInDrought ?? '—'}
                                </span>
                                <span className={droughtPillClass}>
                                    макс засуха {formatNumber(drought?.maxStreakDays ?? null, 2)}д
                                </span>
                            </>
                        );
                    })()}
                </div>
            )}
        </div>
    );
};

export default OverviewStrip;
