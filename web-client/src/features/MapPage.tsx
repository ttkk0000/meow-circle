import { CityField } from "../components/CityField";
import { useMemo, useState } from "react";
import { usePages } from "../core/pages";
import {
  Button,
  Card,
  Choices,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { PageEnd } from "./Records";
import { AmapView } from "../maps/AmapView";
import { cityCenter, recordPoint, type MapPoint } from "../maps/amap";

export function CoarseMap({
  latitude,
  longitude,
  coordinateSystem = "",
}: {
  latitude: number;
  longitude: number;
  coordinateSystem?: string;
}) {
  const point = recordPoint("detail", {
    latitude,
    longitude,
    coordinate_system: coordinateSystem,
  });
  return point ? (
    <AmapView points={[point]} selectedId={point.id} center={point} />
  ) : (
    <Notice title="此记录暂不显示地图点">
      原坐标来源尚未确认，请以城市和文字区域为准。
    </Notice>
  );
}
export function MapPage() {
  const [city, setCity] = useState("");
  const [kind, setKind] = useState("lost");
  const [selected, setSelected] = useState("");
  const [picking, setPicking] = useState(false);
  const [picked, setPicked] = useState<MapPoint>();
  const [copyNotice, setCopyNotice] = useState("");
  const page = usePages("cases", { city, state: "active" });
  const rows = useMemo(
    () => page.rows.filter((row) => kind === "all" || row.data.type === kind),
    [page.rows, kind],
  );
  const points = useMemo(
    () =>
      rows
        .map((row) => recordPoint(String(row.id), row.data))
        .filter((point): point is MapPoint => !!point),
    [rows],
  );
  const center =
    points.find((point) => point.id === selected) ??
    points[0] ??
    cityCenter(city);
  return (
    <TaskPage
      title="走失互助地图"
      actions={<RowLink to="/edit/case">发布走失／拾获求助</RowLink>}
    >
      <CityField
        label="城市"
        value={city}
        onChange={(value) => {
          setCity(value);
          setSelected("");
        }}
      />
      <Choices
        label="求助类型"
        value={kind}
        choices={[
          ["lost", "走失"],
          ["found", "拾获"],
          ["all", "全部"],
        ]}
        onChange={(value) => {
          setKind(value);
          setSelected("");
        }}
      />
      <AmapView
        points={points}
        selectedId={selected}
        center={center}
        onSelect={setSelected}
        onPick={picking ? setPicked : undefined}
      />
      <Button
        pressed={picking}
        onClick={() => {
          setPicking(!picking);
          setCopyNotice("");
        }}
      >
        {picking ? "结束选点" : "选取公开大致区域"}
      </Button>
      {picking ? (
        <Notice title="只选择公开粗点">
          轻点地图选取GCJ-02大致区域；此操作不会自动发布求助或读取当前位置。
        </Notice>
      ) : null}
      {picked ? (
        <Card title="本次公开大致区域">
          <p>
            GCJ-02 · 纬度 {picked.latitude.toFixed(2)} · 经度{" "}
            {picked.longitude.toFixed(2)}
          </p>
          <Button
            onClick={() => {
              void navigator.clipboard
                .writeText(
                  `${picked.latitude.toFixed(2)}, ${picked.longitude.toFixed(2)}`,
                )
                .then(() => setCopyNotice("坐标已复制，可粘贴到桌面求助表单"))
                .catch(() =>
                  setCopyNotice("当前浏览器未允许复制，请手动复制上方坐标"),
                );
            }}
          >
            复制GCJ-02坐标
          </Button>
          {copyNotice ? <p role="status">{copyNotice}</p> : null}
        </Card>
      ) : null}
      <Notice title="地点是事件发生的大致区域">
        只显示已载入、坐标来源明确的公开粗点，不代表猫咪实时位置。无坐标和来源待确认的记录仍可在列表查看。
      </Notice>
      <QueryState
        loading={page.loading}
        error={page.error}
        failure={page.failure}
        readAt={page.readAt}
        onRetry={() => void page.reload()}
      />
      {!page.loading && !rows.length ? <p>当前条件暂无已载入求助</p> : null}
      {rows.map((row) => (
        <Card key={String(row.id)} title={String(row.data.title)}>
          <p>
            {String(row.data.city)} · {String(row.data.area)}
          </p>
          {points.some((point) => point.id === String(row.id)) ? (
            <Button
              pressed={selected === String(row.id)}
              onClick={() => setSelected(String(row.id))}
            >
              {selected === String(row.id) ? "已选中此区域" : "查看此大致区域"}
            </Button>
          ) : (
            <p>仅提供文字区域，或地点来源待确认。</p>
          )}
          <RowLink to={`/r/cases/${row.id}`}>查看原求助与进度</RowLink>
        </Card>
      ))}
      <PageEnd page={page} />
    </TaskPage>
  );
}
