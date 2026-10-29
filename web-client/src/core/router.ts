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
    {
      mnd: true,
      parents: replace
        ? (history.state?.parents ?? [])
        : [
            ...(history.state?.parents ?? []),
            location.pathname.slice(4).replace(/\/$/, "") + location.search ||
              "/",
          ].slice(-64),
    },
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
  if (history.state?.mnd && history.state.parents?.length) history.back();
  else navigate("/");
}
/** 只保存应用公开路由；回到已有目录，保留此前任务的筛选与滚动恢复点。 */
export function returnTo(path: string) {
  const parents: string[] =
    history.state?.mnd && Array.isArray(history.state.parents)
      ? history.state.parents
      : [];
  const index = parents.lastIndexOf(path);
  if (index >= 0) history.go(index - parents.length);
  else navigate(path, true);
}
export function resourceRoute(kind: string, id: unknown): string | null {
  return [
    "group-directory",
    "entries",
    "entry_comments",
    "pet_comments",
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
    "leads",
  ].includes(kind) &&
    /^\d+$/.test(String(id)) &&
    BigInt(String(id)) > 0n
    ? `/${kind}/${id}`
    : null;
}
