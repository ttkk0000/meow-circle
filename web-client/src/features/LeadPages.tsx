import type { RecordData } from "../core/api";
import { usePages } from "../core/pages";
import { useQuery, reload } from "../core/query";
import { useSession } from "../core/session";
import { navigate } from "../core/router";
import { integer } from "../core/wire";
import {
  canAssessLead,
  canContactLead,
  leadState,
  type LeadRecord,
} from "../core/lead";
import {
  Button,
  Empty,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { MediaGallery } from "../components/MediaGallery";
import { PageEnd } from "./Records";
import { workflowURL } from "./workflows";

export function LeadInbox({ caseId }: { caseId: bigint }) {
  const parent = useQuery<RecordData>(caseId > 0n ? `cases/${caseId}` : null);
  const page = usePages("leads", caseId > 0n ? { reference_id: caseId } : {});
  const empty =
    !!page.data &&
    !page.loading &&
    !page.error &&
    !parent.loading &&
    !parent.error &&
    page.data.items.length === 0 &&
    page.rows.length === 0 &&
    page.next === 0n;
  return (
    <TaskPage
      title={
        empty
          ? "还没有新线索"
          : parent.data
            ? `${String(parent.data.data.title)}的线索`
            : "私密线索"
      }
      actions={
        caseId > 0n ? (
          <Button onClick={() => navigate(`/cases/${caseId}`)}>
            返回本次维护
          </Button>
        ) : undefined
      }
    >
      {!empty && (
        <Notice title="仅本案授权人员可见">
          不公开目击者原图、位置和联系信息。列表只显示当前账号有权读取的线索。
        </Notice>
      )}
      <QueryState
        loading={parent.loading || page.loading}
        error={parent.error || page.error}
        failure={parent.failure ?? page.failure}
        readAt={page.readAt}
        onRetry={() => {
          page.refresh();
          if (caseId > 0n) void reload(`cases/${caseId}`);
        }}
      >
        {empty ? (
          <Empty title="暂无新的目击线索">
            <p>可补照片、检查区域或主动分享，不能保证有人响应。</p>
          </Empty>
        ) : (
          <>
            {page.rows.map((row) => (
              <RowLink
                key={row.id.toString()}
                title={`一位宠友 · ${row.created_at}`}
                detail={`${leadState(row.data.assessment).title} · ${String(row.data.text ?? "").slice(0, 100)}`}
                to={`/leads/${row.id}`}
              />
            ))}
            <PageEnd
              page={page}
              empty="暂无你有权查看的新线索；不会生成示例目击或热度。"
            />
          </>
        )}
      </QueryState>
    </TaskPage>
  );
}

export function LeadPage({ id }: { id: bigint }) {
  const owner = useSession().user?.id ?? 0n;
  const lead = useQuery<LeadRecord>(`leads/${id}`);
  const parent = useQuery<RecordData>(
    lead.data ? `cases/${lead.data.reference_id}` : null,
  );
  const query = new URLSearchParams(location.search);
  const requested = query.get("view") === "reviewed";
  const submitted = query.get("view") === "submitted";
  const minimum = integer(query.get("version"));
  const row = lead.data;
  const c = parent.data;
  const state = leadState(row?.data.assessment);
  const fresh =
    !lead.loading && !lead.error && !parent.loading && !parent.error;
  const result =
    requested &&
    fresh &&
    row &&
    c &&
    row.version >= minimum &&
    row.data.reviewed_at &&
    row.data.reviewed_by === c.owner_id &&
    ["helpful", "not_match"].includes(row.data.assessment ?? "");
  const received =
    submitted &&
    minimum > 0n &&
    fresh &&
    row &&
    c &&
    row.owner_id === owner &&
    row.state === "submitted" &&
    row.version >= minimum;
  return (
    <TaskPage
      title={
        received ? "线索已收到" : result ? "线索处理已记录" : "核实这条线索"
      }
      actions={
        (received || result) && c ? (
          <Button onClick={() => navigate(`/cases/${c.id}`)}>
            返回本次维护
          </Button>
        ) : fresh && row && c && canContactLead(row, c, owner) ? (
          <Button
            kind="primary"
            onClick={() => navigate(workflowURL("lead-contact", "leads", id))}
          >
            {row.owner_id === owner ? "联系本案发布人" : "站内联系目击者"}
          </Button>
        ) : undefined
      }
    >
      <QueryState
        loading={lead.loading || parent.loading}
        error={lead.error || parent.error}
        failure={lead.failure ?? parent.failure}
        readAt={lead.readAt}
        onRetry={() => {
          void reload(`leads/${id}`);
          if (c) void reload(`cases/${c.id}`);
        }}
      >
        {row && c ? (
          received ? (
            <>
              <Notice title="已收到" success>
                {["active", "matched"].includes(c.state)
                  ? "等待本案发布人核实，不代表已匹配或找回；不会移动公共标记或修改宠物状态。"
                  : "记录曾被接收；本次求助已停止新联系，仍可查看你有权读取的历史。"}
              </Notice>
              <p className="muted">
                私密线索 #{id.toString()} · {row.created_at}
              </p>
              <RowLink title="返回本次求助" to={`/cases/${c.id}`} />
            </>
          ) : result ? (
            <>
              <Notice
                title={state.title}
                success={row.data.assessment === "helpful"}
              >
                {state.detail}
              </Notice>
              <p className="muted">当前记录时间：{row.data.reviewed_at}</p>
              <RowLink title="继续核实这条线索" to={`/leads/${id}`} />
            </>
          ) : (
            <>
              <strong>
                {String(c.data.title)} · 私密线索 #{id.toString()}
              </strong>
              {row.media_ids?.length ? (
                <div className="lead-photos">
                  <MediaGallery
                    ids={row.media_ids}
                    description="目击者提供的私密照片"
                  />
                </div>
              ) : null}
              <p className="publication-text">{row.data.text}</p>
              <p className="muted">收到于 {row.created_at}</p>
              <Notice title={state.title}>{state.detail}</Notice>
              {(requested || submitted) && row.version < minimum ? (
                <Notice title="当前结果尚未回读">
                  保留原请求，请重新读取当前记录，不会自动重复标记。
                </Notice>
              ) : null}
              {fresh && row && c && canAssessLead(row, c, owner) ? (
                <>
                  {row.data.assessment !== "helpful" ? (
                    <Button
                      onClick={() =>
                        navigate(
                          workflowURL("lead-assessment", "leads", id, {
                            assessment: "helpful",
                          }),
                        )
                      }
                    >
                      标记有帮助
                    </Button>
                  ) : null}
                  {row.data.assessment !== "not_match" ? (
                    <Button
                      kind="quiet"
                      onClick={() =>
                        navigate(
                          workflowURL("lead-assessment", "leads", id, {
                            assessment: "not_match",
                          }),
                        )
                      }
                    >
                      标记不匹配
                    </Button>
                  ) : null}
                </>
              ) : null}
              <RowLink
                title="报告索费或骚扰"
                to={workflowURL("safety", "cases", c.id)}
              />
              <RowLink title="返回线索列表" to={`/leads?ref=${c.id}`} />
              {!["active", "matched"].includes(c.state) ? (
                <Notice title="本次求助已停止新联系">
                  仍可核对你有权读取的线索历史。
                </Notice>
              ) : null}
            </>
          )
        ) : null}
      </QueryState>
    </TaskPage>
  );
}
