import { useEffect, useState } from "react";
import { api, explain, HttpError } from "../core/api";
import { useSession } from "../core/session";
import {
  newDraft,
  readDraft,
  saveDraft,
  clearDraft,
  type Draft,
} from "../core/drafts";
import { key, encodeWire } from "../core/wire";
import { reload } from "../core/query";
import { navigate } from "../core/router";
import { Button, Check, Modal, Notice } from "./ui";

interface Snapshot {
  snapshot_id: bigint;
  version: bigint;
  count: bigint;
  muted_count: bigint;
  expires_at: string;
}
interface Result {
  applied_count: bigint;
  skipped_count: bigint;
}

/** 先取得固定范围，再主动确认；新到消息和业务待办不会被顺带处理。 */
export function InboxReadAll() {
  const owner = useSession().user?.id ?? 0n;
  const [snapshot, setSnapshot] = useState<Snapshot>();
  const [busy, setBusy] = useState(false),
    [unknown, setUnknown] = useState(false),
    [agreed, setAgreed] = useState(false);
  const [error, setError] = useState(""),
    [notice, setNotice] = useState("");
  const route = "/messages?read-all=1";
  useEffect(() => {
    let active = true;
    void readDraft(owner, route)
      .then((d) => {
        if (active) setUnknown(!!d?.uncertain);
      })
      .catch((e) => {
        if (active) setError(explain(e));
      });
    return () => {
      active = false;
    };
  }, [owner]);
  async function run(confirming: boolean) {
    if (!owner || busy || unknown || (confirming && (!snapshot || !agreed)))
      return;
    setBusy(true);
    setError("");
    setNotice("");
    let draft: Draft | undefined,
      sent = false;
    try {
      const old = await readDraft(owner, route);
      if (old?.uncertain) {
        setUnknown(true);
        throw new Error("请先核对原已读操作");
      }
      const request = key(),
        path = confirming ? "inbox/read-all" : "inbox/read-all/snapshot";
      const body = confirming
        ? {
            client_request_id: request,
            snapshot_id: snapshot!.snapshot_id,
            version: snapshot!.version,
          }
        : { client_request_id: request };
      draft = await saveDraft({
        ...(old ?? newDraft(owner, route)),
        values: body,
        requestId: request,
        uncertain: true,
        original: {
          path,
          method: "POST",
          body: encodeWire(body),
          replayable: true,
        },
      });
      sent = true;
      const result = await api.request<Snapshot | Result>(path, {
        method: "POST",
        body,
        owner,
      });
      await clearDraft(draft);
      if (confirming) {
        setSnapshot(undefined);
        const summary = result as Result;
        setNotice(
          `已核销 ${summary.applied_count} 项，失权或已处理的 ${summary.skipped_count} 项已跳过；新消息仍保留未读。`,
        );
        void reload("inbox-summary");
      } else {
        setSnapshot(result as Snapshot);
        setAgreed(false);
      }
    } catch (e) {
      const definite =
        e instanceof HttpError &&
        [400, 401, 403, 404, 409, 422, 429].includes(e.status);
      if (sent && !definite) setUnknown(true);
      else if (sent && draft) await clearDraft(draft).catch(() => {});
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <Button disabled={busy || unknown} onClick={() => void run(false)}>
        核对全部已读范围
      </Button>
      {notice ? <Notice title={notice} /> : null}
      {error ? <Notice title={error} error /> : null}
      {unknown ? (
        <Notice title="原已读操作结果待核对">
          <Button onClick={() => navigate("/recovery")}>
            按原快照和编号核对
          </Button>
        </Notice>
      ) : null}
      {snapshot ? (
        <Modal
          title="确认本次全部已读"
          busy={busy}
          onClose={() => setSnapshot(undefined)}
          actions={
            <Button
              kind="primary"
              disabled={
                busy ||
                !agreed ||
                unknown ||
                Date.parse(snapshot.expires_at) <= Date.now()
              }
              onClick={() => void run(true)}
            >
              确认核销本次快照
            </Button>
          }
        >
          <p>
            本次 {String(snapshot.count)} 项，其中免打扰{" "}
            {String(snapshot.muted_count)} 项。有效至 {snapshot.expires_at}。
          </p>
          <p>
            只标记快照中的消息和通知；不会接受申请、加入群聊、确认预约或办理待办。
          </p>
          <Check checked={agreed} onChange={setAgreed} disabled={busy}>
            我已核对这次范围，新到消息保持未读
          </Check>
          {error ? <Notice title={error} error /> : null}
        </Modal>
      ) : null}
    </>
  );
}
