import type { ProviderCallback } from "./provider";

export interface DesktopHandoff {
  provider: "wechat" | "qq";
  state: string;
  callback: string;
}

/** 本机随机端口和一次性口令仅在fragment传入，读取后立即从历史移除。 */
export function takeDesktopHandoff(): DesktopHandoff | undefined {
  const params = new URLSearchParams(location.hash.slice(1));
  history.replaceState(null, "", "/app/desktop-provider");
  if (location.protocol !== "https:") return;
  const provider = params.get("provider");
  const state = params.get("state") ?? "";
  if ((provider !== "wechat" && provider !== "qq") || !/^[A-Za-z0-9_-]{32,200}$/.test(state)) return;
  if (["provider", "state", "callback"].some(key => params.getAll(key).length !== 1) || [...params.keys()].length !== 3) return;
  try {
    const url = new URL(params.get("callback") ?? "");
    const port = Number(url.port);
    if (url.protocol !== "http:" || url.hostname !== "127.0.0.1" || port < 1024 || port > 65535 ||
      url.username || url.password || url.search || url.hash || !/^\/mnd\/provider\/[A-Za-z0-9_-]{43}$/.test(url.pathname)) return;
    return { provider, state, callback: url.href };
  } catch { return; }
}

/** 使用顶层表单返回，避免跨源fetch权限；本机端仍检查Host、Origin、路径口令与state。 */
export function returnDesktopResult(handoff: DesktopHandoff, result: ProviderCallback): void {
  if (result.state !== handoff.state) throw new Error("授权结果与本次桌面任务不一致");
  const form = document.createElement("form");
  form.method = "POST";
  form.action = handoff.callback;
  form.target = "_self";
  form.hidden = true;
  for (const [name, value] of Object.entries({ state: result.state, code: result.code, cancelled: String(result.cancelled) })) {
    const input = document.createElement("input");
    input.type = "hidden"; input.name = name; input.value = value;
    form.append(input);
  }
  document.body.append(form);
  form.submit();
  form.remove();
}
