import { useSyncExternalStore } from "react";
import { api } from "./api";
import { invalidateQueries } from "./query";
const listeners = new Set<() => void>();
export const signInEvent = "mnd-request-signin";
export function requestSignIn() {
  window.dispatchEvent(new Event(signInEvent));
}
let owner: bigint | undefined;
api.onSession = () => {
  const current = api.session.user?.id;
  if (owner !== current) {
    owner = current;
    invalidateQueries();
  }
  for (const callback of listeners) callback();
};
export function useSession() {
  return useSyncExternalStore(
    (callback) => {
      listeners.add(callback);
      return () => {
        listeners.delete(callback);
      };
    },
    () => api.session,
    () => api.session,
  );
}
