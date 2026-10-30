import { useEffect, useState, type ReactNode } from "react";
import { type RecordData } from "../core/api";
import { back } from "../core/router";
import { Button, Notice, RowLink, TaskPage } from "../components/ui";

export type NotificationView = "preferences" | "frequency" | "summary" | "permission" | "permissionHelp" | "saved";
export function preferenceValues(value: unknown): Record<string, unknown> {
  const raw = value && typeof value === "object" && !Array.isArray(value) ? value : {};
  return { direct_messages: true, groups: true, applications: true, interactions: true,
    frequency: "instant", push_permission: false, ...raw };
}
export function validPreference(value: Record<string, unknown>) {
  return ["direct_messages", "groups", "applications", "interactions", "push_permission"].every(k => typeof value[k] === "boolean") &&
    ["instant", "digest", "off"].includes(String(value.frequency));
}
export function preferenceReceipt(result: unknown, expected: Record<string, unknown>, owner: bigint, version: bigint, previousId?: bigint): RecordData {
  const row = result as RecordData | undefined;
  if (!row || typeof row.id !== "bigint" || row.id <= 0n || row.owner_id !== owner ||
      row.state !== "active" || typeof row.version !== "bigint" || row.version <= version ||
      (previousId != null && row.id !== previousId) || !row.data || !validPreference(row.data) ||
      ["direct_messages", "groups", "applications", "interactions", "frequency", "push_permission"].some(k => row.data[k] !== expected[k]))
    throw Error("通知偏好回执与本次设置不一致，请保留原请求并核对结果。");
  return row;
}
const frequencyLabel = (value: unknown) => value === "digest" ? "每日汇总 · 19:00" : value === "instant" ? "即时通知" : value === "off" ? "关闭非紧急提醒" : "当前频率待核对";

