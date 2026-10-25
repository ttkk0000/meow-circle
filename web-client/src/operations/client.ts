import type { Context, Snapshot } from "./models";

export class OpsError extends Error {
  constructor(
    public status: number,
    public code: string,
    public requestID = "",
  ) {
    const messages: Record<string, string> = {
      authentication_required: "凭据无效或已撤销，请重新连接。",
      environment_forbidden: "当前账号没有此环境的访问权限。",
      role_forbidden: "当前账号没有此动作的权限。",
      environment_unconfigured: "服务端尚未配置技术运维环境。",
      origin_forbidden: "页面来源与部署配置不一致。",
      invalid_input: "请核对输入格式与必填项。",
      version_conflict: "记录已变化或状态不允许，请刷新后重新核对。",
      not_found: "该记录不存在或已不可访问。",
      service_unavailable: "运维存储尚未就绪，请核对迁移与配置。",
      storage_unavailable: "运维数据暂时不可用，请保留请求号后重试。",
    };
    super(messages[code] ?? "请求未完成，请稍后重新读取。");
  }
}
/** 凭据只在当前页面内存中存活；切换模式、断开与离开页面会取消请求并清空。 */
export class OpsClient {
  private token = "";
  private controllers = new Set<AbortController>();
  private generation = 0;
  get epoch() {
    return this.generation;
  }
  disconnect() {
    this.generation++;
    this.token = "";
    for (const request of this.controllers) request.abort();
    this.controllers.clear();
  }
  connect(token: string) {
    this.disconnect();
    this.token = token;
  }
  async request<T>(path: string, body?: Record<string, unknown>): Promise<T> {
    if (
      !/^(?:context|snapshot|(?:runs|audit|incidents|releases)(?:\/[a-zA-Z0-9_-]{1,80}(?:\/(?:actions|preflight))?)?)(?:\?cursor=\d+)?$/.test(
        path,
      )
    )
      throw new Error("无效运维请求");
    if (this.token.length < 32)
      throw new OpsError(401, "authentication_required");
    const epoch = this.generation,
      controller = new AbortController();
    this.controllers.add(controller);
    let timedOut = false;
    const timeout = window.setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, 12_000);
    try {
      const response = await fetch(`/api/ops/v1/${path}`, {
        method: body ? "POST" : "GET",
        credentials: "omit",
        cache: "no-store",
        referrerPolicy: "no-referrer",
        signal: controller.signal,
        headers: {
          "X-Operator-Token": this.token,
          ...(body ? { "Content-Type": "application/json" } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
      });
      const raw = await response.text();
      if (epoch !== this.generation)
        throw new DOMException("会话已关闭", "AbortError");
      if (raw.length > 2 * 1024 * 1024)
        throw new Error("响应过大，请分页读取。");
      const envelope = JSON.parse(raw) as {
        ok: boolean;
        data: T;
        error_code?: string;
        request_id?: string;
      };
      if (!response.ok || !envelope.ok)
        throw new OpsError(
          response.status,
          envelope.error_code ?? "request_failed",
          envelope.request_id,
        );
      return envelope.data;
    } catch (error) {
      if (timedOut && epoch === this.generation)
        throw new OpsError(0, "request_timeout");
      throw error;
    } finally {
      window.clearTimeout(timeout);
      this.controllers.delete(controller);
    }
  }
  context() {
    return this.request<Context>("context");
  }
  snapshot() {
    return this.request<Snapshot>("snapshot");
  }
}
export function isCancelled(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}
export function message(error: unknown) {
  return error instanceof Error ? error.message : "操作未完成。";
}
