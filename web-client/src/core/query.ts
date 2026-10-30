import { useEffect, useSyncExternalStore } from "react";
import { api, explain, isAbort, HttpError } from "./api";
import { readFailure, type ReadFailure } from "./readState";

interface QueryState<T> {
  data?: T;
  loading: boolean;
  error?: string;
  denied?: boolean;
  failure?: ReadFailure;
  readAt?: number;
}
interface Entry {
  state: QueryState<unknown>;
  listeners: Set<() => void>;
  abort?: AbortController;
}
const entries = new Map<string, Entry>();
const empty: QueryState<unknown> = { loading: false };
function entry(path: string): Entry {
  let value = entries.get(path);
  if (!value) {
    value = { state: empty, listeners: new Set() };
    entries.set(path, value);
  }
  return value;
}
function notify(value: Entry) {
  for (const callback of value.listeners) callback();
}
export async function reload(path: string) {
  const record = entry(path);
  record.abort?.abort();
  const abort = new AbortController();
  record.abort = abort;
  record.state = {
    ...record.state,
    loading: true,
    error: undefined,
    failure: undefined,
  };
  notify(record);
  try {
    const data = await api.request(path, { signal: abort.signal });
    if (!abort.signal.aborted)
      record.state = { data, loading: false, readAt: Date.now() };
  } catch (error) {
    if (!isAbort(error) && !abort.signal.aborted) {
      const denied =
        error instanceof HttpError && [401, 403, 404, 410].includes(error.status);
      record.state = {
        data: denied ? undefined : record.state.data,
        loading: false,
        error: explain(error),
        denied,
        failure: readFailure(error),
        readAt: denied ? undefined : record.state.readAt,
      };
    }
  } finally {
    if (!abort.signal.aborted) {
      record.abort = undefined;
      notify(record);
    }
  }
  return !abort.signal.aborted && !!record.state.data && !record.state.error;
}
export function invalidateQueries(prefix = "") {
  for (const [path,record] of entries) {
    if(!path.startsWith(prefix)) continue;
    record.abort?.abort();
    record.state = empty;
    notify(record);
  }
}
/** Replace a currently owned query after an authoritative write receipt. */
export function replaceQueryData<T>(path: string, data: T) {
  const record = entry(path);
  record.abort?.abort();
  record.abort = undefined;
  record.state = { data, loading: false, readAt: Date.now() };
  notify(record);
}
// 清除已关闭的列表授权范围；保留明确拒绝，避免自动重复请求已失权的页面。
export function discardQueryData(prefix: string) {
  for (const [path, record] of entries) {
    if (!path.startsWith(prefix) || record.state === empty) continue;
    if (record.state.denied && !record.state.loading) continue;
    record.abort?.abort();
    record.abort = undefined;
    record.state = record.state.denied
      ? { ...record.state, data: undefined, loading: false, readAt: undefined }
      : empty;
    notify(record);
  }
}
export function useQuery<T>(path: string | null): QueryState<T> {
  const cacheKey = path ?? "";
  const snapshot = useSyncExternalStore(
    (callback) => {
      if (!path) return () => {};
      const record = entry(cacheKey);
      record.listeners.add(callback);
      return () => {
        record.listeners.delete(callback);
      };
    },
    () => (path ? entry(cacheKey).state : empty),
    () => empty,
  ) as QueryState<T>;
  useEffect(() => {
    if (path && entry(cacheKey).state === empty) void reload(path);
  }, [path, cacheKey, api.epoch, snapshot === empty]);
  return snapshot;
}
