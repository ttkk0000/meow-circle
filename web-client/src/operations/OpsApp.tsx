import { useEffect, useState } from "react";
import { Button, Field, Modal } from "../components/ui";
import { navigate, useRoute } from "../core/router";
import { Operations, pagePath } from "./context";
import { demoSnapshot } from "./demo";
import { useOperations } from "./useOperations";
import { sections, sectionFor, views, formatTime, type View } from "./models";
import { OpsNotice } from "./ui";
import { Overview, Services, Access, SystemState } from "./OverviewPages";
import { Workers, Releases, Migrations, Alerts, Audit } from "./RecordPages";
import {
  WorkerDetail,
  ReleaseDetail,
  IncidentDetail,
  ConfirmRelease,
} from "./DetailPages";
import { WorkspaceShell } from "../components/WorkspaceShell";

const descriptions: Record<View, string> = {
  overview: "先确认环境，再定位服务、任务与待办；缺失数据保持未知。",
  services: "逐个查看 HTTP、数据依赖和入口信号，探针成功不等于完整健康。",
  workers: "按任务类型观察心跳、失败与执行结果，保留采样范围。",
  releases: "查看候选版本、构件与核对记录；执行后端尚未接入。",
  migrations: "核对源码版本、持久化账本与 SHA-256，异常时停止并保留证据。",
  alerts: "将告警信号归并为事件，确认影响范围、负责人和下一步。",
  audit: "按操作者、环境和资源追溯申请、被阻止、记录与最终结果。",
  access: "先建立环境身份与采集边界，再分别授权观察和处置。",
  release: "对同一目标环境逐项确认，缺失项阻止进入真实执行。",
  worker: "按执行编号定位失败原因，确认幂等性和外部回执后再决定下一步。",
  incident: "先确认影响、认领和缓解措施，恢复后继续验证与复盘。",
  disconnected: "保留导航与环境身份，清楚说明缺少的数据和接入步骤。",
  forbidden: "当前账号没有此环境或动作的权限，受限数据保持隐藏。",
  confirm: "二次核对目标与证据；缺失前置条件时保持不可提交。",
};
const titles: Record<View, string> = {
  ...Object.fromEntries(sections.map(([key, label]) => [key, label])),
  release: "发布核对",
  worker: "Worker 详情",
  incident: "事件处理",
  disconnected: "环境尚未连接",
  forbidden: "访问受限",
  confirm: "发布确认",
} as Record<View, string>;
export default function OpsApp() {
  const route = useRoute(),
    params = new URLSearchParams(route.split("?")[1]);
  const demo = params.get("mode") === "demo",
    raw = params.get("view") ?? "overview";
  const view = (views.includes(raw) ? raw : "overview") as View;
  const selectedID = params.get("id") ?? "",
    id = /^[a-zA-Z0-9_-]{1,80}$/.test(selectedID) ? selectedID : "";
  const session = useOperations(demo),
    [connection, setConnection] = useState(false),
    [token, setToken] = useState("");
  const active = sectionFor(view),
    data = demo ? demoSnapshot : session.snapshot;
  const go = (next: View, nextID?: string) =>
    navigate(pagePath(next, nextID, demo));
  const href = (next: View, nextID?: string) =>
    "/app" + pagePath(next, nextID, demo);
  useEffect(() => {
    document.title = `${titles[view]} · M&D 技术运维`;
  }, [view, id]);
  useEffect(() => {
    if (session.forbidden && view !== "forbidden") go("forbidden");
  }, [session.forbidden, view]);
  useEffect(() => {
    setToken("");
    setConnection(false);
  }, [demo]);
  function changeMode(next: boolean) {
    session.disconnect();
    setToken("");
    setConnection(false);
    navigate(pagePath("overview", undefined, next));
  }
  const environment = demo
    ? "staging · 占位"
    : (session.context?.environment ?? "未选择环境");
  const currentView = session.forbidden ? "forbidden" : view;
  const displayTitle =
    currentView === "worker" && id
      ? `Worker · ${id}`
      : (currentView === "release" || currentView === "incident") && id
        ? `${titles[currentView]} · ${id.startsWith("REL-DEMO") || id.startsWith("INC-DEMO") ? id : id.slice(0, 12)}`
        : titles[currentView];
  return (
    <Operations.Provider
      value={{ session, demo, data, view: currentView, id, go, href }}
    >
      <>
        <WorkspaceShell
          title={displayTitle}
          subtitle={descriptions[currentView]}
          heading="技术运维 / 环境工作台"
          caption="TECHNICAL OPERATIONS"
          breadcrumb={"技术运维 / " + titles[currentView]}
          identity={
            demo ? "值班账号 · 示例" : (session.context?.id ?? "尚未连接账号")
          }
          roles={
            demo
              ? "只读设计示例"
              : (session.context?.roles.join(" / ") ?? "具名凭据 / 环境范围")
          }
          scope={
            <>
              <strong>{environment}</strong>
              <small>
                {demo
                  ? "等待接入采集与权限服务"
                  : data
                    ? "采集结果以各信号时间为准"
                    : "等待授权或读取采集"}
              </small>
            </>
          }
          navigation={sections.map(([key, label, icon]) => ({
            key,
            label,
            icon,
            href: href(key),
            onSelect: () => go(key),
          }))}
          active={active}
          focusKey={currentView + ":" + id}
          actions={
            <>
              {demo ? (
                <Button kind="quiet" onClick={() => changeMode(false)}>
                  退出示例
                </Button>
              ) : session.context ? (
                <>
                  <Button
                    disabled={session.busy}
                    onClick={() => void session.refresh()}
                  >
                    {session.busy ? "正在读取…" : "刷新采集"}
                  </Button>
                  <Button kind="quiet" onClick={session.disconnect}>
                    断开
                  </Button>
                </>
              ) : (
                <Button onClick={() => setConnection(true)}>连接环境</Button>
              )}
              <span className="ops-environment-mark">
                {demo
                  ? "设计示例 · 未连接环境"
                  : data
                    ? environment + " · 已授权采集"
                    : environment + " · 未连接采集"}
              </span>
            </>
          }
          footer={
            <>
              M&amp;D 技术运维 /{" "}
              {demo
                ? "本页为设计示例，数据、时间和结果均为演示。"
                : data
                  ? "最近读取 " +
                    formatTime(data.observed_at) +
                    "；各信号分别判断完整性。"
                  : "未连接采集，不把空值显示为健康或成功。"}
            </>
          }
        >
          <div
            className="ops-page-body"
            key={`${demo ? "demo" : session.client.epoch}:${currentView}:${id}`}
          >
            {session.error && !demo ? (
              <OpsNotice title="读取或操作未完成" error>
                <p>{session.error}</p>
                {session.requestID ? (
                  <p>请求标识：{session.requestID}</p>
                ) : null}
                <Button
                  disabled={session.busy || !session.context}
                  onClick={() => void session.refresh()}
                >
                  重新读取
                </Button>
              </OpsNotice>
            ) : null}
            {session.busy && !data ? (
              <OpsNotice title="正在读取环境">
                正在验证权限与采集来源；不会用示例值填补结果。
              </OpsNotice>
            ) : null}
            {currentView === "overview" ? (
              <Overview
                onConnect={() => setConnection(true)}
                onPreview={() => changeMode(true)}
              />
            ) : currentView === "services" ? (
              <Services />
            ) : currentView === "workers" ? (
              <Workers />
            ) : currentView === "releases" ? (
              <Releases />
            ) : currentView === "migrations" ? (
              <Migrations />
            ) : currentView === "alerts" ? (
              <Alerts />
            ) : currentView === "audit" ? (
              <Audit />
            ) : currentView === "access" ? (
              <Access
                onConnect={() => setConnection(true)}
                onPreview={() => changeMode(true)}
              />
            ) : currentView === "release" ? (
              <ReleaseDetail />
            ) : currentView === "worker" ? (
              <WorkerDetail />
            ) : currentView === "incident" ? (
              <IncidentDetail />
            ) : currentView === "confirm" ? (
              <ConfirmRelease />
            ) : (
              <SystemState
                forbidden={currentView === "forbidden"}
                onConnect={() => setConnection(true)}
              />
            )}
          </div>
        </WorkspaceShell>
        {connection && !demo ? (
          <Modal
            title="连接技术运维环境"
            onClose={() => {
              setToken("");
              setConnection(false);
            }}
            busy={session.busy}
            actions={
              <>
                <Button
                  onClick={() => {
                    setToken("");
                    setConnection(false);
                  }}
                  disabled={session.busy}
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
                  读取本人权限
                </Button>
              </>
            }
          >
            <p>
              使用部署方分配的具名运维凭据。账号必须同时具有运维角色和当前环境授权。
            </p>
            <Field
              label="运维访问凭据"
              type="password"
              value={token}
              onChange={setToken}
              disabled={session.busy}
              autoComplete="off"
              hint="只在当前页面内存使用，断开或离开页面即清除；不放入网址或消费者登录。"
            />
          </Modal>
        ) : null}
      </>
    </Operations.Provider>
  );
}
