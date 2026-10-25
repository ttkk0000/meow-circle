import { encodeWire, parseWire } from "./wire";
import { routeAllowed } from "./routes";

export interface User {
  id: bigint;
  username: string;
  nickname: string;
  bio?: string;
  avatar_url?: string;
}
export interface RecordData<T = Record<string, unknown>> {
  id: bigint;
  owner_id: bigint;
  version: bigint;
  reference_id: bigint;
  state: string;
  data: T;
  media_ids: bigint[];
  created_at: string;
  updated_at: string;
}
export interface Page<T = Record<string, unknown>> {
  items: RecordData<T>[];
  next_cursor: bigint;
}
export interface Inbox {
  global_total?: bigint;
  direct: Record<string, bigint>;
  groups: Record<string, bigint>;
  notifications: Record<string, bigint>;
  pending: Record<string, bigint>;
  message_requests: bigint;
  direct_first_unread: Record<string, bigint>;
  group_first_unread: Record<string, bigint>;
}
interface Envelope<T> {
  ok: boolean;
  data: T;
  error?: string;
  error_code?: string;
}
export interface Session {
  authenticated: boolean;
  refreshable: boolean;
  user: User | null;
  csrf: string;
}
export class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    detail?: string,
  ) {
    super(
      messages[code] ??
        (
          {
            401: "请登录后继续",
            403: "当前权限或资格不足",
            404: "内容不存在、已撤回或当前不可见",
            409: "记录已变化，请保留输入并核对最新版本",
            429: "操作过于频繁，请稍后再试",
            503: "当前服务尚未配置或暂不可用",
          } as Record<number, string>
        )[status] ??
        detail ??
        "请求失败，请重试",
    );
  }
}
const messages: Record<string, string> = {
  identity_in_use:
    "此登录标识已有绑定，请登录或找回原账号；不会覆盖资料、迁移聊天或透露其他账号信息",
  phone_verification_required: "请先验证当前绑定的手机号",
  adult_required: "此操作需要有效的成年人资格",
  guardian_consent_required: "请先核对有效的监护范围",
  contact_restricted:
    "此联系入口受保护设置限制，未发送消息；可使用好友确认、屏蔽或私密求助入口",
  purpose_grant_required: "请重新验证此次操作用途",
  idempotency_conflict: "原编号与原内容不一致，请核对原操作",
  version_conflict: "版本已变化，当前输入保留",
  publication_declaration_required: "请核对本次内容来源声明",
};
const publicPath = (path: string) =>
  /^reading(?:\/[a-z][a-z0-9-]{0,63}(?:\/download)?)?(?:\?|$)/.test(path) ||
  /^(group-directory|entries|pets|offers|cases|entry_comments|pet_comments)(?:\/\d+)?$/.test(
    path.split("?")[0],
  ) ||
  /^(auth\/capabilities|media\/\d+\/content|share\/(?:entries|pets|offers|cases)\/\d+|users\/\d+|entry-metrics|comment-metrics|expression-catalog|expression-assets\/v1\/[^/]+|(?:pets|offers)\/\d+\/verifications)(?:\?|$)/.test(
    path,
  );
function normalizeRecords(value: unknown): void {
  if (Array.isArray(value)) {
    value.forEach(normalizeRecords);
    return;
  }
  if (!value || typeof value !== "object") return;
  const object = value as Record<string, unknown>;
  for (const field of [
    "members",
    "muted",
    "favorites",
    "recent",
    "followed_users",
    "watched_cases",
    "user_ids",
  ])
    if (object[field] === null) object[field] = [];
  if (
    typeof object.id === "bigint" &&
    typeof object.version === "bigint" &&
    typeof object.state === "string" &&
    object.data
  ) {
    if (object.media_ids == null) object.media_ids = [];
    if (object.reference_id == null) object.reference_id = 0n;
  }
  for (const item of Object.values(object)) normalizeRecords(item);
}

