/** int64 在进入 Number 前解析为 bigint；相同编码器用于请求与持久化原请求。 */
export type Wire =
  | null
  | boolean
  | string
  | number
  | bigint
  | Wire[]
  | { [key: string]: Wire | undefined };
export function encodeWire(value: unknown): string {
  if (value === null) return "null";
  if (typeof value === "bigint") return value.toString();
  if (typeof value === "number") {
    if (!Number.isFinite(value)) throw new Error("数值必须有限");
    return JSON.stringify(value);
  }
  if (typeof value === "string" || typeof value === "boolean")
    return JSON.stringify(value);
  if (Array.isArray(value))
    return (
      "[" +
      value.map((v) => encodeWire(v === undefined ? null : v)).join(",") +
      "]"
    );
  if (typeof value === "object")
    return (
      "{" +
      Object.entries(value)
        .filter(([, v]) => v !== undefined)
        .map(([k, v]) => JSON.stringify(k) + ":" + encodeWire(v))
        .join(",") +
      "}"
    );
  throw new Error("不能编码该数据类型");
}
export function parseWire<T = unknown>(source: string): T {
  if (source.length > 16 * 1024 * 1024) throw new Error("响应过大，请分页读取");
  let cursor = 0;
  function whitespace() {
    while (/\s/.test(source[cursor] ?? "") && cursor < source.length) cursor++;
  }
  function value(depth: number): unknown {
    if (depth > 80) throw new Error("响应嵌套过深");
    whitespace();
    const start = cursor;
    const char = source[cursor++];
    if (char === '"') {
      let escaped = false;
      while (cursor < source.length) {
        const c = source[cursor++];
        if (c === '"' && !escaped)
          return JSON.parse(source.slice(start, cursor));
        if (c === "\\" && !escaped) escaped = true;
        else escaped = false;
      }
      throw new Error("字符串未结束");
    }
    if (char === "{" || char === "[") {
      const object = char === "{";
      const out: Record<string, unknown> | unknown[] = object
        ? Object.create(null)
        : [];
      whitespace();
      if (source[cursor] === (object ? "}" : "]")) {
        cursor++;
        return out;
      }
      while (cursor < source.length) {
        if (object) {
          whitespace();
          if (source[cursor] !== '"') throw new Error("对象键无效");
          const key = value(depth + 1);
          if (Object.hasOwn(out, String(key)))
            throw new Error("响应含重复对象键");
          whitespace();
          if (source[cursor++] !== ":") throw new Error("对象格式无效");
          (out as Record<string, unknown>)[String(key)] = value(depth + 1);
        } else (out as unknown[]).push(value(depth + 1));
        whitespace();
        const next = source[cursor++];
        if (next === (object ? "}" : "]")) return out;
        if (next !== ",") throw new Error("JSON分隔无效");
      }
      throw new Error("JSON容器未结束");
    }
    cursor = start;
    const token =
      /^(true|false|null|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)/.exec(
        source.slice(cursor),
      )?.[0];
    if (!token) throw new Error("JSON值无效");
    cursor += token.length;
    if (token === "true") return true;
    if (token === "false") return false;
    if (token === "null") return null;
    if (!/[.eE]/.test(token)) return BigInt(token);
    const number = Number(token);
    if (!Number.isFinite(number)) throw new Error("响应数值越界");
    return number;
  }
  const result = value(0);
  whitespace();
  if (cursor !== source.length) throw new Error("响应含多余数据");
  return result as T;
}
export const key = () => crypto.randomUUID().replaceAll("-", "");
export function integer(value: unknown, fallback = 0n): bigint {
  try {
    return typeof value === "bigint"
      ? value
      : /^-?\d+$/.test(String(value))
        ? BigInt(String(value))
        : fallback;
  } catch {
    return fallback;
  }
}
export function id(value: string): bigint {
  const result = integer(value);
  if (result <= 0n || result > 9223372036854775807n)
    throw new Error("请输入有效编号");
  return result;
}
export function money(value: unknown): string {
  const cents = integer(value);
  const sign = cents < 0n ? "-" : "";
  const positive = cents < 0n ? -cents : cents;
  return (
    sign +
    positive / 100n +
    "." +
    (positive % 100n).toString().padStart(2, "0") +
    " 元"
  );
}
export function cents(value: string): bigint {
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(value.trim()))
    throw new Error("金额最多两位小数");
  const [a, b = ""] = value.trim().split(".");
  return BigInt(a) * 100n + BigInt(b.padEnd(2, "0"));
}
export function badge(value: unknown): string {
  const count = integer(value);
  return count <= 0n ? "" : count > 99n ? "99+" : count.toString();
}
