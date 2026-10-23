import { useSyncExternalStore } from "react";

const listeners = new Set<() => void>();
function notify() {
  for (const listener of listeners) listener();
}
window.addEventListener("popstate", notify);
/** 仅更新当前列表的公开筛选，不重新挂载组件或把输入放入持久存储。 */
export function replaceFilters(values: Record<string, string>) {
  const url = new URL(location.href);
  for (const [name, value] of Object.entries(values)) {
    if (value) url.searchParams.set(name, value);
    else url.searchParams.delete(name);
  }
  history.replaceState(history.state, "", url.pathname + url.search);
}
export function navigate(path: string, replace = false) {
  if (
    !/^\/(?:[a-z][a-z0-9-]*\/?|[a-z][a-z0-9_-]*\/\d+(?:\/[a-z-]+)?\/?)?(?:\?[^#]*)?$/.test(
      path,
    ) ||
    path.includes("..")
  )
    throw new Error("无效页面");
  history[replace ? "replaceState" : "pushState"](
    { mnd: true },
    "",
    "/app" + (path === "/" ? "/" : path),
  );
  notify();
}
export function useRoute() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    () =>
      location.pathname.slice(4).replace(/\/$/, "") + location.search || "/",
    () => "/",
  );
}
export function back() {
  if (history.state?.mnd) history.back();
  else navigate("/");
}
export function resourceRoute(kind: string, id: unknown): string | null {
  return [
    "group-directory",
    "entries",
    "pets",
    "cases",
    "offers",
    "adoptions",
    "appointments",
    "conversations",
    "groups",
    "reviews",
    "claims",
    "users",
    "guardians",
    "friendships",
    "invitations",
  ].includes(kind) &&
    /^\d+$/.test(String(id)) &&
    BigInt(String(id)) > 0n
    ? `/${kind}/${id}`
    : null;
}
