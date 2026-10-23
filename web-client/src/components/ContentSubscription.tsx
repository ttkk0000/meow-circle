import { useEffect, useState } from "react";
import { api, explain, HttpError, type RecordData } from "../core/api";
import { useSession } from "../core/session";
import { reload, useQuery } from "../core/query";
import { navigate } from "../core/router";
import { clearDraft, newDraft, readDraft, saveDraft } from "../core/drafts";
import { encodeWire, key } from "../core/wire";
import { Button, Notice } from "./ui";

export interface ContentSubscriptions {
  followed_users: bigint[];
  watched_cases: bigint[];
}
export function ContentSubscription({
  id,
  kind,
}: {
  id: bigint;
  kind: "user" | "case";
}) {
  const owner = useSession().user?.id ?? 0n;
  const result = useQuery<RecordData<ContentSubscriptions>>(
    owner ? "content-subscriptions" : null,
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [unknown, setUnknown] = useState(false);
  const route = `/content-subscriptions?kind=${kind}&id=${id}`;
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
  }, [owner, route]);
  const values = result.data?.data;
  const selected =
    !!values &&
    (kind === "user" ? values.followed_users : values.watched_cases).includes(
      id,
    );
  async function toggle() {
    if (!owner) {
      navigate("/security");
      return;
    }
    if (!result.data || !values || unknown || busy) return;
    setBusy(true);
    setError("");
    let sent = false;
    const old = await readDraft(owner, route).catch(() => undefined);
    let draft = old ?? newDraft(owner, route);
    try {
      if (draft.uncertain) throw new Error("请先核对原订阅操作");
      if (
        !confirm(
          selected
            ? "取消此项关注？"
            : "关注仅订阅当前可读内容，不改变好友或私密访问范围。确认关注？",
        )
      )
        return;
      const field = kind === "user" ? "followed_users" : "watched_cases";
      const list = values[field];
      const body = {
        ...values,
        [field]: selected
          ? list.filter((value) => value !== id)
          : [...list, id],
        version: result.data.version,
        client_request_id: key(),
      };
      draft = {
        ...draft,
        requestId: body.client_request_id,
        values: body,
        baseVersion: result.data.version,
        uncertain: true,
        original: {
          path: "content-subscriptions",
          method: "POST",
          body: encodeWire(body),
          replayable: true,
        },
      };
      draft = await saveDraft(draft);
      sent = true;
      await api.request("content-subscriptions", {
        method: "POST",
        body,
        owner,
      });
      await clearDraft(draft);
      void reload("content-subscriptions");
    } catch (e) {
      if (
        sent &&
        !(
          e instanceof HttpError &&
          [400, 401, 403, 404, 409, 422, 429].includes(e.status)
        )
      )
        setUnknown(true);
      else if (sent) await clearDraft(draft).catch(() => {});
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <Button
        onClick={() => void toggle()}
        disabled={busy || unknown || (!!owner && !result.data)}
      >
        {selected ? "取消关注" : kind === "user" ? "关注作者" : "关注求助进展"}
      </Button>
      {error ? <Notice title={error} error /> : null}
      {unknown ? (
        <Notice title="原关注操作结果待核对">
          <Button onClick={() => navigate("/recovery")}>核对原操作</Button>
        </Notice>
      ) : null}
    </>
  );
}
