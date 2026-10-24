import { useEffect, useRef, useState } from "react";
import { usePages } from "../core/pages";
import { api } from "../core/api";
import {
  Button,
  Card,
  Field,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { PageEnd } from "./Records";

export function CoarseMap({
  latitude,
  longitude,
}: {
  latitude: number;
  longitude: number;
}) {
  const template = import.meta.env.VITE_MND_MAP_EMBED_TEMPLATE;
  let source: string | undefined;
  if (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180 &&
    template
  ) {
    try {
      const url = new URL(
        template
          .replaceAll("{lat}", latitude.toFixed(2))
          .replaceAll("{lon}", longitude.toFixed(2))
          .replaceAll("{zoom}", "12"),
      );
      if (url.protocol === "https:" && !url.username && !url.password)
        source = url.href;
    } catch {
      /* 保持真实不可用状态。 */
    }
  }
  return source ? (
    <iframe
      className="coarse-map"
      title="公开大致区域地图"
      src={source}
      loading="lazy"
      referrerPolicy="no-referrer"
      sandbox="allow-scripts allow-same-origin"
    />
  ) : (
    <Notice title="地图暂不可用">
      仍可按城市和公开大致区域查看求助，不影响提供私密线索或认领材料。
    </Notice>
  );
}
export function LocateButton({
  onLocation,
  onError,
}: {
  onLocation: (lat: number, lon: number) => void;
  onError: (message: string) => void;
}) {
  const [busy, setBusy] = useState(false);
  const active = useRef(true);
  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
    };
  }, []);
  return (
    <Button
      disabled={busy}
      onClick={() => {
        if (!navigator.geolocation) {
          onError("当前浏览器不能提供位置，可手动填写大致区域");
          return;
        }
        setBusy(true);
        const epoch = api.epoch;
        navigator.geolocation.getCurrentPosition(
          (position) => {
            if (!active.current || epoch !== api.epoch) return;
            onLocation(
              Math.round(position.coords.latitude * 100) / 100,
              Math.round(position.coords.longitude * 100) / 100,
            );
            setBusy(false);
          },
          () => {
            if (!active.current || epoch !== api.epoch) return;
            onError("没有获得本次位置，可手动填写城市和大致中心");
            setBusy(false);
          },
          { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 },
        );
      }}
    >
      {" "}
      {busy ? "正在读取本次大致位置" : "本次使用大致位置"}
    </Button>
  );
}
export function MapPage() {
  const [city, setCity] = useState("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(0n);
  const lat = latitude === "" ? undefined : Number(latitude);
  const lon = longitude === "" ? undefined : Number(longitude);
  const page = usePages("cases", {
    city,
    latitude: lat,
    longitude: lon,
    radius_km: lat !== undefined && lon !== undefined ? 10 : undefined,
  });
  const record = page.rows.find((row) => row.id === selected) ?? page.rows[0];
  return (
    <TaskPage title="走失互助地图">
      <Notice title="公开位置为大致区域">
        本次位置只在你主动点击后读取，拒绝后仍可手动选择城市；不会持续后台定位。
      </Notice>
      <Field label="城市" value={city} onChange={setCity} />
      <LocateButton
        onLocation={(lat, lon) => {
          setLatitude(String(lat));
          setLongitude(String(lon));
          setError("");
        }}
        onError={setError}
      />
      <div className="search-row">
        <Field
          label="中心纬度（大致）"
          value={latitude}
          onChange={setLatitude}
        />
        <Field
          label="中心经度（大致）"
          value={longitude}
          onChange={setLongitude}
        />
      </div>
      {error ? <Notice title={error} error /> : null}
      {record && record.data.location_precision !== "area" ? (
        <CoarseMap
          latitude={Number(record.data.latitude)}
          longitude={Number(record.data.longitude)}
        />
      ) : !record && lat !== undefined && lon !== undefined ? (
        <CoarseMap latitude={lat} longitude={lon} />
      ) : null}
      {record?.data.location_precision === "area" ? (
        <Notice title="此求助没有地图点">
          {String(record.data.city)} · {String(record.data.area)}
        </Notice>
      ) : null}
      <QueryState
        loading={page.loading}
        error={page.error}
        onRetry={page.retry}
        keepContent={!!page.rows.length && !page.denied}
      >
        {page.rows.map((row) => (
          <Card key={String(row.id)} title={String(row.data.title)}>
            <p>
              {String(row.data.city)} · {String(row.data.area)} ·{" "}
              {row.data.type === "found" ? "拾获" : "走失"}
            </p>
            <div className="actions-row">
              <Button onClick={() => setSelected(row.id)}>
                查看此大致区域
              </Button>
              <RowLink title="查看原求助与进度" to={`/cases/${row.id}`} />
            </div>
          </Card>
        ))}
        <PageEnd page={page} />
      </QueryState>
    </TaskPage>
  );
}
