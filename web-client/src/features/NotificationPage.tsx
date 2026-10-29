import { useState } from "react";
import { Button, Notice, QueryState, TaskPage } from "../components/ui";
import { api, explain, type RecordData } from "../core/api";
import { useQuery, reload, invalidateQueries } from "../core/query";
import { useSession } from "../core/session";
import { navigate, resourceRoute } from "../core/router";

/** 通知链接只接受编号；目标与权限都从当前账号的服务器记录重新核对。 */
export function NotificationPage({ id }: { id: bigint }) {
  const session = useSession();
  const path = `notifications/${id}`;
  const result = useQuery<RecordData>(path);
  const [busy,setBusy] = useState(false);
  const [error,setError] = useState("");
  const record = result.data;
  const owned =
    !!record && record.id === id && record.owner_id === session.user?.id;
  const target = owned
    ? resourceRoute(String(record.data.target_kind), record.data.target_id)
    : undefined;
  async function markRead() {
    if(!owned || !record || record.data.read || busy || result.loading || result.error) return;
    const owner = session.user!.id;
    setBusy(true); setError("");
    try {
      await api.request(`notifications/${id}/read`,{method:"POST",owner,body:{version:record.version}});
      if(api.session.user?.id !== owner) return;
      invalidateQueries("notifications");
      await reload("inbox-summary");
    } catch(e) { if(api.session.user?.id === owner) setError(explain(e)); }
    finally { setBusy(false); }
  }
  return (
    <TaskPage
      title="核对原通知"
      actions={<>
        <Button disabled={!owned || !!record?.data.read || busy || result.loading || !!result.error} onClick={() => void markRead()}>
          {record?.data.read ? "已标为已读" : busy ? "正在标记…" : "标为已读"}
        </Button>
        <Button
          kind="primary"
          disabled={!target || result.loading || !!result.error}
          onClick={() => {
            if (target && api.session.user?.id === record?.owner_id)
              navigate(target);
          }}
        >
          查看原事项
        </Button>
      </>}
    >
      <QueryState
        loading={result.loading}
        error={result.error}
        failure={result.failure}
        readAt={result.readAt}
        onRetry={() => void reload(path)}
      >
        {owned ? (
          <Notice title="请核对后查看">
            {String(record.data.text ?? "有新的进度，请查看原事项。")}
            <p>打开原事项不代表接受或确认。标为已读只核销这条通知，待处理任务仍由当前业务状态决定。</p>
          </Notice>
        ) : !result.loading && record ? (
          <Notice title="此通知当前不可查看" error>
            请返回消息中心核对当前账号可见的通知。
          </Notice>
        ) : null}
        {error ? <Notice title={error} error>
          <Button disabled={busy || result.loading} onClick={() => { setError(""); void reload(path); }}>重新读取当前通知</Button>
        </Notice> : null}
        {owned && !target ? (
          <Notice title="原事项暂不可继续">
            通知目标可能已失效或暂不支持打开，可在消息中心查看已有沟通；不会执行载荷中的操作。
          </Notice>
        ) : null}
        <Button onClick={() => navigate("/messages")}>查看已有沟通</Button>
      </QueryState>
    </TaskPage>
  );
}
