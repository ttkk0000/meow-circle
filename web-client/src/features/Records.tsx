import { CityField } from "../components/CityField";
import { CatPlaceholder } from "../components/CatPlaceholder";
import { useState, type ReactNode } from "react";
import { api, explain, query, type RecordData, type User } from "../core/api";
import { usePages, titleOf } from "../core/pages";
import { reload, useQuery } from "../core/query";
import { navigate, resourceRoute, replaceFilters } from "../core/router";
import { integer, money } from "../core/wire";
import { useSession } from "../core/session";
import {
  Button,
  Card,
  Choices,
  Empty,
  Field,
  Icon,
  Notice,
  PrivateImage,
  QueryState,
  RowLink,
  TaskPage,
} from "../components/ui";
import { stateLabel, workflowURL } from "./workflows";

export const titles: Record<string, string> = {
  "group-directory": "发现宠友群",
  entries: "社区",
  pets: "领养与宠物档案",
  cases: "走失互助",
  offers: "有偿信息",
  adoptions: "领养申请",
  appointments: "预约与到访",
  friendships: "好友与宠友圈",
  invitations: "邀请与协作",
  groups: "我的群聊",
  reviews: "核实、举报与申诉",
  claims: "认领记录",
  guardians: "监护授权",
  notifications: "站内通知",
  care_updates: "私密照护近况",
  leads: "私密线索",
};
export function Author({ id }: { id: bigint }) {
  const member = useQuery<User>(id > 0n ? `users/${id}` : null);
  return (
    <button
      type="button"
      className="author"
      onClick={() => navigate(`/users/${id}`)}
    >
      <PrivateImage
        id={
          integer(
            member.data?.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1],
          ) || undefined
        }
        alt="宠友头像"
        className="mini-avatar"
      />
      {member.data?.nickname || `宠友 #${id}`}
    </button>
  );
}
interface Metrics {
  entry_id: bigint;
  likes: bigint;
  comments: bigint;
  viewer_liked: boolean;
}
export function Feed({
  params = new URLSearchParams(),
}: {
  params?: URLSearchParams;
}) {
  const session = useSession();
  const [space, setSpace] = useState(params.get("space") ?? "community");
  const [word, setWord] = useState(params.get("q") ?? "");
  const [search, setSearch] = useState(word);
  const [error, setError] = useState("");
  const [pending, setPending] = useState<bigint>();
  const [city, setCity] = useState(params.get("city") ?? "");
  const page = usePages("entries", {
    space: ["following", "local"].includes(space) ? "community" : space,
    following: space === "following" ? "true" : undefined,
    city: space === "local" ? city : undefined,
    q: search,
  });
  const like = async (row: RecordData, value: boolean, metricPath: string) => {
    setPending(row.id);
    setError("");
    try {
      await api.request(`entries/${row.id}/reaction`, {
        method: "POST",
        body: { liked: value },
      });
      if (metricPath) void reload(metricPath);
    } catch (e) {
      setError(explain(e));
    } finally {
      setPending(undefined);
    }
  };
  return (
    <TaskPage title="">
      <div className="feed-tabs" role="group" aria-label="内容空间">
        {[
          ["community", "推荐"],
          ["following", "关注"],
          ["circle", "宠友圈"],
          ["local", "同城"],
        ].map(([value, label]) => (
          <button
            type="button"
            className={`button ${space === value ? "selected" : "quiet"}`}
            key={value}
            onClick={() => {
              setSpace(value);
              replaceFilters({ space: value });
            }}
          >
            {label}
          </button>
        ))}
      </div>
      {space === "local" ? (
        <CityField
          label="手动选择城市，不含精确定位"
          value={city}
          onChange={(value) => {
            setCity(value);
            replaceFilters({ city: value });
          }}
        />
      ) : null}
      <button
        className="share-prompt"
        type="button"
        onClick={() => navigate(workflowURL("entry"))}
      >
        <Icon name="camera" />
        <span>晒晒今天的小可爱…</span>
        <Icon name="chevron-right" />
      </button>
      <RowLink title="养宠指南" detail="迎接第一只猫，先做好准备" to="/guide" />
      <form
        className="search-row"
        onSubmit={(e) => {
          e.preventDefault();
          setSearch(word.trim());
          replaceFilters({ q: word.trim() });
        }}
      >
        <Field label="搜索社区" value={word} onChange={setWord} />
        <Button type="submit">搜索</Button>
      </form>
      {!session.authenticated && space !== "community" ? (
        <Notice title="登录后查看自己的宠友圈和日记" />
      ) : null}
      {error ? <Notice title={error} error /> : null}
      <QueryState
        loading={page.loading}
        error={page.error}
        failure={page.failure}
        readAt={page.readAt}
        onRetry={page.retry}
        keepContent={!!page.rows.length && !page.denied}
      >
        <div className="feed-grid">
          {Array.from(
            { length: Math.ceil(page.rows.length / 50) },
            (_, index) => (
              <FeedBatch
                key={String(page.rows[index * 50].id)}
                rows={page.rows.slice(index * 50, index * 50 + 50)}
                pending={pending}
                onLike={like}
              />
            ),
          )}
        </div>
        <PageEnd
          page={page}
          empty={
            <>
              <Empty
                title={
                  search || city || space !== "community"
                    ? "这次还没有合适的结果"
                    : "第一份日常，等你来分享"
                }
              >
                <CatPlaceholder />
                <p>
                  {search || city || space !== "community"
                    ? "可以调整搜索或城市；不会把其它范围的内容偷偷当作当前结果。"
                    : "这里还没有真实内容。可以先看看养宠指南，或记录你和宠物的一天。"}
                </p>
              </Empty>
              {search || city || space !== "community" ? (
                <Button
                  onClick={() => {
                    setWord("");
                    setSearch("");
                    setCity("");
                    setSpace("community");
                    replaceFilters({ q: "", city: "", space: "" });
                  }}
                >
                  清空条件并查看推荐
                </Button>
              ) : null}
              <Button onClick={() => navigate("/guide")}>看看养宠指南</Button>
              <Button kind="primary" onClick={() => navigate("/publish")}>
                晒晒我的宠物
              </Button>
            </>
          }
        />
      </QueryState>
    </TaskPage>
  );
}
function FeedBatch({
  rows,
  pending,
  onLike,
}: {
  rows: RecordData[];
  pending: bigint | undefined;
  onLike: (row: RecordData, value: boolean, path: string) => Promise<void>;
}) {
  const session = useSession();
  const metricPath = "entry-metrics?ids=" + rows.map((row) => row.id).join(",");
  const metrics = useQuery<Metrics[]>(metricPath);
  return (
    <>
      {metrics.error ? (
        <Notice title="互动计数暂时无法读取">
          <Button onClick={() => void reload(metricPath)}>重试读取计数</Button>
        </Notice>
      ) : null}
      {rows.map((row) => {
        const count = metrics.data?.find((m) => m.entry_id === row.id);
        return (
          <article className="card feed-card" key={String(row.id)}>
            <button
              type="button"
              className="media-link"
              onClick={() => navigate(`/entries/${row.id}`)}
            >
              <PrivateImage id={row.media_ids?.[0]} alt={titleOf(row)} />
            </button>
            <button
              type="button"
              className="feed-copy"
              onClick={() => navigate(`/entries/${row.id}`)}
            >
              <span className="feed-title">
                {(row.data.title as string) || String(row.data.text ?? "")}
              </span>
            </button>
            <div className="feed-author">
              <Author id={row.owner_id} />
            </div>
            {row.data.city ? (
              <small className="muted">{String(row.data.city)}</small>
            ) : null}
            <div className="reaction-row">
              <button
                type="button"
                aria-pressed={count?.viewer_liked ?? false}
                aria-label={count?.viewer_liked ? "取消喜欢" : "喜欢"}
                disabled={
                  pending === row.id || !session.authenticated || !count
                }
                onClick={() =>
                  void onLike(row, !count?.viewer_liked, metricPath)
                }
              >
                <Icon name="heart" />
                {count ? String(count.likes) : "—"}
              </button>
              <button
                type="button"
                onClick={() => navigate(`/entries/${row.id}`)}
                aria-label="查看评论"
              >
                <Icon name="message-circle" />
                {count ? String(count.comments) : "—"}
              </button>
            </div>
            {["ai_generated", "ai_modified"].includes(
              String(row.data.content_origin),
            ) ? (
              <p className="disclosure">含 AI 生成／修改内容</p>
            ) : null}
          </article>
        );
      })}
    </>
  );
}
export function PageEnd({
  page,
  empty,
}: {
  page: ReturnType<typeof usePages>;
  empty?: ReactNode;
}) {
  return (
    <>
      {!page.loading &&
      !page.error &&
      page.data &&
      !page.data.items.length &&
      !page.rows.length &&
      page.next === 0n
        ? (empty ?? (
            <Empty title="这里还没有内容">
              <p>当前范围暂无可显示的记录；可以调整筛选或稍后重新读取。</p>
            </Empty>
          ))
        : null}
      {page.next > 0n ? (
        <Button onClick={page.more} disabled={page.loading}>
          继续加载
        </Button>
      ) : null}
    </>
  );
}
export function RecordCard({ kind, row }: { kind: string; row: RecordData }) {
  const to = resourceRoute(kind, row.id);
  const open = () => to && navigate(to);
  if (kind === "invitations" && ["caretaker", "adoption_editor"].includes(String(row.data.scope)))
    return (
      <Card title={row.data.scope === "caretaker" ? "私密照护协作" : "领养介绍协作"}>
        <p>{stateLabel(row.state)} · 事项 #{String(integer(row.data.target_id))}</p>
        {row.data.note ? <p>{String(row.data.note)}</p> : null}
        {row.state === "pending" ? <p className="caption muted">接受截止：{String(row.data.expires_at ?? "待核对")}</p> : null}
        <RowLink title="查看邀请与当前协作" to={`/invitations/${row.id}`} />
      </Card>
    );
  if (kind === "group-directory")
    return (
      <Card title={String(row.data.name)}>
        <p>{String(row.data.summary ?? "")}</p>
        <small>
          {String(row.data.city ?? "")} · {String(row.data.member_count ?? 0n)}
          位成员 · 需审核
        </small>
        <RowLink
          title="先看群介绍，再决定加入"
          to={`/group-directory/${row.id}`}
        />
      </Card>
    );
  if (kind === "pets")
    return (
      <button type="button" className="pet-row" onClick={open}>
        <PrivateImage
          id={row.media_ids?.[0]}
          alt={String(row.data.name ?? "宠物照片")}
        />
        <span>
          <strong>{String(row.data.name ?? "宠物档案")}</strong>
          <small>
            {(
              {
                cat: "猫咪",
                dog: "狗狗",
                rabbit: "兔兔",
                other: "其他宠物",
              } as Record<string, string>
            )[String(row.data.species)] ?? "其他宠物"}{" "}
            · {String(row.data.breed || "品种未填写")}
          </small>
          <span className="caption">
            {String(row.data.bio || "查看档案与照护情况")}
          </span>
          <span className="disclosure">
            {row.data.adoption_available ? "开放领养介绍" : "宠物档案"}
          </span>
        </span>
      </button>
    );
  return (
    <Card>
      <div className="actions-row">
        <h2>
          {kind === "offers"
            ? `${String(row.data.city ?? "")} · ${String(row.data.nature === "sale" ? "出售" : "有偿转让")}`
            : titleOf(row)}
        </h2>
        <span className="caption muted">{stateLabel(row.state)}</span>
      </div>
      {row.media_ids?.length ? (
        <button type="button" className="media-link" onClick={open}>
          <PrivateImage id={row.media_ids[0]} alt={titleOf(row)} />
        </button>
      ) : null}
      {kind === "offers" ? (
        <p>
          披露合计 {money(row.data.total_cents)} · 版本 {String(row.version)}
        </p>
      ) : null}
      {kind === "cases" ? (
        <p className="caption muted">
          {String(row.data.city ?? "")} · {String(row.data.area ?? "")} ·{" "}
          {row.data.type === "found" ? "拾获待认领" : "走失求助"}
        </p>
      ) : null}
      {to ? (
        <RowLink title="查看详情与可用操作" to={to} />
      ) : (
        <p className="caption">{String(row.data.text ?? "")}</p>
      )}
    </Card>
  );
}
export function Records({
  kind,
  params = new URLSearchParams(),
}: {
  kind: string;
  params?: URLSearchParams;
}) {
  const [word, setWord] = useState(params.get("q") ?? "");
  const [search, setSearch] = useState(word);
  const [city, setCity] = useState(params.get("city") ?? "");
  const [species, setSpecies] = useState(params.get("species") ?? "");
  const filters = {
    q: search,
    city,
    species,
    adoption: kind === "pets" && !params.get("owner") ? "true" : undefined,
    owner_id: params.get("owner") ?? "",
    space: params.get("space") ?? "",
    watched: params.get("watched") ?? "",
    reference_id: params.get("ref") ?? "",
  };
  const page = usePages(kind, filters);
  const invitationScope = kind === "invitations" && integer(params.get("ref")) > 0n && ["caretaker", "adoption_editor"].includes(params.get("scope") ?? "")
    ? params.get("scope") : null;
  const rows = invitationScope ? page.rows.filter(row => row.data.scope === invitationScope) : page.rows;
  const create = (
    {
      pets: "pet",
      cases: "case",
      offers: "offer",
      groups: "group",
    } as Record<string, string>
  )[kind];
  return (
    <TaskPage
      title={titles[kind] ?? "事项记录"}
      actions={
        create ? (
          <Button kind="primary" onClick={() => navigate(workflowURL(create))}>
            {kind === "cases"
              ? "发布走失／拾获求助"
              : kind === "pets"
                ? "新增宠物档案"
                : kind === "groups"
                  ? "创建群聊"
                  : "新增记录"}
          </Button>
        ) : undefined
      }
    >
      {["pets", "cases", "offers", "group-directory"].includes(kind) ? (
        <>
          <form
            className="search-row"
            onSubmit={(e) => {
              e.preventDefault();
              setSearch(word.trim());
              replaceFilters({ q: word.trim(), city, species });
            }}
          >
            <Field label="关键词" value={word} onChange={setWord} />
            <CityField label="城市" value={city} onChange={setCity} />
            <Button type="submit">筛选</Button>
          </form>
          {kind !== "group-directory" ? (
            <Choices
              label="物种"
              value={species}
              onChange={setSpecies}
              choices={[
                ["", "全部"],
                ["cat", "猫咪"],
                ["dog", "狗狗"],
              ]}
            />
          ) : null}
        </>
      ) : null}
      {kind === "pets" ? (
        <Notice title="领养前，先了解真实照护情况">
          核对健康、来源、接待和费用披露；核实标记有明确项目与有效期，不保证全部事项。
        </Notice>
      ) : null}
      {invitationScope ? <Notice title="仅此事项的邀请与协作">事项 #{params.get("ref") ?? "待核对"}；仅显示当前账号可读取的邀请与授权。</Notice> : null}
      {kind === "cases" ? <RowLink title="附近走失提醒" to="/nearby" /> : null}
      {["pets", "cases", "offers"].includes(kind) ? (
        <RowLink title="不提供定位也可以" to="/location-help" />
      ) : null}
      <QueryState
        loading={page.loading}
        error={page.error}
        failure={page.failure}
        readAt={page.readAt}
        onRetry={page.retry}
        keepContent={!!page.rows.length && !page.denied}
      >
        {rows.map((row) => (
          <RecordCard key={String(row.id)} kind={kind} row={row} />
        ))}
        {invitationScope && page.data && !page.loading && !page.error && page.rows.length > 0 && !rows.length && page.next === 0n ? <Empty title="本事项暂无邀请或授权" /> : null}
        <PageEnd
          page={page}
          empty={
            invitationScope ? <Empty title="本事项暂无邀请或授权" /> : search || city || species ? (
              <>
                <Empty title="这次还没有合适的结果">
                  <p>可以放宽区域或条件；不把其它城市内容偷偷当成本地结果。</p>
                </Empty>
                <Button
                  onClick={() => {
                    setWord("");
                    setSearch("");
                    setCity("");
                    setSpecies("");
                    replaceFilters({ q: "", city: "", species: "" });
                  }}
                >
                  清空条件
                </Button>
              </>
            ) : undefined
          }
        />
      </QueryState>
    </TaskPage>
  );
}
