import { useEffect, useState } from 'react';
import { fetchDevices } from './api/client';
import type { DeviceState } from './api/types';
import MapView from './components/MapView';
import DeviceDetails from './components/DeviceDetails';
import DeviceCharts from './components/DeviceCharts';
import OverviewStrip from './components/OverviewStrip';
import './App.css';

const DEFAULT_ENV = 'prod';
const DEFAULT_TENANT = 'tenant-1';

function App() {
    const [devices, setDevices] = useState<DeviceState[]>([]);
    const [initialLoading, setInitialLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;

        async function loadDevices(first: boolean) {
            try {
                if (first) {
                    setInitialLoading(true);
                    setError(null);
                }

                const res = await fetchDevices(DEFAULT_ENV, DEFAULT_TENANT);
                if (cancelled) return;

                setDevices(res);

                if (res.length > 0) {
                    const stillExists = res.some(
                        (d) => d.deviceId === selectedDeviceId
                    );
                    if (!stillExists) {
                        setSelectedDeviceId(res[0].deviceId);
                    }
                } else {
                    setSelectedDeviceId(null);
                }
            } catch (e: any) {
                if (!cancelled) {
                    setError(e?.message ?? 'Ошибка загрузки устройств');
                }
            } finally {
                if (first && !cancelled) {
                    setInitialLoading(false);
                }
            }
        }

        loadDevices(true);

        const id = setInterval(() => loadDevices(false), 10_000);

        return () => {
            cancelled = true;
            clearInterval(id);
        };
    }, [selectedDeviceId]);

    const selectedDevice =
        devices.find((d) => d.deviceId === selectedDeviceId) ?? null;

    return (
        <div className="app">
            <OverviewStrip envName={DEFAULT_ENV} tenantId={DEFAULT_TENANT} />

            <main className="app-main">
                <div className="map-panel">
                    {initialLoading && (
                        <div className="status">Загрузка датчиков…</div>
                    )}
                    {error && (
                        <div className="status status-error">Ошибка: {error}</div>
                    )}
                    <MapView
                        devices={devices}
                        selectedDeviceId={selectedDeviceId}
                        onDeviceClick={setSelectedDeviceId}
                    />
                </div>

                <aside className="side-panel">
                    <DeviceDetails device={selectedDevice} />
                    <DeviceCharts
                        envName={DEFAULT_ENV}
                        tenantId={DEFAULT_TENANT}
                        deviceId={selectedDeviceId}
                    />
                </aside>
            </main>

            <footer className="app-footer">
                <small>
                    Данные карты: © OpenStreetMap contributors · Рендеринг: Leaflet
                </small>
            </footer>
        </div>
    );
}

export default App;
