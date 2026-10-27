import { useId, useMemo, useState } from "react";
import { mainlandCity, mainlandProvinces, searchMainlandCities, cityDescription } from "../core/cities";
import { Button, Field } from "./ui";

export function CityField({ value, onChange, label = "城市", disabled = false, allowEmpty = true }: {
  value: string; onChange: (value: string) => void; label?: string; disabled?: boolean; allowEmpty?: boolean;
}) {
  const id = useId();
  const [opened, setOpened] = useState(false);
  const [province, setProvince] = useState("");
  const [search, setSearch] = useState("");
  const selected = mainlandCity(value);
  const rows = useMemo(() => searchMainlandCities(search, province), [search, province]);
  return <fieldset className="city-field" disabled={disabled}>
    <legend>{label}</legend>
    <button type="button" aria-expanded={opened} aria-controls={id} onClick={() => {
      if (!opened) { setSearch(""); setProvince(selected?.provinceCode ?? ""); }
      setOpened(!opened);
    }}>{selected?.name ?? (value || "请选择大陆城市")}</button>
    {opened ? <div id={id}>
      <Field label="搜索城市、拼音或区划代码" value={search} onChange={next => { setSearch(next); setProvince(""); }} disabled={disabled} />
      <label className="field"><span>省份</span><select value={province} onChange={event => setProvince(event.target.value)}>
        <option value="">全部省份</option>
        {mainlandProvinces.map(p => <option key={p.code} value={p.code}>{p.name}</option>)}
      </select></label>
      <p className="muted">支持地级市、县级市、自治州、盟、地区及省直辖地区</p>
      <div className="city-results" role="group" aria-label="城市搜索结果">
        {rows.length ? rows.map(city => <button type="button" key={city.code} aria-pressed={selected?.code === city.code}
          onClick={() => { onChange(city.value); setOpened(false); }}>
          <span>{city.name}{selected?.code === city.code ? " · 已选" : ""}</span><small>{cityDescription(city)}</small>
        </button>) : <p role="status">没有匹配的城市，请更换关键词或省份。</p>}
      </div>
      {allowEmpty ? <Button onClick={() => { onChange(""); setOpened(false); }}>不限城市／暂不填写</Button> : null}
      <Button kind="quiet" onClick={() => setOpened(false)}>关闭</Button>
    </div> : null}
  </fieldset>;
}
