import {useEffect, useRef, useState} from "react";
import {api, HttpError} from "../core/api";
import {type Draft, newDraft, saveDraft} from "../core/drafts";
import {key} from "../core/wire";
import {Button, Card, Field, Modal, Notice, RowLink} from "../components/ui";
import {workflowURL} from "./workflows";

interface Receipt {
  id: bigint; version: bigint; state: string; type: string;
}
const receiptStates: Record<string, string> = { submitted: "待核对", in_review: "核对中", needs_info: "待补充", accepted: "已受理", rejected: "未受理", closed: "已结束" };
const receiptState = (value: string) => receiptStates[value] ?? "进度待核对";

/** Carries an assistance request, never an authentication or account-merge instruction. */
export function AuthRecovery({ onBack, onClose, onSupplement }: {
  onBack: () => void; onClose: () => void; onSupplement: (route: string) => void;
}) {
  const [account, setAccount] = useState("");
  const [contact, setContact] = useState("");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [unknown, setUnknown] = useState(false);
  const [receipt, setReceipt] = useState<Receipt>();
  const [error, setError] = useState("");
  const active = useRef(true);
  const working = useRef(false);
  const epoch = useRef(api.epoch);
  const owner = useRef(api.session.user?.id ?? 0n);
  const token = useRef("");
  const saved = useRef<Draft>();
  const current = () => active.current && epoch.current === api.epoch;
  useEffect(() => {
    active.current = true;
    return () => { active.current = false; token.current = ""; saved.current = undefined; };
  }, []);
  function valid(result: Receipt) { return result.id > 0n && result.version > 0n && result.type === "safety"; }
  async function received(result: Receipt) {
    if (!current()) return;
    if (!valid(result)) throw Error("原回执尚未核对");
    setReceipt(result); setUnknown(false); setAccount(""); setContact(""); setReason("");
    const original = saved.current;
    if (original?.access) {
      try { saved.current = await saveDraft({ ...original, uncertain: false, access: { ...original.access, id: result.id } }); }
      catch { if (current()) setError("已收到回执，本机仍保留原请求查询入口；请在待核对事项继续读取。没有重新提交申请。"); }
    }
  }
  async function submit() {
    if (working.current || unknown || receipt || !current()) return;
    const original = account.trim(), address = contact.trim(), text = reason.trim();
    const count = (value: string) => Array.from(value).length;
    if (!original || !address || !text || count(original) > 120 || count(address) > 240 || count(text) > 2000) {
      setError("请填写原账号、可联系的方式和情况说明（分别最多120、240、2000字）"); return;
    }
    working.current = true; setBusy(true); setError("");
    let sent = false;
    try {
      let access = token.current;
      if (!access) {
        const value = await api.request<{ token: string }>("safety/session", { method: "POST", body: {}, owner: owner.current });
        if (!current()) return;
        if (!value.token) throw Error("尚未取得本次协助入口");
        token.current = access = value.token;
      }
      const requestId = key();
      const reference = newDraft(0n, `capability:${requestId}`);
      reference.requestId = requestId;
      reference.uncertain = true;
      reference.access = { type: "safety", token: access, requestId };
      saved.current = await saveDraft(reference);
      if (!current()) return;
      sent = true;
      const result = await api.request<Receipt>("safety/tickets", {
        method: "POST", owner: owner.current, headers: { "X-Safety-Token": access },
        body: { type: "safety", client_request_id: requestId,
          reason: `账号访问恢复协助\n原账号：${original}\n可联系的方式：${address}\n情况说明：${text}` },
      });
      await received(result);
    } catch (failure) {
      if (current()) {
        const uncertain = sent && !(failure instanceof HttpError && [400, 401, 403, 404, 409, 422, 429].includes(failure.status));
        setUnknown(uncertain);
        setError(uncertain ? "申请结果待核对，请读取原回执，不要重复提交" : "暂未提交恢复申请，请核对后重试");
      }
    } finally { working.current = false; if (current()) setBusy(false); }
  }
  async function readReceipt() {
    const reference = saved.current;
    if (working.current || !reference?.access || !current()) return;
    working.current = true; setBusy(true); setError("");
    try {
      const result = await api.request<Receipt>(`safety/requests/${reference.requestId}`, {
        headers: { "X-Safety-Token": reference.access.token }, owner: owner.current,
      });
      await received(result);
    } catch { if (current()) setError("暂未核对到原回执；这不表示申请失败，请稍后继续读取"); }
    finally { working.current = false; if (current()) setBusy(false); }
  }
  const enabled = !busy && !unknown;
  return <Modal title={receipt ? "恢复申请已收到" : "恢复账号访问"} task busy={busy} onClose={onBack}
    actions={<Button kind="primary" disabled={busy || !receipt && !unknown && (!account.trim() || !contact.trim() || !reason.trim())}
      onClick={receipt ? onClose : unknown ? () => void readReceipt() : () => void submit()}>{receipt ? "继续公开浏览" : unknown ? "读取原回执" : "提交恢复申请"}</Button>}>
    <div className="auth-task-content auth-return-task">
      {receipt ? <>
        <h1>请保留回执</h1>
        <Card><dl className="auth-facts"><div><dt>回执</dt><dd>#{String(receipt.id)}</dd></div><div><dt>状态</dt><dd>{receiptState(receipt.state)}</dd></div></dl></Card>
        <Notice title="下一步">可在此补充必要信息。处理前仍需核对原账号，原资料不会被新账号覆盖；收到回执不代表已恢复访问。</Notice>
        <RowLink title="补充恢复信息" detail="只用于本次申请" disabled={busy || !saved.current?.access}
          onClick={() => { const reference = saved.current; if (!working.current && reference?.access) onSupplement(workflowURL("safety", "", 0n,
            { access: reference.requestId, type: "supplement", related: String(receipt.id), version: String(receipt.version) })); }} />
      </> : <>
        <Field label="原账号" value={account} onChange={setAccount} placeholder={"手机号或M&D账号"} disabled={!enabled} />
        <Field label="可联系你的方式" value={contact} onChange={setContact} placeholder="仅用于本次恢复" disabled={!enabled} />
        <Field label="情况说明" value={reason} onChange={setReason} placeholder="简要说明原号码或登录方式为什么不可用" disabled={!enabled} />
        <Notice title="保护你的资料">不会因相同昵称、头像或邮箱直接合并账号。申请只进入协助核对，不直接修改原账号或登录方式。</Notice>
        {unknown ? <Notice title="原申请结果待核对">本机已保留受限查询入口；离开后也可在待核对事项继续查询。</Notice> : null}
      </>}
      {error ? <p className="field-error caption" role="alert">{error}</p> : null}
    </div>
  </Modal>;
}
