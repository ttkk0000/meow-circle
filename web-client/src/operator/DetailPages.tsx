import { useState } from "react";
import { Facts, SchemaForm } from "../components/SchemaForm";
import { Button, Check, Choices, Field } from "../components/ui";
import { DataTable, EmptyState, OpsNotice, Panel } from "../operations/ui";
import {
  bodyOf,
  detailView,
  draftKey,
  formError,
  label,
  listView,
  receipt,
  roleFor,
  schemaFor,
  versionOf,
  type Row,
  type View,
} from "./models";
import type { OperatorSession } from "./useOperator";
import { BusinessStatus, OriginalSummary, SummaryPanel } from "./ui";

export function DetailPage({
  session: s,
  go,
}: {
  session: OperatorSession;
  go: (view: View, id?: string) => void;
}) {
  const [relationshipChecked, setRelationshipChecked] = useState(false);
  const detail = s.detail;
  if (!detail)
    return (
      <EmptyState title={s.busy ? "正在读取原事项" : "尚未读取可访问的事项"}>
        请返回原队列，或核对事项编号与本人角色。
      </EmptyState>
    );
  const { kind, id, row } = detail,
    value = receipt(row),
    data = bodyOf(row);
  const values = s.drafts[draftKey(kind, id)] ?? {},
    issue = formError(kind, values, row);
  const terminal =
    kind === "reviews" &&
    !["submitted", "in_review", "needs_info"].includes(String(value.state));
  const locked = s.busy || (!!s.prepared && s.prepared.phase !== "rejected");
  const guardian = kind === "guardian-invites";
  const prepare = (process = false) => {
    if (s.prepare(process)) go("confirm");
  };
  return (
    <>
      <OpsNotice
        title={
          guardian
            ? "关系核实与本人授权分开"
            : kind === "rights-cases"
              ? "受限权利事项"
              : "先核实，再作决定"
        }
      >
        {guardian
          ? "只记录已核实关系的最小证据引用；不会替监护人同意，也不会自动生成授权范围。"
          : "仅处理当前事项与当前版本，保留原陈述。再次读取发现版本变化后，须明确采用新版本。"}
      </OpsNotice>
      <div className="ops-columns">
        <OriginalSummary id={id} row={row}>
          {kind === "reviews"
            ? String(data.reason ?? "未提供原陈述")
            : kind === "rights-cases"
              ? String(row.statement ?? "证据不可读取或未提供")
              : guardian
                ? "不展示未成年人姓名、证件或原始材料。"
                : "逐项核对实际处理进度。"}
        </OriginalSummary>
        <Panel
          title={
            guardian
              ? "核对关系证据"
              : kind === "rights-cases"
                ? "保全与通知"
                : kind === "data-requests"
                  ? "具体保全范围"
                  : "核实与处理"
          }
        >
          <p>
            当前版本 v{String(value.version ?? "未知")} · {roleFor(kind)}
          </p>
          {kind === "reviews" ? (
            <p>
              事项类型：{label(data.type)}；关联{" "}
              {String(data.target_kind ?? "")} / {String(data.target_id ?? "")}
            </p>
          ) : null}
          {kind === "rights-cases" ? (
            <p>
              证据范围：
              {row.evidence_available
                ? "当前允许必要材料核对"
                : "证据受限，不恢复或延长默认权限"}
              ；送达：{label(value.transfer_state)}
            </p>
          ) : null}
          {guardian ? (
            <p>
              关系状态：
              <BusinessStatus value={value.state} />
              ；规则版本：{String(row.policy_version ?? "未提供")}
            </p>
          ) : null}
          {kind === "reviews" || kind === "rights-cases" ? (
            <>
              <Choices
                label="处理方向"
                value={String(values.state ?? "")}
                choices={[
                  ["", "请选择"],
                  ...(kind === "reviews"
                    ? [
                        "in_review",
                        "needs_info",
                        "approved",
                        "rejected",
                        "expired",
                      ]
                    : [
                        "in_review",
                        "needs_info",
                        "accepted",
                        "rejected",
                        "closed",
                      ]
                  ).map((state) => [state, label(state)] as [string, string]),
                ]}
                onChange={(state) => s.edit({ ...values, state })}
                disabled={locked || terminal}
              />
              <Field
                label="必要处理说明"
                value={String(values.summary ?? "")}
                onChange={(summary) => s.edit({ ...values, summary })}
                multiline
                maxLength={kind === "reviews" ? 1000 : 2000}
                disabled={locked || terminal}
              />
            </>
          ) : null}
          <SchemaForm
            name={schemaFor(kind)}
            value={values}
            onChange={s.edit}
            omit={[
              "version",
              "state",
              "summary",
              ...(guardian
                ? [
                    "invite_id",
                    "invite_version",
                    "guardian_id",
                    "child_id",
                    "policy_version",
                  ]
                : []),
            ]}
            disabled={locked || terminal}
          />
          {guardian ? (
            <Check
              checked={relationshipChecked}
              onChange={setRelationshipChecked}
              disabled={locked}
            >
              我已依核验流程核对可信关系证据，仅记录最小引用，不代替监护人授权。
            </Check>
          ) : null}
          {issue ? <p className="ops-caption">{issue}</p> : null}
          {terminal ? (
            <p>此事项当前状态不允许再次处理，可回读核对历史结果。</p>
          ) : null}
          <div className="ops-actions">
            <Button
              kind="primary"
              disabled={
                locked ||
                terminal ||
                !!issue ||
                (guardian && !relationshipChecked)
              }
              onClick={() => prepare()}
            >
              核对本次处理
            </Button>
            <Button
              disabled={s.busy}
              onClick={() => void s.loadDetail(kind, id)}
            >
              重新读取原事项
            </Button>
            <Button disabled={locked} onClick={s.adoptVersion}>
              明确采用已读版本并保留说明
            </Button>
          </div>
        </Panel>
      </div>
      {kind === "rights-cases" && row.evidence_available ? (
        <Panel title="必要权利事实">
          <Facts
            value={{
              target_kind: row.target_kind,
              target_id: row.target_id,
              related_case_id: row.related_case_id,
              right_type: row.right_type,
              statement: row.statement,
              messages: row.messages,
              media_ids: row.media_ids,
            }}
          />
        </Panel>
      ) : null}
      {kind === "data-requests" ? <DataItems row={row} /> : null}
      <div className="ops-actions">
        <Button onClick={() => go(listView(kind))}>返回原队列</Button>
        {kind === "data-requests" ? (
          <Button
            disabled={
              locked ||
              !["processing", "partially_held"].includes(String(row.state))
            }
            onClick={() => prepare(true)}
          >
            处理当前可处理项目
          </Button>
        ) : null}
        <Button onClick={() => go("access")}>查看角色范围</Button>
      </div>
    </>
  );
}
function DataItems({ row }: { row: Row }) {
  const items = (Array.isArray(row.items) ? row.items : []) as Row[];
  return (
    <>
      <OpsNotice title="分类处理与保全">
        本页按指定事项读取。部分删除、索引或供应商排队、受限保全副本分别核对，不表示全部删除完成。外部状态：
        {label(row.external_state)}
      </OpsNotice>
      <DataTable
        title="当前请求的处理项目"
        columns={["记录类型", "记录编号", "处理说明", "状态", "操作"]}
        hideFilters
        empty="当前没有返回可列出的项目"
        emptyDescription="字段删除及外部处理可能独立进行，请核对请求类型、处理版本和外部状态。"
        rows={items.map((item, index) => ({
          key: `${item.kind}/${item.id}/${index}`,
          state: String(item.state),
          search: String(item.id),
          cells: [
            String(item.kind),
            String(item.id),
            "按原请求的指定范围",
            <BusinessStatus value={item.state} />,
            "—",
          ],
        }))}
      />
    </>
  );
}
export function PrivacyPage({
  session: s,
  go,
  id,
}: {
  session: OperatorSession;
  go: (view: View, id?: string) => void;
  id: string;
}) {
  const [input, setInput] = useState(id);
  return (
    <>
      <OpsNotice title="按编号读取本人请求">
        privacy角色按明确事项编号读取。没有全用户资料浏览或任意批量导出入口；保全须有具体依据、负责人和复核时间。
      </OpsNotice>
      <Panel title="读取指定事项">
        <div className="operator-id-form">
          <Field
            label="原事项编号"
            value={input}
            onChange={setInput}
            disabled={s.busy}
          />
          <Button
            disabled={
              s.busy ||
              !s.context?.roles.includes("privacy") ||
              !/^[1-9][0-9]{0,18}$/.test(input)
            }
            onClick={() => go("privacy", input)}
          >
            读取原事项与版本
          </Button>
        </div>
      </Panel>
      {id ? (
        <DetailPage key={`data-requests/${id}`} session={s} go={go} />
      ) : null}
    </>
  );
}
export function ConfirmPage({
  session: s,
  go,
}: {
  session: OperatorSession;
  go: (view: View, id?: string) => void;
}) {
  const pending = s.prepared;
  if (!pending)
    return (
      <EmptyState title="没有待确认操作">
        从原事项读取当前版本并填写必要说明后，再进入提交确认。
      </EmptyState>
    );
  return (
    <>
      <OpsNotice title="提交前最后核对">
        本次只处理一个原事项。确认时会重新读取具名权限及事项版本，前置条件不满足时停止提交。
      </OpsNotice>
      <div className="ops-columns">
        <SummaryPanel
          title="本次操作"
          items={[
            ["原事项", `${pending.kind} / ${pending.id}`],
            ["基准版本", `v${pending.version}`],
            ["动作", label(pending.title)],
            [
              "必要说明",
              String(
                pending.body.summary ??
                  pending.body.evidence_ref ??
                  "按下列明确范围处理",
              ),
            ],
          ]}
        />
        <SummaryPanel
          title="提交范围"
          items={[
            ["当前角色", roleFor(pending.kind)],
            ["限定目的", "仅处理本条事项"],
            ["原始材料", "保留原陈述，不替任何用户同意"],
            [
              "连接状态",
              s.context ? `${s.context.id} · 仍由服务端逐次校验` : "未连接",
            ],
          ]}
        />
      </div>
      <Panel title="本次处理内容">
        <Facts value={pending.body} />
      </Panel>
      <div className="ops-actions">
        <Button
          kind="primary"
          disabled={
            s.busy ||
            !s.context ||
            pending.phase !== "ready" ||
            !s.permits(pending.kind)
          }
          onClick={() => void s.submit()}
        >
          确认提交此版本
        </Button>
        <Button
          disabled={s.busy || pending.phase !== "ready"}
          onClick={() => {
            s.cancelPreparation();
            go(detailView(pending.kind), pending.id);
          }}
        >
          返回继续核实
        </Button>
        <Button onClick={() => go("pending")}>查看结果待核对</Button>
      </div>
    </>
  );
}
export function PendingPage({
  session: s,
  go,
}: {
  session: OperatorSession;
  go: (view: View, id?: string) => void;
}) {
  const [acknowledged, setAcknowledged] = useState(false),
    pending = s.prepared;
  if (!pending)
    return (
      <EmptyState title="没有本机待核对记录">
        记录仅保存在原具名会话内存中；断开后不会从其它账号或历史网址恢复。
      </EmptyState>
    );
  const state =
    pending.phase === "accepted"
      ? "服务已接受，具体进度待回读"
      : pending.phase === "rejected"
        ? "服务端明确拒绝，本次未接受"
        : pending.phase === "ready"
          ? "尚未发出请求"
          : "结果未知";
  return (
    <>
      <OpsNotice title="保留原操作，先核对再决定">
        {pending.explanation ?? "原事项、版本和说明均保留。"}
      </OpsNotice>
      <div className="ops-columns">
        <SummaryPanel
          title="保留本次操作"
          items={[
            ["原事项", `${pending.kind} / ${pending.id}`],
            ["提交前版本", `v${pending.version}`],
            [
              "原说明",
              String(
                pending.body.summary ??
                  pending.body.evidence_ref ??
                  "本次具体处理范围",
              ),
            ],
            ["处理结果", state],
          ]}
        />
        <SummaryPanel
          title="核对顺序"
          items={[
            ["第一步", "重新读取原事项和当前版本"],
            ["第二步", "核对已存在的处理记录，必要时交由负责人核实"],
            [
              "当前回读版本",
              pending.checked
                ? `v${versionOf(pending.checked)} · ${label(receipt(pending.checked).state)}`
                : "尚未回读",
            ],
            ["再次提交", "不会自动重发；当前版本变化不能证明是本次操作造成"],
          ]}
        />
      </div>
      {pending.checked ? (
        <Panel title="当前事项回读">
          <Facts value={pending.checked} />
          <p>此处是当前状态，不是本次原请求的独立回执。</p>
        </Panel>
      ) : null}
      {pending.phase === "unknown" && pending.checked ? (
        <Check
          checked={acknowledged}
          onChange={setAcknowledged}
          disabled={s.busy}
        >
          已人工核对处理记录，结束本机待核对记录；此操作不会把未知结果标为成功。
        </Check>
      ) : null}
      <div className="ops-actions">
        <Button
          kind="primary"
          disabled={s.busy || !s.context}
          onClick={() => void s.checkPending()}
        >
          重新读取原事项
        </Button>
        <Button onClick={() => go(detailView(pending.kind), pending.id)}>
          回到原事项
        </Button>
        <Button
          disabled={
            s.busy ||
            (pending.phase === "unknown" && (!pending.checked || !acknowledged))
          }
          onClick={() => {
            s.finishPending(acknowledged);
            go(listView(pending.kind));
          }}
        >
          结束本机核对记录
        </Button>
      </div>
    </>
  );
}
