import { MapContainer, TileLayer, Marker, Tooltip } from 'react-leaflet';
import L, { Icon } from 'leaflet';
import type { DeviceState } from '../api/types';
import './MapView.css';

type MapViewProps = {
    devices: DeviceState[];
    selectedDeviceId: string | null;
    onDeviceClick: (deviceId: string) => void;
};

const sensorOnlineIcon: Icon = L.icon({
    iconUrl: '/icons/sensor-online.png',
    iconSize: [100, 100],
    iconAnchor: [28, 56],
});

const sensorOfflineIcon: Icon = L.icon({
    iconUrl: '/icons/sensor-offline.png',
    iconSize: [100, 100],
    iconAnchor: [28, 56],
});

const NSU_CENTER: [number, number] = [54.8433, 83.0931];

function formatShort(device: DeviceState): string {
    const t = device.t != null ? `${device.t.toFixed(1)}°C` : 't —';
    const h = device.h != null ? `${device.h.toFixed(0)}%` : 'h —';
    return `${t} / ${h}`;
}

function formatRssi(device: DeviceState): string {
    if (device.rssi == null) return 'RSSI —';
    return `RSSI ${device.rssi} dBm`;
}

const MapView = ({ devices, selectedDeviceId, onDeviceClick }: MapViewProps) => {
    return (
        <div className="map-wrapper">
            <MapContainer
                center={NSU_CENTER}
                zoom={17}
                className="map-container"
                attributionControl={false}
            >
                <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />

                {devices
                    .filter((d) => d.lat != null && d.lon != null)
                    .map((device) => (
                        <Marker
                            key={device.deviceId}
                            position={[device.lat!, device.lon!]}
                            icon={
                                device.online === false || device.online === null
                                    ? sensorOfflineIcon
                                    : sensorOnlineIcon
                            }
                            eventHandlers={{
                                click: () => onDeviceClick(device.deviceId),
                            }}
                            opacity={
                                selectedDeviceId &&
                                selectedDeviceId !== device.deviceId
                                    ? 0.6
                                    : 1
                            }
                        >
                            <Tooltip
                                permanent
                                direction="top"
                                offset={[0, -28]}
                                className="device-tooltip"
                            >
                                <div className="device-tooltip-id">
                                    {device.deviceId}
                                </div>
                                <div className="device-tooltip-values">
                                    {formatShort(device)}
                                </div>
                                <div className="device-tooltip-rssi">
                                    {formatRssi(device)}
                                </div>
                            </Tooltip>
                        </Marker>
                    ))}
            </MapContainer>
        </div>
    );
};

export default MapView;
