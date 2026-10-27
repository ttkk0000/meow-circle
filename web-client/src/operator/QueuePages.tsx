import { Button } from "../components/ui";
import { DataTable, Metric, OpsNotice } from "../operations/ui";
import {
  detailView,
  label,
  listView,
  receipt,
  time,
  type Kind,
  type Row,
  type View,
} from "./models";
import type { OperatorSession } from "./useOperator";
import { BusinessStatus } from "./ui";

export function queueRows(
  kind: Kind,
  items: Row[],
  go: (view: View, id?: string) => void,
) {
  return items.map((row) => {
    const value = receipt(row),
      id = String(value.id),
      type = String(
        value.type ?? (kind === "guardian-invites" ? "关系核验" : "事项"),
      );
    const state = String(value.state ?? "guardian_relation_pending");
    return {
      key: `${kind}/${id}`,
      state,
      search: `${id} ${label(type)} ${label(state)}`,
      cells: [
        id,
        label(type),
        time(value.created_at ?? value.review_due ?? value.expires_at),
        <BusinessStatus value={state} />,
        <Button kind="quiet" onClick={() => go(detailView(kind), id)}>
          查看详情 →
        </Button>,
      ],
    };
  });
}
export function QueuePage({
  session: s,
  kind,
  go,
}: {
  session: OperatorSession;
  kind: Kind;
  go: (view: View, id?: string) => void;
}) {
  const queue = s.queues[kind],
    title =
      kind === "reviews"
        ? "举报与申诉队列"
        : kind === "rights-cases"
          ? "权利事项队列"
          : "监护邀请与关系证据";
  return (
    <>
      <OpsNotice title={title}>
        {kind === "guardian-invites"
          ? "关系证据与监护人主动授权分别处理；列表不展示原始身份材料。"
          : kind === "rights-cases"
            ? "区分通知、反通知、补充与限制；仅在必要范围读取权利事实。"
            : "保留原陈述，按当前版本逐项核实。核实结论不会直接授予交易资格。"}
      </OpsNotice>
      {!s.permits(kind) ? (
        <OpsNotice title="当前角色未获此类事项授权">
          <Button onClick={() => go("access")}>查看角色范围</Button>
        </OpsNotice>
      ) : (
        <>
          <div className="ops-toolbar">
            <Button disabled={s.busy} onClick={() => void s.readQueue(kind)}>
              重新读取队列
            </Button>
            <span className="ops-caption">筛选仅作用于当前已读事项。</span>
          </div>
          <DataTable
            title={title}
            columns={[
              kind === "guardian-invites" ? "邀请编号" : "事项编号",
              "类型",
              kind === "rights-cases"
                ? "复核时间"
                : kind === "guardian-invites"
                  ? "有效截止"
                  : "收到时间",
              "当前状态",
              "操作",
            ]}
            rows={queueRows(kind, queue.items, go)}
            loading={s.busy}
            more={
              queue.next_cursor > 0n
                ? () => void s.readQueue(kind, true)
                : undefined
            }
            stateName={label}
            empty={queue.loaded ? "当前读取范围没有事项" : "尚未读取队列"}
            emptyDescription={
              queue.loaded
                ? "可重新读取，或继续处理其它已授权事项。"
                : "连接具名会话后读取真实队列。"
            }
          />
        </>
      )}
      <div className="ops-actions">
        <Button onClick={() => go("access")}>查看权限范围</Button>
      </div>
    </>
  );
}
export function Dashboard({
  session: s,
  go,
}: {
  session: OperatorSession;
  go: (view: View, id?: string) => void;
}) {
  const kinds: Kind[] = ["reviews", "rights-cases", "guardian-invites"];
  const representatives = kinds
    .filter(s.permits)
    .flatMap((kind) =>
      s.queues[kind].items.slice(0, 1).map((row) => ({ kind, row })),
    );
  return (
    <>
      <OpsNotice title="今日工作范围">
        按具名角色读取必要事项。下列数字仅代表本会话已载入范围，不代表全站总数。
      </OpsNotice>
      <div className="ops-metrics">
        {kinds.map((kind) => (
          <Metric
            key={kind}
            label={
              kind === "reviews"
                ? "已读举报"
                : kind === "rights-cases"
                  ? "权利事项"
                  : "监护核验"
            }
            value={s.queues[kind].loaded ? s.queues[kind].items.length : "—"}
            hint={s.permits(kind) ? "仅本页已载入事项" : "当前角色未授权"}
          />
        ))}
        <Metric
          label="发布准入"
          value="待核对"
          hint="读取配置不等于正式运营准入"
        />
      </div>
      <div className="ops-actions">
        <Button
          kind="primary"
          disabled={!s.context || s.busy}
          onClick={() =>
            void Promise.allSettled(
              kinds.filter(s.permits).map((kind) => s.readQueue(kind)),
            )
          }
        >
          读取本人工作范围
        </Button>
        <Button onClick={() => go("readiness")}>核对发布准入</Button>
      </div>
      <DataTable
        title="当前角色的待核对事项"
        columns={["事项编号", "事项类型", "时间与期限", "当前状态", "操作"]}
        rows={representatives.flatMap(({ kind, row }) =>
          queueRows(kind, [row], go),
        )}
        hideFilters
        empty="尚无已读事项"
        emptyDescription="先读取本人工作范围，再进入对应队列。此表每类至多展示一条。"
      />
      <div className="ops-actions">
        {kinds.filter(s.permits).map((kind) => (
          <Button key={kind} onClick={() => go(listView(kind))}>
            {kind === "reviews"
              ? "进入举报队列"
              : kind === "rights-cases"
                ? "进入权利队列"
                : "进入监护核验"}
          </Button>
        ))}
      </div>
    </>
  );
}