export function NotificationPreferences({ value, view, result, disabled, busy, onChange, onView, onSave, children }: {
  value: Record<string, unknown>; view: NotificationView; result?: RecordData;
  disabled: boolean; busy: boolean; onChange: (change: Record<string, unknown>) => void;
  onView: (view: NotificationView) => void; onSave: (frequency?: string) => void; children?: ReactNode;
}) {
  const [permission, setPermission] = useState<string>("unknown");
  useEffect(() => {
    const read = () => setPermission("Notification" in window ? Notification.permission : "unsupported");
    read();
    window.addEventListener("focus", read);
    document.addEventListener("visibilitychange", read);
    return () => { window.removeEventListener("focus", read); document.removeEventListener("visibilitychange", read); };
  }, []);
  function returnView() {
    if (busy) return;
    if (view === "preferences") back();
    else if (view === "permissionHelp") onView("permission");
    else if (view === "saved" && result?.data.frequency === "digest") onView("summary");
    else onView("preferences");
  }
  const savedTitle = result?.data.frequency === "digest" ? "已改为每日汇总" : result?.data.frequency === "instant" ? "已使用即时通知" : result?.data.frequency === "off" ? "已关闭非紧急提醒" : "通知偏好已保存";
  const title = view === "frequency" ? "提醒频率" : view === "permission" ? "系统通知权限" : view === "permissionHelp" ? "开启系统通知" : view === "saved" ? savedTitle : "通知偏好";
  const switchRow = (key: string, label: string) => <PreferenceSwitch key={key} label={label} checked={value[key] === true} disabled={disabled} onChange={checked => onChange({ [key]: checked })} />;
  return <TaskPage key={view} title={title} scrollScope={`notification:${view}`} onBack={returnView}
    actions={view === "preferences" || view === "summary" ? <Button kind="primary" disabled={disabled} onClick={() => onSave()}>完成</Button>
      : view === "saved" ? <Button kind="primary" disabled={busy} onClick={returnView}>返回通知设置</Button>
      : view === "permission" ? <Button kind="primary" disabled={busy} onClick={() => onView("preferences")}>稍后再说</Button>
      : view === "permissionHelp" ? <Button kind="primary" disabled={busy} onClick={() => onView("permission")}>返回应用</Button> : undefined}>
    {view === "saved" && result ? <>
      <h2 className="page-headline">{savedTitle}</h2>
      <Notice title="当前状态">{!result.data.push_permission ? `${frequencyLabel(result.data.frequency)}的偏好已保存。设备推送未开启，你仍可查看站内消息。` : result.data.frequency === "digest" ? "非紧急动态在中国标准时间每天19:00进入汇总投递时段；你随时可以在应用内查看。"
        : result.data.frequency === "instant" ? "新的非紧急动态将按已开启的类别进入投递队列。" : "非紧急设备提醒已关闭；站内消息、未读和待处理事项仍然保留。"}</Notice>
      <p className="caption">偏好已保存。网页仍通过消息中心查看提醒；设备登记和实际送达分别由各设备通道核对。</p>
    </> : null}
    {view === "frequency" ? <>
      {[ ["instant", "即时通知", "按已开启的类别接收"], ["digest", "每日汇总", "中国标准时间每天19:00"], ["off", "关闭非紧急提醒", "保留站内消息"] ].map(([key,label,detail]) =>
        <RowLink key={key} title={label} detail={detail + (value.frequency === key ? " · 当前选择" : "")} disabled={disabled} onClick={() => onSave(key)} />)}
      <Notice title="重要进度">安全、账号与需及时确认的状态会在应用内保留。频率不改变未读或业务处理状态，附近提醒单独管理。</Notice>
    </> : null}
    {view === "preferences" || view === "summary" ? <>
      {view === "preferences" ? <>{switchRow("direct_messages", "私信消息")}{switchRow("groups", "宠友群消息")}{switchRow("applications", "申请与照护进度")}{switchRow("interactions", "社区评论与点赞")}</>
        : <RowLink title="管理通知类别" detail="私信、群聊、申请与互动" disabled={busy} onClick={() => onView("preferences")} />}
      <RowLink title="附近走失提醒" detail="独立管理范围与频率" to="/nearby" disabled={busy} />
      <RowLink title="提醒频率" detail={frequencyLabel(value.frequency)} disabled={busy} onClick={() => onView("frequency")} />
      <RowLink title="系统通知权限" detail="网页站内消息可用，设备权限分别管理" disabled={busy} onClick={() => onView("permission")} />
    </> : null}
    {view === "permission" || view === "permissionHelp" ? <>
      <Notice title="可以继续使用 M&amp;D">你仍能在消息页查看进度。账号偏好与每台设备的系统权限分别管理；允许浏览器通知不代表已接通推送。</Notice>
      <p>浏览器权限：{permission === "granted" ? "已允许" : permission === "denied" ? "未允许" : permission === "default" ? "尚未授权" : permission === "unsupported" ? "当前浏览器不提供通知权限" : "正在核对"}</p>
      {view === "permission" ? <>
        {switchRow("push_permission", "允许设备推送")}
        <p className="caption">此项管理账号设备推送偏好。返回通知偏好点击完成后保存；本网页仍可直接查看站内消息。</p>
        <RowLink title="了解开启方式" disabled={busy} onClick={() => onView("permissionHelp")} />
      </> : <>
        <p>手机 App 可在系统设置中找到 M&amp;D，进入通知设置并允许通知。网页可查看地址栏的网站权限；返回后会重新读取浏览器权限。</p>
        <RowLink title="查看站内消息" to="/messages" disabled={busy} />
      </>}
    </> : null}
    {children}
  </TaskPage>;
}

function PreferenceSwitch({ label, checked, disabled, onChange }: { label: string; checked: boolean; disabled: boolean; onChange: (checked: boolean) => void }) {
  return <button type="button" role="switch" aria-checked={checked} className="preference-switch" disabled={disabled} onClick={() => onChange(!checked)}>
    <span>{label}</span><span className="preference-switch__track" aria-hidden="true"><span /></span>
  </button>;
}
