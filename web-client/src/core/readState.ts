import { HttpError } from "./api";

export type ReadFailure =
  | "connection"
  | "signin"
  | "forbidden"
  | "unavailable"
  | "busy"
  | "service";
export function readFailure(error: unknown): ReadFailure {
  if (error instanceof HttpError) {
    if (error.status === 401) return "signin";
    if (error.status === 403) return "forbidden";
    if (error.status === 404 || error.status === 410) return "unavailable";
    if (error.status === 429) return "busy";
    return "service";
  }
  // fetch的网络失败是TypeError；不将HTTP 503或解析失败误标为设备离线。
  return error instanceof TypeError ? "connection" : "service";
}
export const readStateCopy: Record<
  ReadFailure,
  { title: string; detail: string }
> = {
  connection: {
    title: "网络没跟上，内容先留着",
    detail:
      "已有草稿和阅读位置会保留；当前显示的内容来自上次成功读取，不代表最新状态。",
  },
  signin: {
    title: "登录后继续查看",
    detail:
      "登录状态已变化，受保护内容已收起；重新登录后再核对原任务，不会自动重发操作。",
  },
  forbidden: {
    title: "当前账号暂不能查看",
    detail:
      "访问范围或使用资格可能已变化，受保护内容已收起。你仍可返回或使用安全与隐私求助。",
  },
  unavailable: {
    title: "这条信息暂不可继续",
    detail:
      "可能已结束、撤回或受限；不把旧预约、旧地址和旧费用继续当有效信息。",
  },
  busy: {
    title: "稍等一下再试",
    detail: "操作较频繁。当前输入与原请求编号保留，请稍后重新读取。",
  },
  service: {
    title: "暂时无法读取",
    detail: "服务暂不可用。当前输入保留；重新读取不会重新提交原操作。",
  },
};
