import { useState } from "react";
import {
  adoptionActionOptions,
  adoptionProgressTitle,
  careStageAvailable,
  returnActionAllowed,
} from "../core/adoption";
import { AdoptionProgress } from "./AdoptionPages";
import { ReturnProgress } from "./ReturnPages";
import { api, explain, type RecordData, type User } from "../core/api";
import { useSession } from "../core/session";
import { useQuery, reload } from "../core/query";
import { navigate, resourceRoute } from "../core/router";
import { integer, key, money } from "../core/wire";
import { titleOf, usePages } from "../core/pages";
import { Facts, labelFor } from "../components/SchemaForm";
import {
  Button,
  Card,
  Empty,
  Icon,
  Notice,
  PrivateImage,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { actionNames, stateLabel, workflowURL } from "./workflows";
import { Author, PageEnd, RecordCard, titles } from "./Records";
import { Composer, RichText } from "./chat";
import { ContentSubscription } from "../components/ContentSubscription";
import { MediaGallery } from "../components/MediaGallery";
import { CoarseMap } from "./MapPage";
import { AppointmentDetail } from "./AppointmentPages";
import { invitationActions, invitationReview } from "../core/invitation";
import { CollaborationInvitation } from "./InvitationPages";

type ObjectValue = Record<string, unknown>;
const objects = (value: unknown) =>
  Array.isArray(value) ? (value as ObjectValue[]) : [];
const has = (value: unknown, id: bigint) =>
  Array.isArray(value) && value.some((v) => integer(v) === id);
interface Action {
  label: string;
  flow: string;
  extra?: Record<string, string>;
}
function actionOptions(
  kind: string,
  r: RecordData,
  owner: bigint,
  parent?: RecordData,
): Action[] {
  if (!owner) return [];
  const d = r.data;
  const own = r.owner_id === owner;
  const state = r.state;
  const list: Action[] = [];
  const add = (flow: string, action: string, label = actionNames[action]) =>
    list.push({ flow, label, extra: { action, owner: String(owner) } });
  if (kind === "adoptions") {
    const publisher = integer(d.publisher_id) === owner,
      applicant = integer(d.applicant_id) === owner;
    if (owner <= 0n || (!publisher && !applicant)) return [];
    adoptionActionOptions(r, owner).forEach((action) =>
      add(
        "adoption-action",
        action,
        action === "approve" ? "同意继续沟通" : actionNames[action],
      ),
    );
    if (["return_requested", "return_agreed"].includes(state))
      list.push({ flow: "return", label: "提出安全接回方案" });
    if (applicant && careStageAvailable(r))
      list.push({ flow: "care", label: "记录私密照护近况" });
    if (
      [
        "approved",
        "trial_proposed",
        "trial_active",
        "handover_pending",
      ].includes(state)
    )
      list.push({ flow: "contract", label: "提出协议新版本" });
  }
  if (kind === "claims") {
    const publisher = parent?.owner_id === owner;
    if (publisher && state === "submitted")
      ["match", "reject", "request_supplement"].forEach((a) =>
        add("claim-action", a),
      );
    if (own && state === "needs_info") add("claim-action", "supplement");
    if (own && ["rejected", "withdrawn"].includes(state))
      add("claim-action", "resubmit");
    if (own && ["submitted", "needs_info"].includes(state))
      add("claim-action", "withdraw");
    if ((own || publisher) && ["matched", "handover_pending"].includes(state)) {
      add("claim-action", "dispute");
      if (!(publisher ? d.publisher_confirmed : d.claimant_confirmed))
        add("claim-action", "confirm_handover");
    }
  }
  if (kind === "groups" && state === "active" && has(d.members, owner)) {
    add("group-action", "mute");
    if (own) {
      ["notice", "directory", "transfer", "remove_member", "dissolve"].forEach(
        (a) => add("group-action", a),
      );
      if (integer(d.pending_transfer_to))
        add("group-action", "revoke_transfer");
    } else add("group-action", "leave");
    if (
      integer(d.pending_transfer_to) === owner &&
      Date.parse(String(d.transfer_expires_at)) > Date.now()
    )
      ["accept_transfer", "decline_transfer"].forEach((a) =>
        add("group-action", a),
      );
    if (own)
      list.push({
        flow: "invite",
        label: "邀请好友入群",
        extra: { scope: "group_member" },
      });
  }
  if (kind === "friendships") {
    if (state === "pending") {
      if (integer(d.recipient_id) === owner)
        ["accept", "decline"].forEach((a) => add("friend-action", a));
      else if (integer(d.requester_id) === owner)
        add("friend-action", "withdraw");
    }
    if (state === "accepted") add("friend-action", "remove");
    if (state !== "blocked") add("friend-action", "block");
    else if (integer(d.blocked_by) === owner) add("friend-action", "unblock");
  }
  if (kind === "invitations") {
    invitationActions(r, owner).forEach((action) =>
      add(
        "invitation-action",
        action,
        action === "leave" ? invitationReview(r, action).title : actionNames[action],
      ),
    );
  }
  if (kind === "cases" && own && ["active", "matched"].includes(state))
    add("state-action", "close");
  return list;
}
export function ResourcePage({
  kind,
  id,
  discussionOnly = false,
}: {
  kind: string;
  id: bigint;
  discussionOnly?: boolean;
}) {
  const path = `${kind}/${id}`;
  const result = useQuery<RecordData>(path);
  return (
    <QueryState
      loading={result.loading}
      error={result.error}
      failure={result.failure}
      readAt={result.readAt}
      onRetry={() => void reload(path)}
    >
      {result.data ? (
        <ResourceBody
          key={kind + id + String(discussionOnly)}
          kind={kind}
          row={result.data}
          discussionOnly={discussionOnly}
        />
      ) : null}
    </QueryState>
  );
}
function ResourceBody({
  kind,
  row: r,
  discussionOnly,
}: {
  kind: string;
  row: RecordData;
  discussionOnly: boolean;
}) {
  const session = useSession();
  const owner = session.user?.id ?? 0n;
  const d = r.data;
  const own = r.owner_id === owner;
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [reply, setReply] = useState<bigint>();
  const parent = useQuery<RecordData>(
    kind === "claims" ? `cases/${r.reference_id}` : null,
  );
  const actions = actionOptions(kind, r, owner, parent.data);
  const checks = useQuery<
    Array<{
      id: bigint;
      check: string;
      state: string;
      summary: string;
      target_version: bigint;
      valid_until?: string;
    }>
  >(["pets", "offers"].includes(kind) ? `${kind}/${r.id}/verifications` : null);
  const canShare = ["entries", "pets", "cases", "offers"].includes(kind);
  async function share() {
    setError("");
    try {
      await api.request(`share/${kind}/${r.id}`);
      const url = location.origin + `/share/${kind}/${r.id}`;
      if (navigator.share) await navigator.share({ title: titleOf(r), url });
      else {
        await navigator.clipboard.writeText(url);
        setNotice("公开链接已复制；撤回或权限变化后将重新校验可见性");
      }
    } catch (e) {
      setError(explain(e));
    }
  }
  const editor = (
    {
      entries: "entry",
      pets: "pet",
      cases: "case",
      offers: "offer",
    } as Record<string, string>
  )[kind];
  const commentComposer = (
    <Composer
      kind={kind as "entries" | "pets"}
      id={r.id}
      reply={reply}
      onReplyChange={setReply}
      onSent={() => {
        setReply(0n);
        void reload(
          `${kind === "pets" ? "pet_comments" : "entry_comments"}?reference_id=${r.id}&cursor=0`,
        );
      }}
    />
  );
  if (kind === "pets" && discussionOnly) {
    const available = !!(d.adoption_available || d.adoption_opened);
    return (
      <TaskPage
        title={`${String(d.name || "宠物")}的领养交流`}
        actions={available ? commentComposer : undefined}
      >
        {available ? (
          <>
            <h3>正在讨论：{String(d.name || "这只宠物")}</h3>
            <Comments
              kind="pet_comments"
              showTitle={false}
              entry={r.id}
              reply={reply ?? 0n}
              onReply={setReply}
            />
            <Notice title="评论不能替代医疗资料">
              想了解具体情况可站内联系，不公开证件和家庭地址。
            </Notice>
          </>
        ) : (
          <Notice title="目前未开放领养交流" />
        )}
        <RowLink title="返回宠物介绍" to={`/pets/${r.id}`} />
      </TaskPage>
    );
  }
  if (kind === "appointments")
    return <AppointmentDetail row={r} actor={owner} />;
  if (kind === "invitations" && ["caretaker", "adoption_editor"].includes(String(d.scope)))
    return <CollaborationInvitation row={r} actor={owner} />;
  return (
    <TaskPage
      title={
        kind === "entries"
          ? String(d.title || "晒宠日常")
          : kind === "adoptions"
            ? adoptionProgressTitle(r, owner)
            : (titles[kind] ?? "事项详情")
      }
      subtitle={`#${r.id} · 版本 ${r.version} · ${stateLabel(r.state)}`}
      actions={kind === "entries" ? commentComposer : undefined}
    >
      {error ? <Notice title={error} error /> : null}
      {notice ? <Notice title={notice} /> : null}
      <div className="actions-row">
        {r.owner_id > 0n ? <Author id={r.owner_id} /> : null}
        {canShare ? (
          <Button kind="quiet" onClick={() => void share()}>
            分享公开链接
          </Button>
        ) : null}
      </div>
      <MediaGallery ids={r.media_ids ?? []} description={titleOf(r)} />
      {kind === "entries" ? (
        <>
          <p className="prose">{String(d.text ?? "")}</p>
          {["ai_generated", "ai_modified"].includes(
            String(d.content_origin),
          ) ? (
            <Notice title="含 AI 生成／修改内容" />
          ) : null}
          <Comments entry={r.id} reply={reply ?? 0n} onReply={setReply} />
        </>
      ) : kind === "adoptions" ? (
        <AdoptionProgress row={r} actor={owner} />
      ) : (
        <Card title="当前资料">
          <Facts value={d} />
        </Card>
      )}
      {checks.data ? (
        <Card title="逐项核实与有效范围">
          <p className="muted caption">
            仅适用于当前版本指定项目；核实状态与到访反馈分别展示。
          </p>
          {checks.data.length ? (
            checks.data.map((check) => (
              <div key={String(check.id)} className="stack">
                <strong>
                  {labelFor(check.check)} · {stateLabel(check.state)}
                </strong>
                <p>{check.summary}</p>
                <small>
                  版本 {String(check.target_version)} · 有效至{" "}
                  {check.valid_until || "未提供有效期"}
                </small>
              </div>
            ))
          ) : (
            <p>暂无此版本的核实结论</p>
          )}
        </Card>
      ) : null}
      {kind === "adoptions" ? (
        <>
          <ReturnProgress row={r} actor={owner} />
          <AdoptionVersions row={r} />
          <RowLink title="私密照护近况" to={`/care-updates?ref=${r.id}`} />
          {integer(d.applicant_id) === owner && careStageAvailable(r) ? (
            <RowLink
              title="邀请私密照护协作"
              to={workflowURL("invite", "adoptions", r.id, {
                scope: "caretaker",
              })}
            />
          ) : null}
        </>
      ) : null}
      {kind === "invitations" &&
      r.state === "accepted" &&
      integer(d.recipient_id) === owner ? (
        d.scope === "caretaker" ? (
          <RowLink
            title="查看授权照护近况"
            to={`/care-collaboration/${integer(d.target_id)}`}
          />
        ) : d.scope === "adoption_editor" ? (
          <RowLink
            title="维护该宠物介绍"
            to={workflowURL("introduction", "pets", integer(d.target_id))}
          />
        ) : null
      ) : null}
      {kind === "group-directory" ? (
        <Card title="加入当前群聊">
          {d.member ? (
            <RowLink title="进入已加入的群聊" to={`/groups/${r.id}`} />
          ) : (
            <RowLink
              title="核对并申请加入"
              to={workflowURL("group-join", kind, r.id)}
            />
          )}
        </Card>
      ) : null}
      {kind === "pets" && (d.adoption_available || d.adoption_opened) ? (
        <RowLink
          title="领养交流"
          detail="照护讨论与公开答疑"
          to={`/pets/${r.id}?view=discussion`}
        />
      ) : null}
      {kind === "pets" ? (
        <>
          <RowLink title="查看当前照护方" to={`/users/${r.owner_id}`} />
          {!own && d.adoption_available ? (
            <RowLink
              title="了解后填写领养申请"
              to={workflowURL("application", "pets", r.id)}
            />
          ) : null}
          {own && d.adoption_available ? (
            <>
              <RowLink
                title="邀请介绍协作"
                to={workflowURL("invite", "pets", r.id, {
                  scope: "adoption_editor",
                })}
              />
            </>
          ) : !own && d.adoption_available ? (
            <RowLink
              title="授权介绍协作"
              to={workflowURL("introduction", "pets", r.id)}
            />
          ) : null}
        </>
      ) : null}
      {kind === "cases" ? (
        <>
          <ContentSubscription id={r.id} kind="case" />
          {d.location_precision !== "area" ? (
            <CoarseMap
              latitude={Number(d.latitude)}
              longitude={Number(d.longitude)}
              coordinateSystem={String(d.coordinate_system ?? "")}
            />
          ) : (
            <Notice title="此求助仅提供文字区域">
              {String(d.city)} · {String(d.area)}
            </Notice>
          )}
          <RowLink
            title="提供私密目击线索"
            to={workflowURL("lead", kind, r.id)}
          />
          {!own && d.type === "found" ? (
            <RowLink
              title="提交私密认领材料"
              to={workflowURL("claim", kind, r.id)}
            />
          ) : null}
          {own ? (
            <>
              <RowLink title="私密认领申请" to={`/claims?ref=${r.id}`} />
              <RowLink title="私密目击线索" to={`/leads?ref=${r.id}`} />
            </>
          ) : null}
        </>
      ) : null}
      {["pets", "cases", "offers", "adoptions"].includes(kind) ? (
        <RowLink
          title="安排一次到访"
          to={workflowURL("appointment", kind, r.id, {
            peer: String(own ? 0n : r.owner_id),
            context: (
              {
                pets: "visit",
                cases: "case",
                offers: "offer",
                adoptions: "adoption",
              } as Record<string, string>
            )[kind],
          })}
        />
      ) : null}
      {kind === "offers" ? (
        <Notice title={`完整披露合计 ${money(d.total_cents)}`}>
          预约快照由服务端按当前披露版本生成，新增费用或修改条款需要双方重新确认。
        </Notice>
      ) : null}
      {actions.length ? (
        <Card title="当前角色可处理的事项">
          {actions.map((action, index) => (
            <RowLink
              key={index}
              title={action.label}
              detail={`核对版本 ${r.version} 后再主动确认`}
              to={workflowURL(action.flow, kind, r.id, action.extra)}
            />
          ))}
        </Card>
      ) : null}
      {kind === "guardians" ? (
        <RowLink
          title="核对指定儿童、范围和有效期"
          to={workflowURL("guardian", kind, r.id)}
        />
      ) : null}
      {kind === "reviews" ? (
        <>
          <RowLink
            title="在原事项补充"
            to={workflowURL("review-supplement", kind, r.id)}
          />
          <RowLink
            title="对原结论提出申诉"
            to={workflowURL(
              "review",
              String(d.target_kind ?? ""),
              integer(d.target_id),
              { type: "appeal", appeal: String(r.id) },
            )}
          />
        </>
      ) : null}
      {editor && own ? (
        <Card title="管理此记录">
          <RowLink title="编辑当前版本" to={workflowURL(editor, kind, r.id)} />
          {kind !== "cases" ? (
            <RowLink
              title="删除此记录"
              to={workflowURL("delete", kind, r.id)}
            />
          ) : null}
        </Card>
      ) : null}
      {canShare ? (
        <Card title="安全与核实">
          <RowLink
            title="核实、举报或协助"
            to={workflowURL("review", kind, r.id)}
          />
          <RowLink
            title="安全／隐私报告，游客也可提交"
            to={workflowURL("safety", kind, r.id)}
          />
        </Card>
      ) : null}
    </TaskPage>
  );
}
function AdoptionVersions({ row: r }: { row: RecordData }) {
  const owner = useSession().user?.id ?? 0n;
  const party =
    owner > 0n &&
    [integer(r.data.applicant_id), integer(r.data.publisher_id)].includes(
      owner,
    );
  const groups = [
    ["协议版本", objects(r.data.contracts)],
    ["安全接回方案", objects(r.data.return_plans)],
  ] as const;
  return (
    <>
      {groups.map(([title, versions]) => (
        <Card title={title} key={title}>
          {!versions.length ? (
            <p className="muted">暂无已提交版本</p>
          ) : (
            versions.map((v) => {
              const revision = integer(v.number);
              const status = String(v.status ?? v.state ?? "");
              return (
                <section
                  className="stack version-section"
                  key={String(revision)}
                >
                  <strong>
                    {title === "安全接回方案" ? "R" : "V"}
                    {String(revision)} · {stateLabel(status)}
                  </strong>
                  <Facts value={v.terms} />
                  <p className="caption muted">
                    已确认用户：
                    {(Array.isArray(v.accepted_by)
                      ? v.accepted_by
                      : Array.isArray(v.confirmations)
                        ? v.confirmations
                        : []
                    )
                      .map(String)
                      .join("、") || "暂无"}
                  </p>
                  {party &&
                  title === "协议版本" &&
                  status === "pending" &&
                  !has(v.accepted_by, owner) ? (
                    <>
                      <RowLink
                        title="核对条款并验证本次签约用途"
                        to={workflowURL("contract-accept", "adoptions", r.id, {
                          revision: String(revision),
                        })}
                      />
                      <RowLink
                        title="拒绝此提案"
                        to={workflowURL("contract-decline", "adoptions", r.id, {
                          revision: String(revision),
                        })}
                      />
                    </>
                  ) : null}
                  {title === "安全接回方案"
                    ? (["accept", "withdraw", "decline", "handover"] as const)
                        .filter((action) =>
                          returnActionAllowed(r, owner, action, revision),
                        )
                        .map((action) => (
                          <RowLink
                            key={action}
                            title={
                              {
                                accept: "核对并同意此接回方案",
                                withdraw: "撤回本人未生效提案",
                                decline: "拒绝此接回方案",
                                handover: "确认本人已完成接回交接",
                              }[action]
                            }
                            to={workflowURL(
                              "return-action",
                              "adoptions",
                              r.id,
                              { action, revision: String(revision) },
                            )}
                          />
                        ))
                    : null}
                </section>
              );
            })
          )}
        </Card>
      ))}
    </>
  );
}
function CommentBatch({
  kind,
  rows,
  onReply,
}: {
  kind: "entry_comments" | "pet_comments";
  rows: RecordData[];
  onReply: (id: bigint) => void;
}) {
  const owner = useSession().user?.id ?? 0n;
  const path = `comment-metrics?kind=${kind}&ids=${rows.map((row) => row.id).join(",")}`;
  const metrics = useQuery<
    Array<{ comment_id: bigint; likes: bigint; viewer_liked: boolean }>
  >(rows.length ? path : null);
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function like(id: bigint, liked: boolean) {
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await api.request(`${kind}/${id}/reaction`, {
        method: "POST",
        body: { liked },
        headers: { "Idempotency-Key": key() },
        owner,
      });
      await reload(path);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      {rows.map((row) => {
        const metric = metrics.data?.find((item) => item.comment_id === row.id);
        return (
          <section className="comment-row" key={String(row.id)}>
            <Author id={row.owner_id} />
            {integer(row.data.reply_to) > 0n ? (
              <p className="caption muted">
                回复评论 #{String(row.data.reply_to)}
              </p>
            ) : null}
            <RichText content={row.data} />
            <div className="comment-actions">
              <Button kind="quiet" onClick={() => onReply(row.id)}>
                回复
              </Button>
              <Button
                kind="quiet"
                pressed={metric?.viewer_liked}
                disabled={busy || !owner || !metric}
                onClick={() =>
                  metric && void like(row.id, !metric.viewer_liked)
                }
              >
                <Icon
                  name={
                    metric?.viewer_liked
                      ? "comment-heart-liked"
                      : "comment-heart"
                  }
                />
                {metric?.viewer_liked ? "已赞" : "点赞"} ·{" "}
                {metric?.likes.toString() ?? "—"}
              </Button>
              <details className="comment-menu">
                <summary aria-label="评论操作">
                  <Icon name="comment-more" />
                </summary>
                <div className="comment-menu-items">
                  <RowLink
                    title="举报此评论"
                    to={workflowURL("review", kind, row.id, { type: "report" })}
                  />
                  {row.owner_id === owner ? (
                    <RowLink
                      title="删除此评论"
                      to={workflowURL("delete", kind, row.id)}
                    />
                  ) : null}
                </div>
              </details>
            </div>
          </section>
        );
      })}
      {metrics.error ? (
        <Notice title="点赞状态暂不可用">
          <Button onClick={() => void reload(path)}>重新读取</Button>
        </Notice>
      ) : null}
      {error ? <Notice title={error} error /> : null}
    </>
  );
}
function Comments({
  showTitle = true,
  kind = "entry_comments",
  entry,
  reply,
  onReply,
}: {
  entry: bigint;
  kind?: "entry_comments" | "pet_comments";
  showTitle?: boolean;
  reply: bigint;
  onReply: (id: bigint) => void;
}) {
  const page = usePages(kind, { reference_id: entry });
  return (
    <section className="stack" aria-label="评论">
      {showTitle ? (
        <h3>{kind === "pet_comments" ? "领养交流" : "评论"}</h3>
      ) : null}
      <QueryState
        loading={page.loading}
        error={page.error}
        failure={page.failure}
        readAt={page.readAt}
        onRetry={page.retry}
        keepContent={!!page.rows.length && !page.denied}
      >
        {Array.from(
          { length: Math.ceil(page.rows.length / 50) },
          (_, index) => (
            <CommentBatch
              key={index}
              kind={kind}
              rows={page.rows.slice(index * 50, index * 50 + 50)}
              onReply={onReply}
            />
          ),
        )}
        <PageEnd page={page} />
      </QueryState>
      {reply ? (
        <Notice title={`回复评论 #${reply}`}>
          <Button kind="quiet" onClick={() => onReply(0n)}>
            取消回复
          </Button>
        </Notice>
      ) : null}
    </section>
  );
}
export function Member({ id }: { id: bigint }) {
  const result = useQuery<User>(`users/${id}`);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function converse() {
    setBusy(true);
    try {
      const result = await api.request<RecordData>("conversations", {
        method: "POST",
        body: { user_id: id, client_request_id: key() },
      });
      navigate(`/conversations/${result.id}`);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <TaskPage title={result.data?.nickname || "宠友主页"}>
      <QueryState
        loading={result.loading}
        error={result.error}
        failure={result.failure}
        readAt={result.readAt}
      >
        {result.data ? (
          <>
            <Card>
              <h2>{result.data.nickname}</h2>
              <p>{result.data.bio || "这位宠友还没有填写介绍"}</p>
              <ContentSubscription id={id} kind="user" />
              <RowLink
                title="举报这份用户资料"
                to={workflowURL("review", "users", id, { type: "report" })}
              />
              <RowLink title="查看对方公开动态" to={`/entries?owner=${id}`} />
              <RowLink
                title="发送好友申请"
                to={workflowURL("friend", "users", id)}
              />
              <Button onClick={() => void converse()} disabled={busy}>
                发起私信请求
              </Button>
            </Card>
            {error ? <Notice title={error} error /> : null}
          </>
        ) : null}
      </QueryState>
    </TaskPage>
  );
}
