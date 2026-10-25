import { createContext, useContext, type ReactNode } from "react";
import { navigate } from "../core/router";
import type { OperationsSession } from "./useOperations";
import type { Snapshot, View } from "./models";

export interface OperationsContext {
  session: OperationsSession;
  demo: boolean;
  data?: Snapshot;
  view: View;
  id: string;
  go: (view: View, id?: string) => void;
  href: (view: View, id?: string) => string;
}
export const Operations = createContext<OperationsContext | null>(null);
export function useOps() {
  const value = useContext(Operations);
  if (!value) throw new Error("缺少运维页面上下文");
  return value;
}
export function pagePath(view: View, id: string | undefined, demo: boolean) {
  const p = new URLSearchParams({ view });
  if (id) p.set("id", id);
  if (demo) p.set("mode", "demo");
  return `/ops?${p}`;
}
export function OpsLink({
  view,
  id,
  children,
  button = false,
}: {
  view: View;
  id?: string;
  children: ReactNode;
  button?: boolean;
}) {
  const ops = useOps();
  return (
    <a
      className={button ? "button secondary" : "ops-link"}
      href={ops.href(view, id)}
      onClick={(event) => {
        if (
          !event.metaKey &&
          !event.ctrlKey &&
          !event.shiftKey &&
          !event.altKey &&
          event.button === 0
        ) {
          event.preventDefault();
          navigate(pagePath(view, id, ops.demo));
        }
      }}
    >
      {children}
    </a>
  );
}
