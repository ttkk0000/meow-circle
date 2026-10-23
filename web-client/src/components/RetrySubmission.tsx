import { useState } from "react";
import { api, explain } from "../core/api";
import type { Draft } from "../core/drafts";
import { parseWire, integer } from "../core/wire";
import { Facts } from "./SchemaForm";
import { PurposeCheck } from "./PurposeCheck";
import { Button, Check, Modal, Notice } from "./ui";

/** 只在用户查过原回执后使用。始终重用原正文与原编号，重新验证也不产生新业务请求。 */
export function RetrySubmission({
  draft,
  onDone,
}: {
  draft: Draft;
  onDone: () => void;
}) {
  const [opened, setOpened] = useState(false);
  const [body, setBody] = useState<Record<string, unknown>>();
  const [scope, setScope] = useState<Record<string, unknown>>();
  const [grant, setGrant] = useState("");
  const [agreed, setAgreed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const original = draft.original;
  const supported =
    !!original &&
    (original.replayable ||
      /^(adoptions\/\d+\/contracts\/accept|guardians\/\d+\/decision|data-requests|safety\/tickets)$/.test(
        original.path,
      ));
  async function prepare() {
    if (!original) return;
    setBusy(true);
    setError("");
    setAgreed(false);
    setGrant("");
    try {
      const snapshot = parseWire<Record<string, unknown>>(original.body);
      if (draft.access?.type === "data")
        snapshot.query_token = draft.access.token;
      let path: string | undefined;
      if (/^adoptions\/\d+\/contracts\/accept$/.test(original.path))
        path = original.path.replace(/accept$/, "scope");
      if (/^guardians\/\d+\/decision$/.test(original.path) && snapshot.consent)
        path = original.path.replace(/decision$/, "scope");
      if (
        original.path === "data-requests" &&
        snapshot.type !== "matters_assistance"
      )
        path = "data-requests/scope";
      if (
        original.path === "safety/tickets" &&
        api.session.authenticated &&
        ["rights_notice", "counterstatement"].includes(String(snapshot.type))
      )
        path = "safety/scope";
      const scope = path
        ? await api.request<Record<string, unknown>>(path, {
            method: "POST",
            body: snapshot,
          })
        : undefined;
      setScope(scope);
      setBody(snapshot);
      setOpened(true);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  async function submit() {
    if (!original || !body || !agreed || (scope && !grant)) return;
    setBusy(true);
    setError("");
    try {
      const headers =
        draft.access?.type === "safety"
          ? { "X-Safety-Token": draft.access.token }
          : undefined;
      await api.request(original.path, {
        method: original.method,
        body: { ...body, ...(scope ? { reauth_token: grant } : {}) },
        headers,
        owner: integer(draft.ownerKey.split("|").at(-1)),
      });
      setOpened(false);
      if (original.path === "data-requests" && body.type === "close_account")
        api.endSession();
      onDone();
    } catch (e) {
      setError("结果仍待核对：" + explain(e));
    } finally {
      setGrant("");
      setBusy(false);
    }
  }
  return supported ? (
    <>
      <Button onClick={() => void prepare()} disabled={busy}>
        核对原内容并按原编号继续
      </Button>
      {!opened && error ? <Notice title={error} error /> : null}
      {opened ? (
        <Modal
          title="核对原操作"
          busy={busy}
          onClose={() => {
            setOpened(false);
            setGrant("");
          }}
          actions={
            <Button
              kind="primary"
              onClick={() => void submit()}
              disabled={busy || !agreed || (!!scope && !grant)}
            >
              确认按原编号提交
            </Button>
          }
        >
          <Notice title={`原编号 ${draft.requestId}`}>
            未找到回执不代表失败。保留原对象、版本与内容，服务端将再次核对权限和幂等结果。
          </Notice>
          <Facts value={body} />
          {scope ? (
            <PurposeCheck scope={scope} busy={busy} onGrant={setGrant} />
          ) : null}
          <Check checked={agreed} onChange={setAgreed} disabled={busy}>
            我已核对原对象、原内容和本次影响
          </Check>
          {error ? <Notice title={error} error /> : null}
        </Modal>
      ) : null}
    </>
  ) : null;
}
