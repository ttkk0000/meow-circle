import { Button, Notice, QueryState, TaskPage } from "../components/ui";
import { api, type RecordData } from "../core/api";
import { useQuery, reload } from "../core/query";
import { useSession } from "../core/session";
import { navigate, resourceRoute } from "../core/router";

/** 通知链接只接受编号；目标与权限都从当前账号的服务器记录重新核对。 */
export function NotificationPage({ id }: { id: bigint }) {
  const session = useSession();
  const path = `notifications/${id}`;
  const result = useQuery<RecordData>(path);
  const record = result.data;
  const owned =
    !!record && record.id === id && record.owner_id === session.user?.id;
  const target = owned
    ? resourceRoute(String(record.data.target_kind), record.data.target_id)
    : undefined;
  return (
    <TaskPage
      title="核对原通知"
      actions={
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
      }
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
          </Notice>
        ) : !result.loading && record ? (
          <Notice title="此通知当前不可查看" error>
            请返回消息中心核对当前账号可见的通知。
          </Notice>
        ) : null}
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
