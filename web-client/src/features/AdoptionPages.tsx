import type { RecordData } from "../core/api";
import {
  currentAdoptionContract,
  adoptionProgressTitle,
} from "../core/adoption";
import { integer, money } from "../core/wire";
import { Card, Notice, RowLink } from "../components/ui";

export function AdoptionProgress({
  row,
  actor,
}: {
  row: RecordData;
  actor: bigint;
}) {
  const data = row.data,
    contract = currentAdoptionContract(row),
    terms = contract?.terms as Record<string, unknown> | undefined;
  const mine = (value: unknown) =>
    Array.isArray(value) && value.some((v) => integer(v) === actor);
  return (
    <>
      <Notice title={adoptionProgressTitle(row, actor)}>
        {row.state === "waitlisted"
          ? "申请保留为候补，不是已被拒绝；不会自动替你接受新条件，也不披露其它家庭资料。"
          : row.state === "trial_proposed"
            ? "双方确认同一试养安排后再开始，不代替另一方同意。"
            : row.state === "handover_pending"
              ? "双方分别确认本人实际交接后才进入完成状态。"
              : row.state === "completed"
                ? "30天私密照护更新仅向本次送养人和获授权的照护者共享。"
                : "仅本次申请双方可见；不索取无关证件或完整家庭地址。"}
      </Notice>
      <Card title="本次申请">
        <p>
          申请 #{String(row.id)} · 宠物 #{String(data.pet_id)} · 第
          {String(data.adoption_cycle ?? "未知")}轮
        </p>
        <p className="prose">{String(data.message ?? "")}</p>
        {data.supplement ? (
          <p className="prose">补充材料：{String(data.supplement)}</p>
        ) : null}
        {data.note ? (
          <p className="prose">最新说明：{String(data.note)}</p>
        ) : null}
      </Card>
      {["trial_proposed", "trial_active"].includes(row.state) ? (
        <Card title="本次试养安排">
          <dl className="appointment-facts">
            <div>
              <dt>截止</dt>
              <dd>{String(data.trial_until ?? "待核对")}</dd>
            </div>
            <div>
              <dt>照护</dt>
              <dd>{String(data.trial_terms ?? "待核对")}</dd>
            </div>
            <div>
              <dt>费用与退出</dt>
              <dd>
                {terms
                  ? money(terms.amount_cents) +
                    " · " +
                    String(terms.payee) +
                    " · " +
                    String(terms.return_terms)
                  : "先核对当前同一生效协议"}
              </dd>
            </div>
            <div>
              <dt>本人确认</dt>
              <dd>{mine(data.trial_confirmations) ? "已确认" : "待确认"}</dd>
            </div>
          </dl>
        </Card>
      ) : null}
      {[
        "approved",
        "trial_proposed",
        "trial_active",
        "handover_pending",
      ].includes(row.state) && !contract ? (
        <Notice title="先核对同一生效协议">
          双方尚未接受同一当前协议，或有新协议提案待处理；不能据此开始试养或交接。
        </Notice>
      ) : null}
      <RowLink
        title="查看当前宠物介绍"
        detail="资料仍由服务端重新核对权限"
        to={"/pets/" + String(data.pet_id)}
      />
    </>
  );
}
export function AdoptionActionReview({
  row,
  action,
}: {
  row: RecordData;
  action: string;
}) {
  const contract = currentAdoptionContract(row),
    terms = contract?.terms as Record<string, unknown> | undefined;
  if (!["propose_trial", "confirm_trial", "confirm_handover"].includes(action))
    return null;
  return (
    <>
      <Notice
        title={
          action === "confirm_handover"
            ? "只确认本人实际交接"
            : "双方核对同一协议与试养安排"
        }
      >
        {action === "confirm_handover"
          ? "确认本人已经安全完成交接并收妥约定资料与用品，不替对方确认，也不把同意申请当作档案转移。"
          : "试养责任、期限及退出方式来自当前生效协议；修改须先提出协议新版本并由双方接受。"}
      </Notice>
      <Card
        title={
          contract
            ? "生效协议 V" + String(contract.number)
            : "当前无可用生效协议"
        }
      >
        {terms ? (
          <dl className="appointment-facts">
            {[
              ["截止", String(terms.trial_until ?? "未约定试养期限")],
              ["照护责任", String(terms.responsibilities)],
              [
                "费用与收款方",
                money(terms.amount_cents) + " · " + String(terms.payee),
              ],
              ["退出与安全接回", String(terms.return_terms)],
            ].map(([name, value]) => (
              <div key={name}>
                <dt>{name}</dt>
                <dd>{value}</dd>
              </div>
            ))}
          </dl>
        ) : (
          <p>先核对协议与当前版本。</p>
        )}
      </Card>
    </>
  );
}
