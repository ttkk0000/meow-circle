import { useEffect, useMemo, useState } from "react";
import { api, query, type Page, type RecordData } from "./api";
import { discardQueryData, reload, useQuery } from "./query";

// 仅会话内保留列表翻页；账号epoch是缓存键的一部分，不落盘私密列表。
const pageMemory = new Map<string, { cursor: bigint; rows: RecordData[] }>();
let memoryEpoch = -1;

/** 分页状态属于当前任务；网络读取仍统一走查询缓存，切换账号／筛选不会串页。 */
export function usePages(
  kind: string,
  filters: Record<string, unknown> = {},
  enabled = true,
) {
  const base = kind + "?" + query(filters);
  const epoch = api.epoch;
  const identity = base + "|" + epoch;
  if (memoryEpoch !== epoch) {
    pageMemory.clear();
    memoryEpoch = epoch;
  }
  const [page, setPage] = useState({
    identity,
    cursor: pageMemory.get(identity)?.cursor ?? 0n,
  });
  const cursor = page.identity === identity ? page.cursor : 0n;
  const prefix = base + (base.endsWith("?") ? "" : "&");
  const path = prefix + "cursor=" + cursor;
  const response = useQuery<Page | RecordData[]>(enabled ? path : null);
  const data = useMemo(
    () =>
      Array.isArray(response.data)
        ? { items: response.data, next_cursor: 0n }
        : response.data,
    [response.data],
  );
  const result = { ...response, data };
  const [collected, setCollected] = useState<{
    identity: string;
    rows: RecordData[];
  }>({ identity, rows: pageMemory.get(identity)?.rows ?? [] });
  useEffect(() => {
    if (!enabled || result.denied) {
      pageMemory.delete(identity);
      setCollected({ identity, rows: [] });
      if (!enabled) setPage({ identity, cursor: 0n });
      discardQueryData(prefix);
      return;
    }
    if (!result.data) return;
    setCollected((old) => {
      const map = new Map(
        (old.identity === identity && cursor > 0n ? old.rows : []).map(
          (row) => [row.id, row],
        ),
      );
      for (const row of result.data!.items) map.set(row.id, row);
      const rows = [...map.values()];
      pageMemory.set(identity, { cursor, rows });
      if (pageMemory.size > 40)
        pageMemory.delete(pageMemory.keys().next().value!);
      return { identity, rows };
    });
  }, [enabled, result.data, result.denied, identity, cursor, prefix]);
  return {
    ...result,
    rows:
      enabled && !result.denied && collected.identity === identity ? collected.rows : [],
    retry: () => { if (enabled) void reload(path); },
    more: () => {
      if (enabled && result.data?.next_cursor && !result.loading)
        setPage({ identity, cursor: result.data.next_cursor });
    },
    refresh: () => {
      if (!enabled) return;
      setPage({ identity, cursor: 0n });
      void reload(prefix + "cursor=0");
    },
    next: result.data?.next_cursor ?? 0n,
  };
}
export const titleOf = (row: RecordData) =>
  String(
    row.data.title ||
      row.data.name ||
      row.data.pet_name ||
      row.data.text ||
      row.data.reason ||
      row.data.note ||
      `记录 #${row.id}`,
  );
