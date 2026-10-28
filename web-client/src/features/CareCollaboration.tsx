import { usePages } from "../core/pages";
import { useQuery, reload } from "../core/query";
import { careReadOnlyReason, type CareContext } from "../core/care";
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
  const page = usePages(
    "care_updates",
    { reference_id: id },
    !!context.data && !context.error,
  );
  const refresh = () => {
    void reload(path);
    page.refresh();
  };
  return (
    <TaskPage
      title="授权照护近况"
      actions={
        <Button onClick={refresh} disabled={context.loading || page.loading}>
          重新读取
        </Button>
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
            {context.data.can_post ? (
              <RowLink
                title="记录私密照护近况"
                to={workflowURL("care", "adoptions", id)}
              />
            ) : (
              <Notice title="当前仅可查阅">
                {careReadOnlyReason(context.data)}
              </Notice>
            )}
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
