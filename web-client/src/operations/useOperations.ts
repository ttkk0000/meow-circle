import { useCallback, useEffect, useRef, useState } from "react";
import { OpsClient, OpsError, isCancelled, message } from "./client";
import type { Context, Document, Page, Resource, Snapshot } from "./models";

export function useOperations(demo: boolean) {
  const [client] = useState(() => new OpsClient());
  const [context, setContext] = useState<Context>();
  const contextRef = useRef<Context>();
  const [snapshot, setSnapshot] = useState<Snapshot>();
  const [error, setError] = useState("");
  const [requestID, setRequestID] = useState("");
  const [forbidden, setForbidden] = useState(false);
  const [busy, setBusy] = useState(false);
  const sequence = useRef(0);
  const disconnect = useCallback(() => {
    sequence.current++;
    client.disconnect();
    setContext(undefined);
    contextRef.current = undefined;
    setSnapshot(undefined);
    setError("");
    setRequestID("");
    setForbidden(false);
    setBusy(false);
  }, [client]);
  useEffect(() => {
    disconnect();
    return () => {
      sequence.current++;
      client.disconnect();
    };
  }, [demo, disconnect, client]);
  const report = useCallback(
    (failure: unknown) => {
      setError(message(failure));
      setRequestID(failure instanceof OpsError ? failure.requestID : "");
      if (
        failure instanceof OpsError &&
        (failure.status === 401 || failure.status === 403)
      ) {
        client.disconnect();
        setContext(undefined);
        contextRef.current = undefined;
        setSnapshot(undefined);
        setForbidden(failure.status === 403);
      }
    },
    [client],
  );
  const refresh = useCallback(async () => {
    if (demo) return;
    const seq = ++sequence.current;
    let epoch = client.epoch;
    setBusy(true);
    setError("");
    try {
      const identity = await client.context();
      if (seq !== sequence.current || epoch !== client.epoch) return;
      const before = contextRef.current;
      if (
        before &&
        JSON.stringify([
          before.id,
          before.environment,
          before.roles,
          before.capabilities,
        ]) !==
          JSON.stringify([
            identity.id,
            identity.environment,
            identity.roles,
            identity.capabilities,
          ])
      ) {
        client.cancel();
        epoch = client.epoch;
        setSnapshot(undefined);
      }
      contextRef.current = identity;
      setContext(identity);
      const next = await client.snapshot();
      if (seq === sequence.current && epoch === client.epoch) setSnapshot(next);
    } catch (e) {
      if (seq === sequence.current && epoch === client.epoch && !isCancelled(e))
        report(e);
    } finally {
      if (seq === sequence.current) setBusy(false);
    }
  }, [client, demo, report]);
  async function connect(token: string) {
    if (demo) return;
    disconnect();
    client.connect(token);
    const seq = ++sequence.current,
      epoch = client.epoch;
    setBusy(true);
    try {
      const next = await client.context();
      if (seq !== sequence.current || epoch !== client.epoch) return;
      setContext(next);
      contextRef.current = next;
      await refresh();
    } catch (e) {
      if (
        seq === sequence.current &&
        epoch === client.epoch &&
        !isCancelled(e)
      ) {
        client.disconnect();
        report(e);
      }
    } finally {
      if (seq === sequence.current) setBusy(false);
    }
  }
  async function more(kind: Resource) {
    if (demo || !snapshot || !snapshot[kind].next_cursor) return;
    const seq = ++sequence.current,
      epoch = client.epoch;
    setBusy(true);
    setError("");
    try {
      const next = await client.request<Page<unknown>>(
        `${kind}?cursor=${snapshot[kind].next_cursor}`,
      );
      if (seq !== sequence.current || epoch !== client.epoch) return;
      setSnapshot((previous) => {
        if (!previous) return previous;
        const ids = new Set(previous[kind].items.map((item) => item.id));
        return {
          ...previous,
          [kind]: {
            items: [
              ...previous[kind].items,
              ...next.items.filter((item) => !ids.has(item.id)),
            ],
            next_cursor: next.next_cursor,
          },
        };
      });
    } catch (e) {
      if (seq === sequence.current && epoch === client.epoch && !isCancelled(e))
        report(e);
    } finally {
      if (seq === sequence.current) setBusy(false);
    }
  }
  async function readDocument<T>(kind: Resource, id: string) {
    if (demo) throw new Error("示例模式不读取环境");
    return client.request<Document<T>>(`${kind}/${id}`);
  }
  return {
    client,
    context,
    snapshot,
    busy,
    error,
    requestID,
    forbidden,
    connect,
    disconnect,
    refresh,
    more,
    readDocument,
    report,
  };
}
export type OperationsSession = ReturnType<typeof useOperations>;
