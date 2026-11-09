import {resourceRoute} from "./router";
import {integer} from "./wire";

export interface ReplyAnchor { kind: "entry_comments" | "pet_comments"; id: bigint; version: bigint }
const positive = (value: unknown) => {
  const id = integer(value);
  return id > 0n && id <= 9223372036854775807n ? id : 0n;
};

export function notificationRoute(data: Record<string, unknown>): string | null {
  const kind = String(data.target_kind), parent = positive(data.target_id);
  if (!parent) return null;
  const route = resourceRoute(kind, parent);
  if (!route) return null;
  const anchor = data.reply_anchor;
  if (!anchor || typeof anchor !== "object") return route;
  const value = anchor as Record<string, unknown>;
  const expected = kind === "entries" ? "entry_comments" : kind === "pets" ? "pet_comments" : null;
  const id = positive(value.id), version = positive(value.version);
  return expected && value.kind === expected && id && version
    ? `${route}?reply=${id}&reply_version=${version}` : route;
}

export function notificationReplyAnchor(kind: string, params: URLSearchParams): ReplyAnchor | null {
  if (!["entries", "pets"].includes(kind)) return null;
  const raw = params.get("reply") ?? "", revision = params.get("reply_version") ?? "";
  if (!/^[1-9][0-9]{0,18}$/.test(raw) || !/^[1-9][0-9]{0,18}$/.test(revision)) return null;
  const id = positive(raw), version = positive(revision);
  return id && version ? { kind: kind === "pets" ? "pet_comments" : "entry_comments", id, version } : null;
}
