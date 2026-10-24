import { useState } from "react";
import { useSession } from "../core/session";
import { usePages, titleOf } from "../core/pages";
import { integer } from "../core/wire";
import { Button, Card, Check, Choices, QueryState } from "./ui";

export interface DataSelection {
  kind: string;
  id: bigint;
}
const kinds: Array<[string, string]> = [
  ["entries", "我的日常"],
  ["pets", "宠物档案"],
  ["cases", "寻宠求助"],
  ["offers", "有偿信息"],
  ["drafts", "云端草稿"],
  ["media", "自己的照片"],
  ["subscriptions", "区域提醒"],
];

/** 只从当前账号可读记录选择，不要求用户查内部编号。 */
export function DataSelector({
  value,
  onChange,
  disabled,
  assistance,
}: {
  value: DataSelection[];
  onChange: (value: DataSelection[]) => void;
  disabled: boolean;
  assistance: boolean;
}) {
  const owner = useSession().user?.id;
  const [kind, setKind] = useState("entries");
  const options = assistance
    ? [
        ...kinds,
        ["adoptions", "领养事项"] as [string, string],
        ["appointments", "预约安排"] as [string, string],
        ["claims", "认领事项"] as [string, string],
        ["reviews", "核实与举报"] as [string, string],
      ]
    : kinds;
  const activeKind = options.some(([name]) => name === kind) ? kind : "entries";
  const page = usePages(
    activeKind,
    assistance ? {} : { owner_id: owner },
    !!owner,
  );
  return (
    <Card title="选择当前账号的具体记录">
      <p>
        已选 {value.length}{" "}
        项。注销会由服务端重新盘点资料和未结事项，受理不等于已经删除。
      </p>
      {value.map((item) => (
        <Button
          kind="quiet"
          key={`${item.kind}:${item.id}`}
          disabled={disabled}
          onClick={() =>
            onChange(
              value.filter(
                (old) =>
                  !(
                    old.kind === item.kind &&
                    integer(old.id) === integer(item.id)
                  ),
              ),
            )
          }
        >
          移除此选择 ·{" "}
          {options.find(([name]) => name === item.kind)?.[1] ?? "原事项"} #
          {String(item.id)}
        </Button>
      ))}
      <Choices
        label="资料类型"
        value={activeKind}
        onChange={setKind}
        choices={options}
        disabled={disabled}
      />
      <QueryState
        loading={page.loading}
        error={page.error}
        onRetry={page.retry}
        keepContent={!!page.rows.length && !page.denied}
      >
        {page.rows.map((row) => {
          const checked = value.some(
            (item) => item.kind === activeKind && integer(item.id) === row.id,
          );
          return (
            <Check
              key={String(row.id)}
              checked={checked}
              disabled={disabled || (!checked && value.length >= 200)}
              onChange={(selected) =>
                onChange(
                  selected
                    ? [...value, { kind: activeKind, id: row.id }]
                    : value.filter(
                        (item) =>
                          !(
                            item.kind === activeKind &&
                            integer(item.id) === row.id
                          ),
                      ),
                )
              }
            >
              {titleOf(row)} · 版本 {String(row.version)}
            </Check>
          );
        })}
        {page.next ? (
          <Button onClick={page.more} disabled={disabled || page.loading}>
            继续读取当前资料
          </Button>
        ) : null}
        {!page.loading && !page.rows.length ? (
          <p>当前类型没有可选记录。</p>
        ) : null}
      </QueryState>
    </Card>
  );
}
