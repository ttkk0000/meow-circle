import { useEffect, useState } from "react";
import { WorkspaceShell } from "../components/WorkspaceShell";
import { Button, Field, Modal } from "../components/ui";
import { navigate, useRoute } from "../core/router";
import { OpsNotice } from "../operations/ui";
import {
  AccessPage,
  EmergencyPage,
  ReadinessPage,
  SystemPage,
} from "./ContextPages";
import {
  ConfirmPage,
  DetailPage,
  PendingPage,
  PrivacyPage,
} from "./DetailPages";
import { Dashboard, QueuePage } from "./QueuePages";
import {
  listView,
  pagePath,
  sections,
  titles,
  views,
  type Kind,
  type View,
} from "./models";
import { useOperator } from "./useOperator";
import {
  assignedWorkspaces,
  workspaceAllows,
  workspaceFor,
  type BusinessRole,
} from "./roleWorkspaces";
import {
  isRoleTask,
  RoleTaskPage,
  RoleRevenuePage,
  useRoleTasks,
} from "./RoleTasks";
import "./operator.css";

export default function OperatorApp() {
  const route = useRoute(),
    params = new URLSearchParams(route.split("?")[1]);
  const raw = params.get("view") ?? "dashboard";
  // 兼容旧运营入口；详情须独立回读，不从URL恢复材料或凭据。
  const normalized = raw === "case" ? "rights" : raw;
  const view: View = views.includes(normalized as View)
    ? (normalized as View)
    : "dashboard";
  const selected = params.get("id") ?? "",
    id = /^[1-9][0-9]{0,18}$/.test(selected) ? selected : "";
  const session = useOperator(),
    [connection, setConnection] = useState(false),
    [token, setToken] = useState("");
  const roleTasks = useRoleTasks(session);
  const [selectedRole, setSelectedRole] = useState<BusinessRole>();
  const [switchTo, setSwitchTo] = useState<BusinessRole>();
  useEffect(() => {
    setSelectedRole(undefined);
    setSwitchTo(undefined);
  }, [session.context?.id]);
  const workspaces = assignedWorkspaces(session.context?.roles ?? []);
  const workspace = workspaceFor(
    workspaces.some((w) => w.role === selectedRole)
      ? selectedRole
      : workspaces[0]?.role,
  );
  const go = (next: View, nextID?: string) => {
    if (roleTasks.locked && next !== view) return;
    navigate(pagePath(next, nextID));
  };
  const connect = () => {
    setToken("");
    setConnection(true);
  };
  const kind: Kind | undefined =
    view === "reviews" || view === "reviewDetail"
      ? "reviews"
      : view === "rights" || view === "rightsDetail"
        ? "rights-cases"
        : view === "privacy"
          ? "data-requests"
          : view === "guardian"
            ? "guardian-invites"
            : undefined;
  const detailRoute =
    view === "reviewDetail" ||
    view === "rightsDetail" ||
    (view === "privacy" && !!id) ||
    (view === "guardian" && !!id);
  const requiredKind =
    kind ??
    (view === "confirm" || view === "pending"
      ? session.prepared?.kind
      : undefined);
  const current: View =
    session.forbidden ||
    (session.context &&
      !workspaceAllows(workspace?.role, view, session.prepared?.kind)) ||
    (session.context && requiredKind && !session.permits(requiredKind))
      ? "forbidden"
      : !session.context &&
          !(
            [
              "dashboard",
              "readiness",
              "access",
              "forbidden",
              "emergency",
            ] as View[]
          ).includes(view)
        ? "disconnected"
        : view;
  useEffect(() => {
    document.title = titles[current] + " · M&D 后台管理";
  }, [current]);
  useEffect(() => {
    if (!session.context || current === "forbidden") return;
    if (kind && detailRoute && id) void session.loadDetail(kind, id);
    else {
      session.clearDetail();
      if (kind && kind !== "data-requests") void session.readQueue(kind);
    }
  }, [
    view,
    id,
    session.context?.id,
    session.client.epoch,
    current === "forbidden",
  ]);
  useEffect(() => {
    if (
      view === "confirm" &&
      session.prepared &&
      session.prepared.phase !== "ready"
    )
      go("pending");
  }, [view, session.prepared?.phase]);
  const active =
    current === "reviewDetail"
      ? "reviews"
      : current === "rightsDetail"
        ? "rights"
        : current === "confirm" || current === "pending"
          ? session.prepared
            ? listView(session.prepared.kind)
            : "access"
          : current === "disconnected" || current === "forbidden"
            ? "access"
            : current;
  return (
    <>
      <WorkspaceShell
        title={
          current === "dashboard" && workspace
            ? workspace.home
            : titles[current]
        }
        heading="后台管理 / 业务与核验"
        caption={workspace?.label ?? "角色与身份"}
        breadcrumb={"业务管理 / " + titles[current]}
        subtitle="具名角色与当前版本 · 按事项读取、核实与处理"
        identity={session.context?.id ?? "尚未连接账号"}
        roles={
          session.context?.roles.join(" / ") ?? "独立具名凭据 / 按事项授权"
        }
        scope={
          <>
            <strong>{workspace?.label ?? "无人工处理工作区"}</strong>
            <small>仅处理当前角色授权事项</small>
          </>
        }
        navigation={[
          ...sections
            .filter(
              ([key]) =>
                workspaceAllows(workspace?.role, key) &&
                (key !== "dashboard" || !!workspace),
            )
            .map(([key, label, icon]) => ({
              key,
              label,
              icon,
              href: "/app" + pagePath(key),
              onSelect: () => go(key),
              collection:
                key === "reviews" || key === "privacy" || key === "guardian"
                  ? ("figma" as const)
                  : ("ops" as const),
            })),
          ...(session.context?.roles.some((r) => r.startsWith("ops_"))
            ? [
                {
                  key: "technical",
                  label: "技术运维",
                  icon: "sliders-horizontal",
                  href: "/app/ops",
                },
              ]
            : []),
        ]}
        active={active}
        focusKey={current + ":" + id}
        className="operator-workspace"
        actions={
          <>
            {workspaces.length > 1 ? (
              <label className="field">
                <span>当前岗位</span>
                <select
                  aria-label="切换已授权岗位"
                  value={workspace?.role ?? ""}
                  disabled={
                    session.busy ||
                    !!session.prepared ||
                    roleTasks.locked ||
                    roleTasks.reading
                  }
                  onChange={(event) =>
                    setSwitchTo(event.target.value as BusinessRole)
                  }
                >
                  {workspaces.map((w) => (
                    <option key={w.role} value={w.role}>
                      {w.label}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            {session.context ? (
              <>
                <Button
                  disabled={session.busy}
                  onClick={() => void session.refresh()}
                >
                  核对本人角色
                </Button>
                <Button
                  kind="quiet"
                  onClick={() => {
                    session.disconnect();
                    setToken("");
                    setConnection(false);
                  }}
                >
                  断开
                </Button>
              </>
            ) : (
              <Button onClick={connect}>连接</Button>
            )}
            <span className="ops-environment-mark">
              {session.context
                ? "已连接具名会话 · 按权限处理"
                : "未连接业务后台"}
            </span>
          </>
        }
        footer={"M&D 后台管理 / 材料与处理结果以当前角色读取的服务端事实为准。"}
      >
        {session.error ? <OpsNotice title={session.error} error /> : null}
        {session.busy ? (
          <p role="status">正在读取或提交，请保留当前事项…</p>
        ) : null}
        {session.prepared &&
        current !== "confirm" &&
        current !== "pending" &&
        current !== "forbidden" &&
        current !== "disconnected" ? (
          <OpsNotice title="原操作记录仍保留">
            <Button
              onClick={() =>
                go(session.prepared?.phase === "ready" ? "confirm" : "pending")
              }
            >
              继续核对原操作
            </Button>
          </OpsNotice>
        ) : null}
        {roleTasks.lostReference ? (
          <OpsNotice title="原登记仍需核对">
            恢复引用：{roleTasks.lostReference}
            。身份或岗位范围已变化，原材料已清除；请由有权限的人员核对，不能据此认定成功或失败。
          </OpsNotice>
        ) : null}
        {current === "dashboard" ? (
          <Dashboard session={session} workspace={workspace?.role} go={go} />
        ) : isRoleTask(current) ? (
          <RoleTaskPage key={current} flow={current} tasks={roleTasks} />
        ) : current === "revenue" ? (
          <RoleRevenuePage tasks={roleTasks} />
        ) : current === "reviews" ? (
          <QueuePage session={session} kind="reviews" go={go} />
        ) : current === "rights" ? (
          <QueuePage session={session} kind="rights-cases" go={go} />
        ) : current === "reviewDetail" || current === "rightsDetail" ? (
          <DetailPage key={current + "/" + id} session={session} go={go} />
        ) : current === "guardian" ? (
          id ? (
            <DetailPage key={"guardian/" + id} session={session} go={go} />
          ) : (
            <QueuePage session={session} kind="guardian-invites" go={go} />
          )
        ) : current === "privacy" ? (
          <PrivacyPage key={id} session={session} go={go} id={id} />
        ) : current === "readiness" ? (
          <ReadinessPage session={session} />
        ) : current === "access" ? (
          <AccessPage session={session} connect={connect} go={go} />
        ) : current === "confirm" ? (
          <ConfirmPage session={session} go={go} />
        ) : current === "pending" ? (
          <PendingPage session={session} go={go} />
        ) : current === "emergency" ? (
          <EmergencyPage go={go} />
        ) : (
          <SystemPage
            session={session}
            forbidden={current === "forbidden"}
            connect={connect}
            go={go}
          />
        )}
      </WorkspaceShell>
      {switchTo ? (
        <Modal
          title="切换已授权岗位"
          busy={session.busy}
          onClose={() => setSwitchTo(undefined)}
          actions={
            <>
              <Button onClick={() => setSwitchTo(undefined)}>取消</Button>
              <Button
                kind="primary"
                disabled={
                  session.busy ||
                  !!session.prepared ||
                  roleTasks.locked ||
                  roleTasks.reading
                }
                onClick={() => {
                  if (
                    workspaces.some((w) => w.role === switchTo) &&
                    session.clearWorkspace()
                  ) {
                    roleTasks.reset();
                    setSelectedRole(switchTo);
                    setSwitchTo(undefined);
                    navigate(pagePath("dashboard"));
                  }
                }}
              >
                清空当前输入并切换
              </Button>
            </>
          }
        >
          <p>
            切换到{workspaceFor(switchTo)?.label}
            。当前未提交输入和已读材料将清空，服务端角色与已完成事项不变。结果待核对的操作须先结束核对。
          </p>
        </Modal>
      ) : null}
      {connection ? (
        <Modal
          title="连接业务后台"
          busy={session.busy}
          onClose={() => {
            setToken("");
            setConnection(false);
          }}
          actions={
            <>
              <Button
                disabled={session.busy}
                onClick={() => {
                  setToken("");
                  setConnection(false);
                }}
              >
                取消
              </Button>
              <Button
                kind="primary"
                disabled={
                  session.busy ||
                  token.trim().length < 32 ||
                  token.length > 4096
                }
                onClick={() => {
                  const value = token.trim();
                  setToken("");
                  setConnection(false);
                  void session.connect(value);
                }}
              >
                读取本人角色与范围
              </Button>
            </>
          }
        >
          <p>
            使用部署方分配的具名运营凭据。切换账号会取消原请求、清除原事项和本机核对记录。
          </p>
          <Field
            label="本机运营访问凭据"
            type="password"
            value={token}
            onChange={setToken}
            disabled={session.busy}
            autoComplete="off"
            maxLength={4096}
            hint="只在当前页面内存使用，不放入网址、消费者会话或持久存储。"
          />
        </Modal>
      ) : null}
    </>
  );
}
