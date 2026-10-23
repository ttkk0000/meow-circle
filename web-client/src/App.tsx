import { Component, useEffect, useState, type ReactNode } from "react";
import {
  api,
  explain,
  type Inbox,
  type Page,
  type RecordData,
} from "./core/api";
import { useSession } from "./core/session";
import { back, navigate, resourceRoute, useRoute } from "./core/router";
import { useQuery, reload, invalidateQueries } from "./core/query";
import { integer } from "./core/wire";
import { usePages } from "./core/pages";
import {
  Badge,
  Button,
  Card,
  Check,
  Empty,
  Icon,
  Modal,
  Notice,
  QueryState,
  RowLink,
  TaskPage,
} from "./components/ui";
import { AuthDialog } from "./features/AuthDialog";
import { Mine, Rules, Security, type Profile } from "./features/Account";
import { Feed, PageEnd, Records, titles } from "./features/Records";
import { ResourcePage, Member } from "./features/ResourcePage";
import { Chat } from "./features/chat";
import { Recovery } from "./features/Recovery";
import { MapPage } from "./features/MapPage";
import { ContentPage } from "./features/ContentPage";
import { WorkflowEditor } from "./features/WorkflowEditor";
import { workflows, workflowURL, type Context } from "./features/workflows";

const navigation = [
  ["/", "社区", "house"],
  ["/pets", "领养", "heart-handshake"],
  ["/publish", "发布", "circle-plus"],
  ["/cases", "互助", "paw-print"],
  ["/messages", "消息", "message-circle"],
  ["/mine", "我的", "user-round"],
];
export const moduleLinks: Record<string, Array<[string, string]>> = {
  community: [
    ["/", "推荐日常"],
    ["/?space=following", "关注"],
    ["/?space=local", "同城"],
    ["/?space=circle", "宠友圈"],
    ["/?space=diary", "宠物日记"],
    ["/friendships", "好友与邀请"],
  ],
  care: [
    ["/pets", "领养介绍"],
    ["/adoptions", "领养申请"],
    ["/appointments", "预约与到访"],
    ["/offers", "有偿信息与披露"],
  ],
  publish: [
    ["/publish", "选择发布类型"],
    [workflowURL("entry"), "晒晒我的宠物"],
    ["/recovery", "本机草稿与原操作"],
  ],
  cases: [
    ["/cases", "走失与拾获"],
    ["/map", "互助地图"],
    ["/nearby", "附近提醒"],
    [workflowURL("case"), "发布求助"],
    ["/claims", "我的认领申请"],
  ],
  messages: [
    ["/messages", "聊天与未读"],
    ["/messages?tab=interactions", "社区互动"],
    ["/messages?tab=notices", "站内通知"],
    ["/invitations", "消息请求与邀请"],
    ["/groups", "我的群聊"],
    ["/group-directory", "发现宠友群"],
    ["/settings", "提醒设置"],
  ],
  account: [
    ["/mine", "我的资料"],
    ["/security", "账号安全"],
    ["/guardians", "监护授权"],
    ["/reviews", "核实、举报与申诉"],
    ["/recovery", "待核对与受限事项"],
    ["/rules", "规则与权益"],
  ],
};
function moduleFor(path: string, params: URLSearchParams) {
  if (
    path === "/publish" ||
    (path === "/edit" &&
      ["entry", "pet", "case", "offer"].includes(params.get("flow") ?? ""))
  )
    return "publish";
  if (
    /^\/(messages|notifications|conversations|groups|group-directory|invitations)/.test(
      path,
    )
  )
    return "messages";
  if (/^\/(cases|claims|leads|nearby)/.test(path)) return "cases";
  if (/^\/(pets|adoptions|appointments|offers|care-updates)/.test(path))
    return "care";
  if (path === "/" || path.startsWith("/entries")) return "community";
  return "account";
}
const moduleTitles: Record<string, string> = {
  community: "社区与宠友圈",
  care: "领养与照护",
  publish: "发布与素材",
  cases: "同城互助",
  messages: "消息与未读",
  account: "我的任务与设置",
};
function ModuleNav({
  name,
  current,
  onSelect,
}: {
  name: string;
  current: string;
  onSelect: () => void;
}) {
  return (
    <>
      <h2>{moduleTitles[name]}</h2>
      {moduleLinks[name].map(([to, label]) => (
        <button
          type="button"
          className={`nav-item ${current === to ? "selected" : ""}`}
          key={to}
          onClick={() => {
            navigate(to);
            onSelect();
          }}
        >
          {label}
        </button>
      ))}
      <RowLink title="规则与权益" to="/rules" />
    </>
  );
}
export default function App() {
  const route = useRoute();
  const u = new URL(route, "https://mnd.invalid");
  const path = u.pathname;
  const session = useSession();
  const [login, setLogin] = useState(false);
  const [menu, setMenu] = useState(false);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");
  const module = moduleFor(path, u.searchParams);
  const inbox = useQuery<Inbox>(session.authenticated ? "inbox-summary" : null);
  const task = ![
    "/",
    "/group-directory",
    "/pets",
    "/messages",
    "/mine",
    "/cases",
    "/offers",
  ].includes(path);
  useEffect(() => {
    let active = true;
    void api
      .openSession()
      .then((s) =>
        s.refreshable && !s.authenticated ? api.refresh() : undefined,
      )
      .catch((e) => {
        if (active) setError(explain(e));
      })
      .finally(() => {
        if (active) setReady(true);
      });
    return () => {
      active = false;
    };
  }, []);
  useEffect(() => {
    document.title = `${moduleTitles[module]} · M&D`;
    setMenu(false);
  }, [route, module]);
  return (
    <div
      className={`app-shell ${task ? "task-open" : ""} ${path === "/" ? "feed-open" : ""}`}
    >
      <a className="skip-link" href="#task-main">
        跳到当前任务
      </a>
      <header className="global-header">
        <button
          className="brand brand-button"
          type="button"
          onClick={() => navigate("/")}
        >
          M&amp;D
        </button>
        <nav aria-label="主导航">
          {navigation.map(([to, label, icon]) => (
            <button
              type="button"
              key={to}
              className={`nav-item ${to === "/publish" ? "publish" : ""} ${to === "/" ? (path === "/" ? "selected" : "") : path.startsWith(to) ? "selected" : ""}`}
              onClick={() => navigate(to)}
            >
              <Icon name={icon} />
              {label}
              {to === "/messages" ? (
                <Badge count={inbox.data?.global_total} />
              ) : null}
            </button>
          ))}
        </nav>
        <span className="brand-copy">meow &amp; doggie</span>
        <Button
          kind="quiet"
          onClick={() =>
            session.authenticated ? navigate("/mine") : setLogin(true)
          }
        >
          {session.user?.nickname || "登录"}
        </Button>
      </header>
      <div className="shell-workspace">
        <aside className="module-nav" aria-label="模块导航">
          <ModuleNav name={module} current={route} onSelect={() => {}} />
        </aside>
        <main id="task-main" className="task-workspace" tabIndex={-1}>
          <div className="task-toolbar">
            {path !== "/" ? (
              <button
                type="button"
                className="toolbar-icon"
                aria-label="返回当前任务上一步"
                onClick={back}
              >
                <Icon name="chevron-left" />
              </button>
            ) : null}
            <h2>{moduleTitles[module]}</h2>
            <button
              type="button"
              className="toolbar-icon module-menu"
              aria-label="打开当前模块入口"
              onClick={() => setMenu(true)}
            >
              <Icon name="ellipsis" />
            </button>
          </div>
          {error ? (
            <Notice title="浏览器会话暂不可用" error>
              {error} · 公开内容仍可浏览。
              <Button
                kind="quiet"
                onClick={() => {
                  void api
                    .openSession()
                    .then(() => {
                      setError("");
                      setReady(true);
                    })
                    .catch((e) => setError(explain(e)));
                }}
              >
                重试连接
              </Button>
            </Notice>
          ) : null}
          {ready ? (
            <PageBoundary key={`${api.epoch}:${route}`}>
              <RouteContent
                key={`${api.epoch}:${route}`}
                path={path}
                params={u.searchParams}
                login={() => setLogin(true)}
              />
            </PageBoundary>
          ) : (
            <p className="loading">正在恢复浏览器会话…</p>
          )}
        </main>
      </div>
      <nav className="bottom-nav" aria-label="手机主导航">
        {navigation
          .filter(([to]) => to !== "/cases")
          .map(([to, label, icon]) => (
            <button
              type="button"
              key={to}
              className={`nav-item ${path === to ? "selected" : ""}`}
              onClick={() => navigate(to)}
            >
              <Icon name={icon} />
              {label}
              {to === "/messages" ? (
                <Badge count={inbox.data?.global_total} />
              ) : null}
            </button>
          ))}
      </nav>
      {login ? <AuthDialog onClose={() => setLogin(false)} /> : null}
      {menu ? (
        <Modal
          title={moduleTitles[module]}
          onClose={() => setMenu(false)}
          actions={
            <>
              <Button
                onClick={() => {
                  invalidateQueries();
                  setMenu(false);
                }}
              >
                重新读取真实记录
              </Button>
              <Button onClick={() => setMenu(false)}>返回当前任务</Button>
            </>
          }
        >
          <ModuleNav
            name={module}
            current={route}
            onSelect={() => setMenu(false)}
          />
        </Modal>
      ) : null}
    </div>
  );
}
const lists = new Set([
  "group-directory",
  "entries",
  "pets",
  "cases",
  "offers",
  "adoptions",
  "appointments",
  "friendships",
  "invitations",
  "groups",
  "reviews",
  "claims",
  "guardians",
  "care-updates",
  "leads",
]);
const anonymous = new Set([
  "/",
  "/group-directory",
  "/pets",
  "/cases",
  "/map",
  "/offers",
  "/rules",
  "/publish",
  "/mine",
  "/recovery",
]);
function RouteContent({
  path,
  params,
  login,
}: {
  path: string;
  params: URLSearchParams;
  login: () => void;
}) {
  const session = useSession();
  const pieces = path.split("/").filter(Boolean);
  const kind = pieces[0];
  const id = integer(pieces[1]);
  const publicResource =
    ["group-directory", "entries", "pets", "cases", "offers", "users"].includes(
      kind,
    ) && id > 0n;
  const guestForm =
    path === "/edit" && workflows[params.get("flow") ?? ""]?.guest;
  if (
    !session.authenticated &&
    !anonymous.has(path) &&
    !publicResource &&
    !guestForm
  )
    return (
      <TaskPage title="登录后继续当前任务">
        <Notice title="当前输入与任务会保留">
          登录后重新核对账号权限，再由你主动继续。
        </Notice>
        <Button kind="primary" onClick={login}>
          登录并返回此处
        </Button>
      </TaskPage>
    );
  if (path === "/") return <Feed params={params} />;
  if (path === "/mine") return <Mine login={login} />;
  if (path === "/rules") return <Rules />;
  if (path === "/map") return <MapPage />;
  if (path === "/content") return <ContentPage />;
  if (path === "/publish") return <Publish />;
  if (path === "/messages" || path === "/notifications")
    return <InboxPage params={params} />;
  if (path === "/recovery") return <Recovery />;
  if (path === "/security") return <Security />;
  if (path === "/profile-edit") return <ProfileEditor />;
  if (path === "/settings" || path === "/nearby")
    return <SettingsEditor nearby={path === "/nearby"} />;
  if (path === "/edit") return <EditorRoute params={params} />;
  if (pieces.length === 1 && lists.has(kind))
    return (
      <Records
        kind={kind === "care-updates" ? "care_updates" : kind}
        params={params}
      />
    );
  if (pieces.length === 2 && id > 0n && resourceRoute(kind, id)) {
    if (kind === "users") return <Member id={id} />;
    if (
      (kind === "groups" || kind === "conversations") &&
      params.get("view") !== "manage"
    )
      return <Chat kind={kind} id={id} />;
    return <ResourcePage kind={kind} id={id} />;
  }
  return (
    <TaskPage title="此入口暂不可用">
      <Empty title="没有找到这个页面">
        <Button onClick={() => navigate("/")}>回到社区</Button>
      </Empty>
    </TaskPage>
  );
}
function Publish() {
  return (
    <TaskPage title="今天想记录什么？">
      <Card>
        <RowLink
          title="晒晒我的宠物"
          detail="社区分享、宠友圈与私密日记"
          to={workflowURL("entry")}
        />
        <RowLink title="新增宠物档案" to={workflowURL("pet")} />
        <RowLink title="走失／拾获求助" to={workflowURL("case")} />
        <RowLink title="有偿信息与完整披露" to={workflowURL("offer")} />
        <RowLink title="本机草稿与原操作" to="/recovery" />
      </Card>
    </TaskPage>
  );
}
function EditorRoute({ params }: { params: URLSearchParams }) {
  const flow = params.get("flow") ?? "";
  const kind = params.get("kind") ?? "";
  const id = integer(params.get("id"));
  const resource = id > 0n && !!resourceRoute(kind, id);
  const result = useQuery<RecordData>(resource ? `${kind}/${id}` : null);
  if (resource && (result.loading || !result.data))
    return (
      <QueryState
        loading={result.loading}
        error={result.error}
        onRetry={() => void reload(`${kind}/${id}`)}
      />
    );
  const context: Context = { kind, id, record: result.data, params };
  return <WorkflowEditor flow={flow} context={context} />;
}
function ProfileEditor() {
  const result = useQuery<Profile>("profile");
  if (!result.data)
    return <QueryState loading={result.loading} error={result.error} />;
  const p = result.data;
  const record: RecordData = {
    id: p.user.id,
    owner_id: p.user.id,
    version: p.version,
    reference_id: 0n,
    state: "active",
    media_ids: [],
    created_at: "",
    updated_at: "",
    data: {
      nickname: p.user.nickname,
      bio: p.user.bio ?? "",
      theme: p.theme,
      background: p.background,
      avatar_media_id: integer(
        p.user.avatar_url?.match(/\/media\/(\d+)\/content/)?.[1],
      ),
    },
  };
  return (
    <WorkflowEditor
      flow="profile"
      context={{
        kind: "profile",
        id: 0n,
        params: new URLSearchParams(),
        record,
      }}
    />
  );
}
function SettingsEditor({ nearby }: { nearby: boolean }) {
  const path = nearby ? "subscriptions" : "preferences";
  const result = useQuery<Page>(path);
  if (!result.data)
    return <QueryState loading={result.loading} error={result.error} />;
  const record = result.data.items[0];
  return (
    <WorkflowEditor
      flow={nearby ? "nearby" : "settings"}
      context={{ kind: path, id: 0n, record, params: new URLSearchParams() }}
    />
  );
}
function InboxPage({ params }: { params: URLSearchParams }) {
  const session = useSession();
  const tab = params.get("tab") ?? "chat";
  const [unread, setUnread] = useState(false);
  const [error, setError] = useState("");
  const summary = useQuery<Inbox>("inbox-summary");
  const direct = usePages("conversations", {}, tab === "chat");
  const groups = usePages("groups", {}, tab === "chat");
  const notices = usePages("notifications", {}, tab !== "chat");
  const notificationRows = notices.rows.filter(
    (row) =>
      (!unread || !row.data.read) &&
      (tab === "interactions"
        ? row.data.category === "interactions"
        : !["direct_messages", "groups", "interactions"].includes(
            String(row.data.category),
          )),
  );
  const sum = (values: Record<string, bigint> | undefined) =>
    Object.values(values ?? {}).reduce((a, b) => a + b, 0n);
  const counts = {
    chat: sum(summary.data?.direct) + sum(summary.data?.groups),
    interactions: summary.data?.notifications.interactions,
    notices: Object.entries(summary.data?.notifications ?? {})
      .filter(
        ([key]) => !["direct_messages", "groups", "interactions"].includes(key),
      )
      .reduce((a, [, b]) => a + b, 0n),
  };
  async function openNotice(row: RecordData) {
    setError("");
    try {
      if (!row.data.read) {
        await api.request(`notifications/${row.id}/read`, {
          method: "POST",
          body: { version: row.version },
        });
        notices.refresh();
        void reload("inbox-summary");
      }
      const to = resourceRoute(
        String(row.data.target_kind),
        row.data.target_id,
      );
      if (to) navigate(to);
      else setError("原事项当前没有可打开的入口，请核对通知文本");
    } catch (e) {
      setError(explain(e));
    }
  }
  return (
    <TaskPage title="消息">
      <div className="feed-tabs" aria-label="消息分类">
        {[
          ["chat", "聊天"],
          ["interactions", "互动"],
          ["notices", "通知"],
        ].map(([value, label]) => (
          <button
            type="button"
            className={`button ${tab === value ? "selected" : "quiet"}`}
            key={value}
            onClick={() => navigate(`/messages?tab=${value}`)}
          >
            {label}
            <Badge count={counts[value as keyof typeof counts]} />
          </button>
        ))}
      </div>
      <Check checked={unread} onChange={setUnread}>
        仅看未读
      </Check>
      {error ? <Notice title={error} error /> : null}
      {tab === "chat" ? (
        <>
          <Notice
            title={`消息请求 ${summary.data ? String(summary.data.message_requests) : "—"}`}
          >
            待处理事项 {summary.data ? String(sum(summary.data.pending)) : "—"}
            ，与未读分开计算。
            <RowLink title="查看领养、预约与邀请" to="/mine" />
          </Notice>
          <QueryState
            loading={direct.loading || groups.loading}
            error={direct.error || groups.error}
            onRetry={() => {
              direct.refresh();
              groups.refresh();
            }}
          >
            {direct.rows
              .filter(
                (row) =>
                  !unread || integer(summary.data?.direct[String(row.id)]) > 0n,
              )
              .map((row) => {
                const peer =
                  integer(row.data.requester_id) === session.user?.id
                    ? integer(row.data.recipient_id)
                    : integer(row.data.requester_id);
                return (
                  <MessageRow
                    key={String(row.id)}
                    title={`宠友 #${peer}`}
                    detail={
                      row.state === "pending"
                        ? "接受后才可发送照片"
                        : "日常交流"
                    }
                    count={summary.data?.direct[String(row.id)]}
                    onClick={() => navigate(`/conversations/${row.id}`)}
                  />
                );
              })}
            <PageEnd page={direct} />
            {groups.rows
              .filter(
                (row) =>
                  !unread || integer(summary.data?.groups[String(row.id)]) > 0n,
              )
              .map((row) => (
                <MessageRow
                  key={String(row.id)}
                  title={String(row.data.name)}
                  detail={
                    Array.isArray(row.data.muted) &&
                    row.data.muted.includes(session.user?.id)
                      ? "免打扰 · 不计入全局总数"
                      : "群聊"
                  }
                  muted={
                    Array.isArray(row.data.muted) &&
                    row.data.muted.includes(session.user?.id)
                  }
                  count={summary.data?.groups[String(row.id)]}
                  onClick={() => navigate(`/groups/${row.id}`)}
                />
              ))}
            <PageEnd page={groups} />
          </QueryState>
        </>
      ) : (
        <QueryState
          loading={notices.loading}
          error={notices.error}
          onRetry={notices.refresh}
        >
          {notificationRows.map((row) => (
            <MessageRow
              key={String(row.id)}
              title={tab === "interactions" ? "社区互动" : "业务与系统通知"}
              detail={String(row.data.text)}
              count={row.data.read ? 0n : 1n}
              onClick={() => void openNotice(row)}
            />
          ))}
          <PageEnd page={notices} />
        </QueryState>
      )}
    </TaskPage>
  );
}
function MessageRow({
  title,
  detail,
  count,
  muted = false,
  onClick,
}: {
  title: string;
  detail: string;
  count: unknown;
  muted?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={`message-row ${muted ? "muted-thread" : ""}`}
      onClick={onClick}
    >
      <span className="message-icon">
        <Icon name={muted ? "bell" : "message-circle"} />
      </span>
      <span className="message-copy">
        <strong>{title}</strong>
        <small>{detail}</small>
      </span>
      <Badge count={count} />
    </button>
  );
}
class PageBoundary extends Component<
  { children: ReactNode },
  { failed: boolean }
> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  render() {
    return this.state.failed ? (
      <TaskPage title="此页面暂不可用">
        <Notice title="页面未能完整读取" error>
          本机草稿保留，请回到任务后重新载入。
        </Notice>
        <Button onClick={() => navigate("/recovery")}>
          查看本机草稿与原操作
        </Button>
      </TaskPage>
    ) : (
      this.props.children
    );
  }
}
