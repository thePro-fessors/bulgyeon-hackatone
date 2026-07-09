'use client';

import { useEffect, useRef } from 'react';

export default function MapComponent() {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<any>(null);
  const markerGroupRef = useRef<any>(null);
  const updateIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;

    const initMap = async () => {
      try {
        if (!mapContainerRef.current || !mountedRef.current) return;

        const L = (await import('leaflet')).default;

        // Prevent re-initialization
        if (mapRef.current) return;

        // Fix leaflet marker icon issue
        delete (L.Icon.Default.prototype as any)._getIconUrl;
        L.Icon.Default.mergeOptions({
          iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
          iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
          shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        });

        // Initialize map (Busan center)
        const map = L.map(mapContainerRef.current).setView([35.1595, 129.0430], 12);
        mapRef.current = map;

        // Add CartoDB Dark Matter tiles
        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
          attribution: '&copy; OpenStreetMap contributors &copy; CARTO'
        }).addTo(map);

        const markerGroup = L.layerGroup().addTo(map);
        markerGroupRef.current = markerGroup;

        console.log('✓ Map initialized');
      } catch (err) {
        console.error('Map init error:', err);
      }
    };

    initMap();

    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Update locations every 5 seconds
  useEffect(() => {
    if (!mapRef.current || !markerGroupRef.current) return;

    const updateLocations = async () => {
      try {
        if (!mountedRef.current) return;

        const L = (await import('leaflet')).default;
        const res = await fetch('/api/location/list');
        const data = await res.json();

        console.log('📍 API Response:', data);

        if (data.success && data.locations && markerGroupRef.current && mountedRef.current) {
          console.log(`📍 Found ${data.locations.length} locations`);
          markerGroupRef.current.clearLayers();
          const bounds: [number, number][] = [];

          data.locations.forEach((loc: any) => {
            console.log(`📍 Processing location:`, loc);
            if (loc.latitude && loc.longitude) {
              L.marker([loc.latitude, loc.longitude])
                .addTo(markerGroupRef.current)
                .bindPopup(`
                  <div style="color: #000; font-family: sans-serif; font-size: 13px; line-height: 1.5; padding: 4px;">
                    <b style="font-size: 14px; color: #000;">👤 ${loc.name}</b><br/>
                    <span style="color: #666; font-size: 11px;">사원ID: ${loc.employeeId}</span><br/>
                    <span style="color: #333;">위도: ${loc.latitude.toFixed(5)}</span><br/>
                    <span style="color: #333;">경도: ${loc.longitude.toFixed(5)}</span><br/>
                    <span style="color: #00E676; font-weight: bold; font-size: 11px;">📡 최종 보고: ${new Date(loc.timestamp).toLocaleTimeString()}</span>
                  </div>
                `);
              bounds.push([loc.latitude, loc.longitude]);
            }
          });

          if (bounds.length > 0 && mapRef.current && mountedRef.current) {
            mapRef.current.fitBounds(bounds, { padding: [50, 50], maxZoom: 16 });
          }
        }
      } catch (err) {
        console.error('Update error:', err);
      }
    };

    updateLocations();
    updateIntervalRef.current = setInterval(updateLocations, 5000);

    return () => {
      if (updateIntervalRef.current) {
        clearInterval(updateIntervalRef.current);
      }
    };
  }, []);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
      if (updateIntervalRef.current) {
        clearInterval(updateIntervalRef.current);
      }
      if (mapRef.current) {
        mapRef.current.remove();
      }
    };
  }, []);

  return (
    <div
      ref={mapContainerRef}
      style={{
        height: '400px',
        borderRadius: '12px',
        border: '1px solid var(--border-color)',
        backgroundColor: 'rgba(0, 0, 0, 0.4)',
        width: '100%',
      }}
    />
  );
}
