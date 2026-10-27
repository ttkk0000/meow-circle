import type { ReactNode } from "react";
import { Summary, Panel } from "../operations/ui";
import { label, receipt, type Row } from "./models";

export function BusinessStatus({ value }: { value: unknown }) {
  const state = String(value ?? "");
  const tone = ["rejected", "expired"].includes(state)
    ? "danger"
    : [
          "submitted",
          "needs_info",
          "pending",
          "held",
          "partially_held",
          "draft",
        ].includes(state)
      ? "warning"
      : ["approved", "accepted", "completed", "erased", "published"].includes(
            state,
          )
        ? "success"
        : ["in_review", "processing", "guardian_relation_verified"].includes(
              state,
            )
          ? "brand"
          : "neutral";
  return <span className={`ops-status ${tone}`}>{label(value)}</span>;
}
export function SummaryPanel({
  title,
  items,
}: {
  title: string;
  items: Array<[string, ReactNode]>;
}) {
  return (
    <Panel title={title}>
      <dl>
        {items.map(([key, value]) => (
          <Summary key={key} label={key}>
            {value}
          </Summary>
        ))}
      </dl>
    </Panel>
  );
}
export function OriginalSummary({
  row,
  id,
  children,
}: {
  row: Row;
  id: string;
  children?: ReactNode;
}) {
  const value = receipt(row);
  return (
    <SummaryPanel
      title="原事项摘要"
      items={[
        ["事项编号", id],
        ["当前状态", <BusinessStatus value={value.state} />],
        ["当前版本", `v${value.version ?? "未知"}`],
        ["限定目的", "仅当前具名角色处理本事项"],
        ["原始说明", children ?? "按必要范围核对，保留原陈述。"],
      ]}
    />
  );
}
