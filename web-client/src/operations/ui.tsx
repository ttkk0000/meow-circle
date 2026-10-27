import { useId, useState, type ReactNode } from "react";
import { Button, Card, Icon, Notice } from "../components/ui";
import { stateLabel } from "./models";

export function OpsIcon({ name }: { name: string }) {
  return <Icon name={name} collection="ops" />;
}
export function Status({
  state,
  children,
}: {
  state: string;
  children?: ReactNode;
}) {
  const tone = [
    "failed",
    "unreachable",
    "mismatch",
    "source_missing",
    "source_unavailable",
    "blocked",
  ].includes(state)
    ? "danger"
    : ["retry", "pending", "missing", "reference_only", "baseline"].includes(
          state,
        )
      ? "warning"
      : [
            "verified",
            "reachable",
            "acknowledged",
            "batch_complete",
            "resolved",
          ].includes(state)
        ? "success"
        : ["investigating", "observing", "responding"].includes(state)
          ? "brand"
          : "neutral";
  return (
    <span className={`ops-status ${tone}`}>
      {children ?? stateLabel(state)}
    </span>
  );
}
export function OpsNotice({
  title,
  children,
  error = false,
}: {
  title: string;
  children?: ReactNode;
  error?: boolean;
}) {
  return (
    <div className={`ops-notice ${error ? "ops-notice-error" : ""}`}>
      <OpsIcon name="circle-alert" />
      <Notice title={title} error={error}>
        {children}
      </Notice>
    </div>
  );
}
export function Panel({
  title,
  description,
  children,
  className = "",
}: {
  title: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`ops-panel ${className}`}>
      <Card title={title}>
        {description ? (
          <p className="ops-panel-description">{description}</p>
        ) : null}
        {children}
      </Card>
    </div>
  );
}
export function Metric({
  label,
  value,
  hint,
}: {
  label: string;
  value: ReactNode;
  hint: string;
}) {
  return (
    <div className="ops-metric">
      <Card>
        <p className="muted">{label}</p>
        <strong>{value}</strong>
        <small>{hint}</small>
      </Card>
    </div>
  );
}
export function Summary({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="ops-summary">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
export function EmptyState({
  title,
  children,
}: {
  title: string;
  children?: ReactNode;
}) {
  return (
    <div className="ops-empty" role="status">
      <OpsIcon name="sliders-horizontal" />
      <strong>{title}</strong>
      <div>{children}</div>
    </div>
  );
}
export interface TableRow {
  key: string;
  cells: ReactNode[];
  search: string;
  state: string;
  level?: string;
}
export function DataTable({
  title,
  columns,
  rows,
  placeholder = "搜索名称、编号或关键词",
  dense = false,
  more,
  loading = false,
  empty = "当前没有记录",
  hideFilters = false,
  stateName = stateLabel,
  emptyDescription,
}: {
  title: string;
  columns: string[];
  rows: TableRow[];
  placeholder?: string;
  dense?: boolean;
  more?: () => void;
  loading?: boolean;
  empty?: string;
  hideFilters?: boolean;
  stateName?: (value: string) => string;
  emptyDescription?: string;
}) {
  const [query, setQuery] = useState(""),
    [state, setState] = useState(""),
    [level, setLevel] = useState("");
  const id = useId();
  const states = [...new Set(rows.map((row) => row.state))];
  const levels = [
    ...new Set(rows.flatMap((row) => (row.level ? [row.level] : []))),
  ];
  const needle = query.trim().toLocaleLowerCase();
  const filtered = rows.filter(
    (row) =>
      (!state || row.state === state) &&
      (!level || row.level === level) &&
      (!needle || row.search.toLocaleLowerCase().includes(needle)),
  );
  return (
    <div className="ops-stack ops-table-group">
      {!hideFilters ? (
        <div className="ops-toolbar">
          <label className="ops-search" htmlFor={`${id}-search`}>
            <OpsIcon name="search" />
            <span className="sr-only">{placeholder}</span>
            <input
              id={`${id}-search`}
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={placeholder}
              maxLength={160}
            />
          </label>
          <label className="ops-select">
            <span>状态</span>
            <select
              aria-label="按状态筛选"
              value={state}
              onChange={(e) => setState(e.target.value)}
            >
              <option value="">全部</option>
              {states.map((value) => (
                <option key={value} value={value}>
                  {stateName(value)}
                </option>
              ))}
            </select>
          </label>
          {levels.length ? (
            <label className="ops-select">
              <span>等级</span>
              <select
                aria-label="按等级筛选"
                value={level}
                onChange={(e) => setLevel(e.target.value)}
              >
                <option value="">全部</option>
                {levels.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
          ) : null}
          <span className="ops-caption">当前已读 {rows.length} 条</span>
          <Button
            onClick={() => {
              setQuery("");
              setState("");
              setLevel("");
            }}
          >
            重置筛选
          </Button>
        </div>
      ) : null}
      <Panel
        title={title}
        className={`ops-data-panel ${dense ? "ops-dense" : ""}`}
      >
        <div
          className="ops-table-scroll"
          role="region"
          tabIndex={0}
          aria-label={title}
        >
          <table>
            <caption className="sr-only">{title}</caption>
            <colgroup>
              {columns.map((_, i) => (
                <col key={i} />
              ))}
            </colgroup>
            <thead>
              <tr>
                {columns.map((column, i) => (
                  <th key={i} scope="col">
                    {column}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((row) => (
                <tr key={row.key}>
                  {row.cells.map((cell, i) => (
                    <td key={i}>{cell}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!filtered.length ? (
          <EmptyState title={rows.length ? "没有符合筛选的记录" : empty}>
            {rows.length
              ? "调整关键词或重置筛选后继续。"
              : (emptyDescription ??
                "空列表不代表系统健康；请核对采集来源与权限。")}
          </EmptyState>
        ) : null}
        {more ? (
          <div className="ops-page-end">
            <Button disabled={loading} onClick={more}>
              {loading ? "正在读取…" : "读取更早记录"}
            </Button>
            <small>筛选作用于当前已读取记录。</small>
          </div>
        ) : null}
      </Panel>
    </div>
  );
}
