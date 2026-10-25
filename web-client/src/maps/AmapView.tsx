import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { Button, Notice } from "../components/ui";
import {
  amapPosition,
  cityCenter,
  coarsePoint,
  loadAmap,
  getMapConsent,
  subscribeMapConsent,
  setMapConsent,
  type AmapMap,
  type AmapSDK,
  type MapPoint,
} from "./amap";

const emptyPoints: MapPoint[] = [];
const defaultCenter = cityCenter();
export function AmapView({
  points = emptyPoints,
  selectedId = "",
  center = defaultCenter,
  onSelect,
  onPick,
  disabled = false,
}: {
  points?: MapPoint[];
  selectedId?: string;
  center?: MapPoint;
  onSelect?: (id: string) => void;
  onPick?: (point: MapPoint) => void;
  disabled?: boolean;
}) {
  const container = useRef<HTMLDivElement>(null);
  const map = useRef<AmapMap | null>(null);
  const sdk = useRef<AmapSDK | null>(null);
  const handlers = useRef({ onSelect, onPick, disabled });
  useEffect(() => {
    handlers.current = { onSelect, onPick, disabled };
  }, [onSelect, onPick, disabled]);
  const agreed = useSyncExternalStore(
    subscribeMapConsent,
    getMapConsent,
    () => false,
  );
  const [retry, setRetry] = useState(0);
  const [ready, setReady] = useState(0);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState("");
  const [pointError, setPointError] = useState("");
  const pointsKey = JSON.stringify(points);
  const centerKey = JSON.stringify(center);
  useEffect(() => {
    if (!agreed || !container.current) return;
    let active = true;
    let instance: AmapMap | undefined;
    let timeout: ReturnType<typeof setTimeout> | undefined;
    setError("");
    setLoaded(false);
    void loadAmap()
      .then((api) => {
        if (!active || !container.current) return;
        sdk.current = api;
        instance = new api.Map(container.current, {
          zoom: 12,
          center: [defaultCenter.longitude, defaultCenter.latitude],
          viewMode: "2D",
          resizeEnable: true,
        });
        map.current = instance;
        instance.on("click", ({ lnglat }) => {
          if (!active) return;
          const point = coarsePoint(lnglat.getLat(), lnglat.getLng());
          if (!handlers.current.disabled && point)
            handlers.current.onPick?.(point);
        });
        instance.on("complete", () => {
          if (active) {
            clearTimeout(timeout);
            setLoaded(true);
          }
        });
        timeout = setTimeout(() => {
          if (active) setError("地图未能完成载入，当前输入和列表仍保留");
        }, 15_000);
        setReady((value) => value + 1);
      })
      .catch(() => {
        if (active) setError("地图暂时无法载入，仍可按城市查看或填写文字区域");
      });
    return () => {
      active = false;
      clearTimeout(timeout);
      instance?.destroy();
      if (map.current === instance) map.current = null;
    };
  }, [agreed, retry]);
  useEffect(() => {
    const api = sdk.current,
      instance = map.current;
    if (!agreed || !api || !instance) return;
    let active = true;
    const current = JSON.parse(pointsKey) as MapPoint[];
    const focus =
      current.find((point) => point.id === selectedId) ??
      (JSON.parse(centerKey) as MapPoint);
    instance.clearMap();
    setPointError("");
    void Promise.all(
      current.map(async (point) => {
        try {
          return { point, position: await amapPosition(api, point) };
        } catch {
          return undefined;
        }
      }),
    )
      .then(async (values) => {
        if (!active) return;
        let missing = false;
        const brand = getComputedStyle(container.current!)
          .getPropertyValue("--brand")
          .trim();
        for (const value of values) {
          if (!value) {
            missing = true;
            continue;
          }
          const { point, position } = value;
          const marker = new api.Marker({ position, title: point.title });
          marker.on("click", () => {
            if (active && !handlers.current.disabled)
              handlers.current.onSelect?.(point.id);
          });
          instance.add([marker]);
          if (point.id === selectedId || handlers.current.onPick)
            instance.add([
              new api.Circle({
                center: position,
                radius: 1000,
                strokeColor: brand,
                strokeWeight: 2,
                fillColor: brand,
                fillOpacity: 0.12,
              }),
            ]);
        }
        if (missing)
          setPointError("部分地点暂无法转换，仍可在列表查看文字区域");
        try {
          const position = await amapPosition(api, focus);
          if (active) instance.setZoomAndCenter(12, position);
        } catch {
          if (active) setPointError("地点暂无法转换，请以文字区域为准");
        }
      })
      .catch(() => {
        if (active) setPointError("地图标记暂时无法更新，请以当前列表为准");
      });
    return () => {
      active = false;
    };
  }, [agreed, ready, pointsKey, centerKey, selectedId]);
  if (!import.meta.env.VITE_MND_AMAP_JS_KEY?.trim())
    return (
      <Notice title="地图暂不可用">
        地图服务尚未配置，城市和文字区域仍可使用。
      </Notice>
    );
  if (!agreed)
    return (
      <Notice title="使用高德地图">
        加载地图会向高德发送网络及必要设备信息和当前地图视野，不会自动读取当前位置。
        <a
          href="https://lbs.amap.com/pages/privacy/"
          target="_blank"
          rel="noreferrer"
        >
          阅读高德隐私政策
        </a>
        <Button onClick={() => setMapConsent(true)}>同意并加载地图</Button>
      </Notice>
    );
  return (
    <div className="amap-panel">
      <div
        ref={container}
        className="amap-canvas"
        role="region"
        aria-label={
          onPick ? "轻点选择最后见到的大致位置" : "求助公开大致区域地图"
        }
      />
      {!loaded && !error ? <p role="status">地图正在载入…</p> : null}
      {error ? (
        <Notice title={error}>
          <Button onClick={() => setRetry((value) => value + 1)}>
            重试地图
          </Button>
        </Notice>
      ) : null}
      {pointError ? <p role="status">{pointError}</p> : null}
      {onPick ? (
        <p>轻点地图选择大致位置，只保存公里级公开粗点；也可只填写文字区域。</p>
      ) : null}
      <Button kind="quiet" onClick={() => setMapConsent(false)}>
        停止加载高德地图
      </Button>
    </div>
  );
}
