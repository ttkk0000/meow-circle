import { mainlandCity } from "../core/cities";

/** 仅在用户同意后加载。JS公开Key与服务端安全密钥分别配置。 */
export interface MapPoint {
  id: string;
  title: string;
  latitude: number;
  longitude: number;
  coordinateSystem: "gcj02" | "wgs84";
}
export interface LngLat {
  getLng(): number;
  getLat(): number;
}
interface MapEvent {
  lnglat: LngLat;
}
interface Overlay {
  on(name: string, handler: () => void): void;
}
export interface AmapMap {
  on(name: "click", handler: (event: MapEvent) => void): void;
  on(name: "complete", handler: () => void): void;
  clearMap(): void;
  add(overlays: Overlay[]): void;
  destroy(): void;
  setZoomAndCenter(zoom: number, center: number[]): void;
}
export interface AmapSDK {
  Map: new (
    container: HTMLElement,
    options: Record<string, unknown>,
  ) => AmapMap;
  Marker: new (options: Record<string, unknown>) => Overlay;
  Circle: new (options: Record<string, unknown>) => Overlay;
  convertFrom(
    position: number[],
    from: string,
    handler: (status: string, result: { locations?: LngLat[] }) => void,
  ): void;
}
declare global {
  interface Window {
    AMap?: AmapSDK;
    _AMapSecurityConfig?: { serviceHost: string };
    __mndAmapReady?: () => void;
  }
}
let loading: Promise<AmapSDK> | undefined;
let mapConsent = false;
const consentListeners = new Set<() => void>();
export function getMapConsent() {
  return mapConsent;
}
export function subscribeMapConsent(listener: () => void) {
  consentListeners.add(listener);
  return () => {
    consentListeners.delete(listener);
  };
}
export function setMapConsent(value: boolean) {
  mapConsent = value;
  consentListeners.forEach((listener) => listener());
}
export function loadAmap(): Promise<AmapSDK> {
  if (window.AMap) return Promise.resolve(window.AMap);
  if (loading) return loading;
  const key = import.meta.env.VITE_MND_AMAP_JS_KEY?.trim();
  if (!key) return Promise.reject(new Error("地图服务尚未配置"));
  loading = new Promise<AmapSDK>((resolve, reject) => {
    window._AMapSecurityConfig = {
      serviceHost: `${location.origin}/maps/amap/_AMapService`,
    };
    const script = document.createElement("script");
    let timeout: ReturnType<typeof setTimeout>;
    const clean = () => {
      clearTimeout(timeout);
      delete window.__mndAmapReady;
      script.onerror = null;
    };
    const fail = () => {
      clean();
      script.remove();
      loading = undefined;
      reject(new Error("地图暂时无法载入"));
    };
    window.__mndAmapReady = () => {
      if (!window.AMap) {
        fail();
        return;
      }
      clean();
      resolve(window.AMap);
    };
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&callback=__mndAmapReady`;
    script.async = true;
    script.onerror = fail;
    timeout = setTimeout(fail, 15_000);
    document.head.append(script);
  });
  return loading;
}
export function coarsePoint(
  latitude: number,
  longitude: number,
): MapPoint | undefined {
  if (
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude) ||
    Math.abs(latitude) > 90 ||
    Math.abs(longitude) > 180
  )
    return;
  return {
    id: "picked",
    title: "本次公开大致区域",
    latitude:
      (Math.sign(latitude) * Math.round(Math.abs(latitude) * 100)) / 100,
    longitude:
      (Math.sign(longitude) * Math.round(Math.abs(longitude) * 100)) / 100,
    coordinateSystem: "gcj02",
  };
}
export function recordPoint(
  id: string,
  data: Record<string, unknown>,
): MapPoint | undefined {
  if (
    data.location_precision === "area" ||
    !["gcj02", "wgs84"].includes(String(data.coordinate_system))
  )
    return;
  const latitude = Number(data.latitude),
    longitude = Number(data.longitude);
  if (!coarsePoint(latitude, longitude)) return;
  return {
    id,
    title: String(data.title ?? "公开大致区域"),
    latitude,
    longitude,
    coordinateSystem: data.coordinate_system as MapPoint["coordinateSystem"],
  };
}
export function cityCenter(city = ""): MapPoint {
  const entry = mainlandCity(city);
  return {
    id: entry ? "center" : "country-center",
    title: entry?.name ?? "中国大陆",
    latitude: entry?.latitude ?? 35,
    longitude: entry?.longitude ?? 105,
    coordinateSystem: "gcj02",
  };
}
export function amapPosition(sdk: AmapSDK, point: MapPoint): Promise<number[]> {
  if (point.coordinateSystem === "gcj02")
    return Promise.resolve([point.longitude, point.latitude]);
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("位置转换超时")), 10_000);
    sdk.convertFrom(
      [point.longitude, point.latitude],
      "gps",
      (status, result) => {
        clearTimeout(timeout);
        const position = result.locations?.[0];
        if (status === "complete" && position)
          resolve([position.getLng(), position.getLat()]);
        else reject(new Error("位置来源暂时无法转换"));
      },
    );
  });
}
