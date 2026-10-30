import { usePages } from "../core/pages";
import { useQuery, reload } from "../core/query";
import { careReadOnlyReason, type CareContext } from "../core/care";
import { navigate } from "../core/router";
import {
  Button,
  Card,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { MediaGallery } from "../components/MediaGallery";
import { PageEnd } from "./Records";
import { stateLabel, workflowURL } from "./workflows";

export function CareCollaboration({ id }: { id: bigint }) {
  const path = `adoptions/${id}/care-context`;
  const context = useQuery<CareContext>(path);
  const readable = !!context.data && !context.loading && !context.error;
  const page = usePages(
    "care_updates",
    { reference_id: id },
    readable,
  );
  const refresh = async () => {
    if(await reload(path)) void reload(`care_updates?reference_id=${id}&cursor=0`);
  };
  const unavailable = !!context.error || context.denied || page.denied;
  if(unavailable) return (
    <TaskPage title="照护授权不可用" backTo="/messages" actions={<RowLink title="返回消息" to="/messages" />}>
      <Notice title="不再显示受限资料">
        本次授权已结束或当前无法确认访问权限。此处不再显示私密近况、照片或旧编辑内容；已有记录由原照护双方管理。
      </Notice>
      <QueryState loading={context.loading} error={context.error ?? page.error} failure={context.failure ?? page.failure}
        onRetry={refresh} />
      <RowLink title="查看协作与退出" to={`/invitations?ref=${id}&scope=caretaker`} />
    </TaskPage>
  );
  return (
    <TaskPage
      title={readable && !context.data!.can_post ? "照护近况仅可查阅" : "授权照护近况"}
      actions={
        readable && context.data!.can_post ? (
          <Button kind="primary" onClick={() => navigate(workflowURL("care", "adoptions", id))} disabled={page.loading}>
            记录私密照护近况
          </Button>
        ) : (
          <Button onClick={refresh} disabled={context.loading || page.loading}>
            重新读取当前权限
          </Button>
        )
      }
    >
      <Notice title="本次照护的有限协作">
        仅显示获授权事项的私密近况；协议、申请材料和完整宠物档案由原照护双方管理。
      </Notice>
      <QueryState
        loading={context.loading}
        error={context.error}
        failure={context.failure}
        readAt={context.readAt}
        onRetry={refresh}
      >
        {context.data ? (
          <>
            <p>当前阶段：{stateLabel(context.data.state)}</p>
            {!context.data.can_post ? (
              <Notice title="当前仅可查阅">
                {careReadOnlyReason(context.data)}
              </Notice>
            ) : null}
            <RowLink title="查看协作与退出" to={`/invitations?ref=${id}&scope=caretaker`} />
            <QueryState
              loading={page.loading}
              error={page.error}
              failure={page.failure}
              readAt={page.readAt}
              onRetry={page.retry}
              keepContent={!!page.rows.length && !page.denied}
            >
              {page.rows.map((row) => (
                <Card
                  key={String(row.id)}
                  title={row.data.needs_help ? "需要协助" : "照护近况"}
                >
                  <p>{String(row.data.text ?? "")}</p>
                  <small>{row.created_at}</small>
                  <MediaGallery
                    ids={row.media_ids}
                    description="授权照护照片"
                  />
                </Card>
              ))}
              <PageEnd page={page} />
            </QueryState>
          </>
        ) : null}
      </QueryState>
    </TaskPage>
  );
}
