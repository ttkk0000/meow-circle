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
import { workspaceFor, workspaceQueues } from "./roleWorkspaces";

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
  workspace,
}: {
  session: OperatorSession;
  go: (view: View, id?: string) => void;
  workspace?: string;
}) {
  const profile = workspaceFor(workspace);
  const kinds: Kind[] = workspaceQueues(workspace);
  if (!profile)
    return (
      <OpsNotice
        title={s.context ? "当前身份无人工处理工作区" : "连接后读取本人岗位"}
      >
        worker与证据读取身份不自动获得人工处理权限。
        <Button onClick={() => go("access")}>查看角色与用途</Button>
      </OpsNotice>
    );
  if (!kinds.length) {
    const links: Array<[View, string]> =
      workspace === "privacy"
        ? [["privacy", "按编号读取资料请求"]]
        : workspace === "authority_verifier"
          ? [
              ["authority", "核对授权对象与证据"],
              ["readiness", "核对公开准入配置"],
            ]
          : [
              ["revenue", "读取实际收支台账"],
              ["received", "登记实际收款"],
              ["refund", "登记关联原收款的退款"],
            ];
    return (
      <>
        <OpsNotice title={profile.home}>
          仅处理本岗的明确对象与范围；尚未读取的资料保持未知，不编造全站待办数量。
        </OpsNotice>
        <div className="ops-metrics">
          <Metric label="当前岗位" value="已授权" hint={profile.label} />
          <Metric
            label="读取范围"
            value={
              workspace === "privacy"
                ? "按请求编号"
                : workspace === "authority_verifier"
                  ? "按对象与范围"
                  : "实际台账"
            }
            hint="不提供全用户私密目录"
          />
        </div>
        <div className="ops-actions">
          {links.map(([view, label]) => (
            <Button key={view} onClick={() => go(view)}>
              {label}
            </Button>
          ))}
        </div>
      </>
    );
  }
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
        {kinds.filter(s.permits).map((kind) => (
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
        {workspace === "moderator" ? (
          <Metric
            label="发布准入"
            value="待核对"
            hint="读取配置不等于正式运营准入"
          />
        ) : null}
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
        {workspace === "moderator" ? (
          <Button onClick={() => go("readiness")}>核对发布准入</Button>
        ) : (
          <Button onClick={() => go("ageEvidence")}>登记可信年龄证据</Button>
        )}
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
