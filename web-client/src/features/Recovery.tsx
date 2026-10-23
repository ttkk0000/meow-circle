import { useEffect, useState } from "react";
import { api, explain, HttpError } from "../core/api";
import {
  clearDraft,
  listCapabilities,
  listDrafts,
  newDraft,
  readDraft,
  saveDraft,
  type Draft,
} from "../core/drafts";
import { useSession } from "../core/session";
import { navigate, resourceRoute } from "../core/router";
import { Button, Card, Empty, Notice, TaskPage } from "../components/ui";
import { stateLabel, workflowURL } from "./workflows";
import { RetrySubmission } from "../components/RetrySubmission";

interface Receipt {
  client_request_id: string;
  result_kind: string;
  result_id: bigint;
  result_version: bigint;
  state: string;
}
interface LimitedStatus {
  id: bigint;
  version: bigint;
  state: string;
  type: string;
  pending?: bigint;
  held?: bigint;
  external_state?: string;
  review_due?: string;
}
export function Recovery() {
  const owner = useSession().user?.id ?? 0n;
  const [rows, setRows] = useState<Draft[]>([]);
  const [capabilities, setCapabilities] = useState<Draft[]>([]);
  const [error, setError] = useState("");
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    setRows([]);
    setCapabilities([]);
    void Promise.all([
      listDrafts(owner).then(async (list) =>
        Promise.all(list.map((item) => readDraft(owner, item.route))),
      ),
      listCapabilities(),
    ])
      .then(([drafts, capabilities]) => {
        if (active) {
          setRows(
            drafts.filter(
              (d): d is Draft =>
                !!d &&
                (!!d.original ||
                  !!d.photos.length ||
                  !!Object.keys(d.values).length),
            ),
          );
          setCapabilities(capabilities);
        }
      })
      .catch((e) => {
        if (active) setError(explain(e));
      });
    return () => {
      active = false;
    };
  }, [owner, version]);
  return (
    <TaskPage
      title="本机草稿与待核对事项"
      actions={
        <Button onClick={() => setVersion((v) => v + 1)}>
          重新读取本机记录
        </Button>
      }
    >
      <Notice title="恢复不会自动发送">
        先读取原编号的服务端回执。未找到回执不能当作操作失败；只有支持幂等的普通操作可由你按原内容重试。验证码和用途授权不会保存在草稿。
      </Notice>
      {error ? <Notice title={error} error /> : null}
      {!rows.length && !capabilities.length ? (
        <Empty title="此浏览器暂无可恢复事项" />
      ) : null}
      {rows.map((draft) =>
        draft.access ? (
          <LimitedCard key={draft.key} draft={draft} />
        ) : (
          <RecoveryCard
            key={draft.key}
            draft={draft}
            onChange={() => setVersion((v) => v + 1)}
          />
        ),
      )}
      {capabilities.map((draft) => (
        <LimitedCard key={draft.key} draft={draft} />
      ))}
    </TaskPage>
  );
}
function RecoveryCard({
  draft,
  onChange,
}: {
  draft: Draft;
  onChange: () => void;
}) {
  const [receipt, setReceipt] = useState<Receipt>();
  const [missing, setMissing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function check() {
    setBusy(true);
    setMissing(false);
    setError("");
    try {
      const result = await api.request<Receipt>(
        `operations/${draft.requestId}`,
      );
      setReceipt(result);
    } catch (e) {
      if (e instanceof HttpError && e.status === 404) {
        setMissing(true);
        setError("当前尚未找到回执，请保留原编号；这不代表请求未被接收");
      } else setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  async function clear() {
    setBusy(true);
    try {
      await clearDraft(draft);
      onChange();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  const to = receipt && resourceRoute(receipt.result_kind, receipt.result_id);
  return (
    <Card title={draft.uncertain ? "原操作结果待核对" : "本机未提交草稿"}>
      <p className="caption muted">原编号 {draft.requestId}</p>
      <p>
        照片 {draft.photos.length} 张 · 基准版本 {String(draft.baseVersion)}
      </p>
      {error ? <Notice title={error} error /> : null}
      {draft.uncertain ? (
        <>
          <Button onClick={() => void check()} disabled={busy}>
            查询原操作回执
          </Button>
          {receipt ? (
            <Notice title={`已找到服务端回执 · ${stateLabel(receipt.state)}`}>
              结果版本 {String(receipt.result_version)}
              {to ? (
                <Button onClick={() => navigate(to)}>查看真实结果记录</Button>
              ) : null}
              <Button onClick={() => void clear()} disabled={busy}>
                回执已核对，清除此本机快照
              </Button>
            </Notice>
          ) : null}
          {missing && draft.original ? (
            <RetrySubmission draft={draft} onDone={() => void check()} />
          ) : null}
          {missing && !draft.original?.replayable ? (
            <Notice title="此操作需要重新获取当前状态和用途验证">
              保留本机快照，请先核对真实业务结果；不能直接重放旧敏感授权。
            </Notice>
          ) : null}
        </>
      ) : (
        <>
          <Button onClick={() => navigate(draft.route)}>
            恢复编辑，暂不发送
          </Button>
          <Button
            kind="quiet"
            onClick={() => {
              if (
                confirm(
                  "清除此浏览器内该任务的草稿和照片？已上传的无引用材料由服务端按策略回收。",
                )
              )
                void clear();
            }}
            disabled={busy}
          >
            清除此本机草稿
          </Button>
        </>
      )}
    </Card>
  );
}
function LimitedCard({ draft }: { draft: Draft }) {
  const access = draft.access!;
  const [status, setStatus] = useState<LimitedStatus>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [backup, setBackup] = useState(false);
  const [missing, setMissing] = useState(false);
  async function check() {
    setBusy(true);
    setError("");
    setMissing(false);
    try {
      const safety = access.type === "safety";
      const path = access.id
        ? safety
          ? `safety/tickets/${access.id}`
          : `data-requests/${access.id}`
        : safety
          ? `safety/requests/${access.requestId}`
          : `data-requests/requests/${access.requestId}`;
      const result = await api.request<LimitedStatus>(path, {
        headers: {
          [safety ? "X-Safety-Token" : "X-Request-Access"]: access.token,
        },
      });
      setStatus(result);
      const route = `capability:${access.requestId}`;
      const old = await readDraft(0n, route);
      const saved = old ?? newDraft(0n, route);
      await saveDraft({ ...saved, access: { ...access, id: result.id } });
      setBackup(true);
    } catch (e) {
      if (e instanceof HttpError && e.status === 404) setMissing(true);
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Card
      title={
        access.type === "safety"
          ? "安全与隐私事项的受限查询"
          : "资料事项的受限查询"
      }
    >
      <p className="caption muted">
        原编号 {access.requestId} · 只查询当前事项
      </p>
      <Button onClick={() => void check()} disabled={busy}>
        凭原受限能力查询进度
      </Button>
      {error ? <Notice title={error} error /> : null}
      {missing && draft.original ? (
        <RetrySubmission draft={draft} onDone={() => void check()} />
      ) : null}
      {status ? (
        <>
          <p>
            {stateLabel(status.state)} · #{String(status.id)} · 版本{" "}
            {String(status.version)}
          </p>
          {status.review_due ? <p>复核日期 {status.review_due}</p> : null}
          {status.pending !== undefined ? (
            <p>
              待处理 {String(status.pending)} 项 · 保全{" "}
              {String(status.held ?? 0n)} 项 · 外部处理{" "}
              {stateLabel(String(status.external_state ?? ""))}
            </p>
          ) : null}
          {access.type === "safety" && backup ? (
            <Button
              onClick={() =>
                navigate(
                  workflowURL("safety", "", 0n, {
                    related: String(status.id),
                    version: String(status.version),
                    access: access.requestId,
                    type: "supplement",
                  }),
                )
              }
            >
              沿原事项补充材料
            </Button>
          ) : null}
          <Notice title="以实际处理状态为准">
            受理、待补充和保全均不代表已经删除；此查询能力仅存于当前浏览器，不会写入网址或展示为账号凭据。
          </Notice>
        </>
      ) : null}
    </Card>
  );
}
