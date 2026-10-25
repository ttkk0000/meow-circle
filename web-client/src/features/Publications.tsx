import { useSession } from "../core/session";
import { usePages } from "../core/pages";
import { navigate } from "../core/router";
import {
  Button,
  Card,
  Choices,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { PageEnd, RecordCard } from "./Records";
import { workflowURL } from "./workflows";

const choices: Array<[string, string]> = [
  ["pets", "宠物介绍"],
  ["entries", "日常图文"],
  ["cases", "走失求助"],
  ["offers", "有偿信息"],
];
export function Publications({ params }: { params: URLSearchParams }) {
  const owner = useSession().user?.id ?? 0n;
  const selected = params.get("kind") ?? "pets";
  const kind = choices.some(([name]) => name === selected) ? selected : "pets";
  const page = usePages(kind, { owner_id: owner }, !!owner);
  const editor = (
    { pets: "pet", entries: "entry", cases: "case", offers: "offer" } as Record<
      string,
      string
    >
  )[kind];
  return (
    <TaskPage
      title="我的发布"
      actions={
        <Button kind="primary" onClick={() => navigate("/publish")}>
          新建发布
        </Button>
      }
    >
      <Choices
        label="发布类型"
        value={kind}
        choices={choices}
        onChange={(value) => {
          navigate(`/publications?kind=${value}`, true);
        }}
      />
      <QueryState
        loading={page.loading}
        error={page.error}
        failure={page.failure}
        readAt={page.readAt}
        onRetry={page.retry}
        keepContent={!!page.rows.length && !page.denied}
      >
        {page.rows.map((row) => (
          <Card key={String(row.id)}>
            <RecordCard kind={kind} row={row} />
            {kind === "pets" ? (
              <>
                <RowLink
                  title={`维护${String(row.data.name)}的介绍与评论`}
                  detail="补充图文、照护协作、持续答疑"
                  to={`/pets/${row.id}`}
                />
                <RowLink
                  title="处理申请"
                  detail="沟通、接受或说明拒绝原因"
                  to={`/adoptions?ref=${row.id}`}
                />
                <RowLink
                  title="暂停／结束发布或修改资料"
                  detail="按当前版本核对，不删除必要处理记录"
                  to={workflowURL("pet", kind, row.id)}
                />
                <RowLink
                  title="管理走失求助"
                  detail="更新线索与找回状态"
                  to="/publications?kind=cases"
                />
              </>
            ) : (
              <>
                <RowLink
                  title="修改资料或材料"
                  detail="关键变更会生成新版本"
                  to={workflowURL(editor, kind, row.id)}
                />
                {kind === "cases" ? (
                  <RowLink title="管理线索与找回状态" to={`/cases/${row.id}`} />
                ) : null}
              </>
            )}
          </Card>
        ))}
        <PageEnd page={page} />
      </QueryState>
      <Notice title="旧安排不会悄悄继续">
        地点、宠物、接待人和费用变更后，双方需重新确认。
      </Notice>
      <RowLink title="本机草稿与待核对操作" to="/recovery" />
    </TaskPage>
  );
}
