import { useEffect, useSyncExternalStore } from "react";
import { api, explain, isAbort } from "./api";

interface QueryState<T> {
  data?: T;
  loading: boolean;
  error?: string;
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
  record.state = { ...record.state, loading: true, error: undefined };
  notify(record);
  try {
    const data = await api.request(path, { signal: abort.signal });
    if (!abort.signal.aborted) record.state = { data, loading: false };
  } catch (error) {
    if (!isAbort(error) && !abort.signal.aborted)
      record.state = { loading: false, error: explain(error) };
  } finally {
    if (!abort.signal.aborted) {
      record.abort = undefined;
      notify(record);
    }
  }
}
export function invalidateQueries() {
  for (const record of entries.values()) {
    record.abort?.abort();
    record.state = empty;
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
