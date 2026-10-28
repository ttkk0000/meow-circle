import { encodeWire, parseWire } from "../core/wire";
import type { Row } from "./models";

export class OperatorError extends Error {
  constructor(
    public status: number,
    public uncertain = false,
  ) {
    super(
      status === 403
        ? "当前凭据没有此事项的处理角色，请重新核对本人权限。"
        : status === 401
          ? "凭据无效，请重新连接。"
          : status === 409
            ? "事项版本或状态已变化，请重新读取并核对。"
            : status === 400
              ? "请核对必填项、具体范围和时间格式。"
              : status === 404
                ? "该事项不存在或已不可访问。"
                : uncertain
                  ? "连接中断，处理结果尚未确认。"
                  : "服务暂时不可用，请稍后重新读取。",
    );
  }
}
/** 与消费者及技术运维会话独立；只在当前页面内存持有具名凭据。 */
export class OperatorClient {
  private token = "";
  private generation = 0;
  private requests = new Set<AbortController>();
  get epoch() {
    return this.generation;
  }
  cancel() {
    this.generation++;
    for (const controller of this.requests) controller.abort();
    this.requests.clear();
  }
  disconnect() {
    this.cancel();
    this.token = "";
  }
  connect(token: string) {
    this.disconnect();
    this.token = token;
  }
  async request<T>(path: string, body?: Row): Promise<T> {
    const write = body !== undefined;
    const allowed = write
      ? /^(?:age-evidence|authority|revenue|guardian-evidence|reviews\/[1-9][0-9]{0,18}|rights-cases\/[1-9][0-9]{0,18}\/decision|data-requests\/[1-9][0-9]{0,18}\/(?:hold|process))$/
      : /^(?:context|review-queue|revenue|guardian-invites(?:\/[1-9][0-9]{0,18})?|reviews\/[1-9][0-9]{0,18}|rights-cases(?:\/[1-9][0-9]{0,18})?|data-requests\/[1-9][0-9]{0,18})(?:\?cursor=\d{1,19})?$/;
    const lookup =
      !write &&
      path.startsWith("authority?") &&
      [...new URLSearchParams(path.slice(10)).keys()].every((k) =>
        ["actor_id", "pet_id", "scope", "city", "species"].includes(k),
      );
    if (!allowed.test(path) && !lookup) throw new Error("无效业务后台请求");
    if (this.token.length < 32 || this.token.length > 4096)
      throw new OperatorError(401);
    const epoch = this.generation,
      controller = new AbortController();
    this.requests.add(controller);
    const timeout = window.setTimeout(() => controller.abort(), 12_000);
    try {
      const response = await fetch(`/api/v2/ops/${path}`, {
        method: write ? "POST" : "GET",
        credentials: "omit",
        cache: "no-store",
        referrerPolicy: "no-referrer",
        signal: controller.signal,
        headers: {
          "X-Operator-Token": this.token,
          ...(write ? { "Content-Type": "application/json" } : {}),
        },
        body: write ? encodeWire(body) : undefined,
      });
      const raw = await response.text();
      if (epoch !== this.generation)
        throw new DOMException("会话已关闭", "AbortError");
      if (raw.length > 2 * 1024 * 1024) throw new OperatorError(0, write);
      let envelope: { ok: boolean; data: T };
      try {
        envelope = parseWire(raw);
      } catch {
        throw new OperatorError(
          response.status >= 400 ? response.status : 0,
          write &&
            (response.ok || response.status === 408 || response.status >= 500),
        );
      }
      if (!response.ok || !envelope.ok)
        throw new OperatorError(
          response.status,
          write &&
            (response.status < 400 ||
              response.status === 408 ||
              response.status >= 500),
        );
      return envelope.data;
    } catch (error) {
      if (epoch !== this.generation)
        throw new DOMException("会话已关闭", "AbortError");
      if (error instanceof OperatorError) throw error;
      throw new OperatorError(0, write);
    } finally {
      window.clearTimeout(timeout);
      this.requests.delete(controller);
    }
  }
}