class BrowserAPI {
  session: Session = {
    authenticated: false,
    refreshable: false,
    user: null,
    csrf: "",
  };
  epoch = 0;
  private opening?: Promise<Session>;
  private rotating?: Promise<void>;
  private channel =
    typeof BroadcastChannel !== "undefined"
      ? new BroadcastChannel("mnd-web-session")
      : null;
  onSession: (() => void) | null = null;
  constructor() {
    this.channel?.addEventListener("message", (event) => {
      if (event.data === "changed")
        void this.openSession().catch(() => this.invalidate());
    });
  }
  private accept(next: Session) {
    if (!next.authenticated && next.refreshable && this.session.user)
      next = { ...next, user: this.session.user };
    if (this.session.user?.id !== next.user?.id) this.epoch++;
    this.session = next;
    this.onSession?.();
  }
  invalidate() {
    this.accept({
      authenticated: false,
      refreshable: false,
      user: null,
      csrf: "",
    });
  }
  endSession() {
    this.invalidate();
    this.channel?.postMessage("changed");
  }
  async openSession(): Promise<Session> {
    if (this.opening) return this.opening;
    const run = async () => {
      const response = await fetch("/api/web/v2/session", {
        credentials: "same-origin",
        cache: "no-store",
      });
      const envelope = parseWire<Envelope<Session>>(await response.text());
      if (!response.ok || !envelope.ok)
        throw new HttpError(
          response.status,
          envelope.error_code ?? "",
          envelope.error,
        );
      this.accept(envelope.data);
      return envelope.data;
    };
    this.opening = run().finally(() => {
      this.opening = undefined;
    });
    return this.opening;
  }
  async refresh(): Promise<void> {
    if (this.rotating) return this.rotating;
    const run = async () => {
      const session = await this.openSession();
      if (session.authenticated) return;
      if (!session.refreshable)
        throw new HttpError(401, "authentication_required");
      const response = await fetch("/api/web/v2/auth/refresh", {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        headers: {
          "X-MND-CSRF": session.csrf,
          "Content-Type": "application/json",
        },
        body: "{}",
      });
      if (!response.ok) {
        this.invalidate();
        throw new HttpError(response.status, "authentication_required");
      }
      await this.openSession();
    };
    const serialized = () =>
      navigator.locks ? navigator.locks.request("mnd-web-refresh", run) : run();
    this.rotating = serialized().finally(() => {
      this.rotating = undefined;
    });
    return this.rotating;
  }
  async request<T>(
    path: string,
    options: {
      method?: string;
      body?: unknown;
      headers?: Record<string, string>;
      signal?: AbortSignal;
      blob?: boolean;
      owner?: bigint;
    } = {},
    retry = true,
  ): Promise<T> {
    if (
      !/^[a-z][a-z0-9_./-]*(?:\?[^#]*)?$/.test(path) ||
      path.includes("..") ||
      path.startsWith("ops")
    )
      throw new Error("无效的资源入口");
    const method = options.method ?? "GET";
    if (!routeAllowed(method, path))
      throw new Error("此操作尚未出现在当前服务合同中");
    const isPublic =
      method === "GET" && !this.session.authenticated && publicPath(path);
    if (!isPublic && !this.session.csrf) await this.openSession();
    if (
      options.owner !== undefined &&
      options.owner !== (this.session.user?.id ?? 0n)
    )
      throw new DOMException("账号已变化", "AbortError");
    const epoch = this.epoch;
    const headers = new Headers(options.headers);
    if (method !== "GET" && method !== "HEAD")
      headers.set("X-MND-CSRF", this.session.csrf);
    const multipart = options.body instanceof FormData;
    if (options.body !== undefined && !multipart)
      headers.set("Content-Type", "application/json");
    const response = await fetch(
      (isPublic ? "/api/v2/" : "/api/web/v2/") + path,
      {
        method,
        headers,
        credentials: "same-origin",
        cache: "no-store",
        signal: options.signal,
        body:
          options.body === undefined
            ? undefined
            : multipart
              ? (options.body as FormData)
              : encodeWire(options.body),
      },
    );
    if (epoch !== this.epoch)
      throw new DOMException("账号已变化", "AbortError");
    if (
      response.status === 401 &&
      retry &&
      this.session.refreshable &&
      (!path.startsWith("auth/") || path.startsWith("auth/age-assurance"))
    ) {
      await this.refresh();
      if (epoch !== this.epoch)
        throw new DOMException("账号已变化", "AbortError");
      return this.request<T>(path, options, false);
    }
    if (options.blob && response.ok) return (await response.blob()) as T;
    const envelope = parseWire<Envelope<T>>(await response.text());
    if (!response.ok || !envelope.ok)
      throw new HttpError(
        response.status,
        envelope.error_code ?? "",
        envelope.error,
      );
    if (
      /^auth\/(password|email|register|phone|oauth-finish|oauth-resolve|logout|account\/[^/]+)$/.test(
        path,
      ) &&
      !path.endsWith("/scope")
    ) {
      await this.openSession();
      this.channel?.postMessage("changed");
    }
    normalizeRecords(envelope.data);
    return envelope.data;
  }
}
export const api = new BrowserAPI();
export const query = (values: Record<string, unknown>) =>
  new URLSearchParams(
    Object.entries(values)
      .filter(([, v]) => v !== undefined && v !== "")
      .map(([k, v]) => [k, String(v)]),
  ).toString();
export function isAbort(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}
export function explain(error: unknown) {
  return error instanceof Error ? error.message : "暂时无法完成操作，请重试";
}
