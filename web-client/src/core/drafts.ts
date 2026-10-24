import { key } from "./wire";

export interface PhotoDraft {
  key: string;
  name: string;
  type: string;
  blob?: Blob;
  mediaId?: bigint;
  scope: "public" | "private" | "safety";
  error?: string;
}
export interface Draft {
  key: string;
  ownerKey: string;
  route: string;
  revision: number;
  values: Record<string, unknown>;
  ui?: { origin?: string; keepLocation?: boolean };
  baseVersion: bigint;
  photos: PhotoDraft[];
  requestId: string;
  uncertain: boolean;
  original?: {
    path: string;
    method: string;
    body: string;
    replayable: boolean;
  };
  access?: {
    type: "safety" | "data";
    token: string;
    requestId: string;
    id?: bigint;
  };
}
let opening: Promise<IDBDatabase> | undefined;
function database() {
  opening ??= new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open("mnd-web-v2-private", 1);
    request.onupgradeneeded = () => {
      const store = request.result.createObjectStore("drafts", {
        keyPath: "key",
      });
      store.createIndex("ownerKey", "ownerKey");
    };
    request.onsuccess = () => {
      request.result.onversionchange = () => {
        request.result.close();
        opening = undefined;
      };
      resolve(request.result);
    };
    request.onerror = () => {
      opening = undefined;
      reject(new Error("本机私有草稿不可用，请检查浏览器存储权限"));
    };
    request.onblocked = () =>
      reject(new Error("请关闭旧版本页面后重试读取本机草稿"));
  });
  return opening;
}
export const ownerKey = (owner: bigint) => location.origin + "|" + owner;
export const draftKey = (owner: bigint, route: string) =>
  ownerKey(owner) + "|" + route;
export function newDraft(
  owner: bigint,
  route: string,
  values: Record<string, unknown> = {},
  baseVersion = 0n,
): Draft {
  return {
    key: draftKey(owner, route),
    ownerKey: ownerKey(owner),
    route,
    revision: 0,
    values,
    baseVersion,
    photos: [],
    requestId: key(),
    uncertain: false,
  };
}
export async function readDraft(
  owner: bigint,
  route: string,
): Promise<Draft | undefined> {
  const db = await database();
  return new Promise((resolve, reject) => {
    const request = db
      .transaction("drafts")
      .objectStore("drafts")
      .get(draftKey(owner, route));
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
export async function listDrafts(
  owner: bigint,
): Promise<Array<{ route: string; uncertain: boolean; requestId: string }>> {
  const db = await database();
  return new Promise((resolve, reject) => {
    const rows: Array<{
      route: string;
      uncertain: boolean;
      requestId: string;
    }> = [];
    const request = db
      .transaction("drafts")
      .objectStore("drafts")
      .index("ownerKey")
      .openCursor(IDBKeyRange.only(ownerKey(owner)));
    request.onsuccess = () => {
      const cursor = request.result;
      if (!cursor) {
        resolve(rows);
        return;
      }
      const d = cursor.value as Draft;
      if (!d.route.startsWith("capability:"))
        rows.push({
          route: d.route,
          uncertain: d.uncertain,
          requestId: d.requestId,
        });
      cursor.continue();
    };
    request.onerror = () => reject(request.error);
  });
}
export async function saveDraft(draft: Draft): Promise<Draft> {
  // 账号凭据和一次用途授权只能驻留内存；受限事项查询能力单独存入access。
  const forbidden = new Set([
    "password",
    "code",
    "reauth_token",
    "query_token",
    "safety_token",
    "access_token",
    "refresh_token",
    "token",
  ]);
  const inspect = (value: unknown): void => {
    if (Array.isArray(value)) value.forEach(inspect);
    else if (value && typeof value === "object")
      for (const [key, item] of Object.entries(value)) {
        if (forbidden.has(key)) throw new Error("草稿不能保存账号凭据或验证码");
        inspect(item);
      }
  };
  inspect(draft.values);
  const db = await database();
  return new Promise((resolve, reject) => {
    const tx = db.transaction("drafts", "readwrite");
    const store = tx.objectStore("drafts");
    const get = store.get(draft.key);
    let next: Draft;
    get.onsuccess = () => {
      const current = get.result as Draft | undefined;
      if ((current?.revision ?? 0) !== draft.revision) {
        tx.abort();
        return;
      }
      let count = 0,
        bytes = draft.photos.reduce(
          (sum, photo) => sum + (photo.blob?.size ?? 0),
          0,
        );
      const scan = store
        .index("ownerKey")
        .openCursor(IDBKeyRange.only(draft.ownerKey));
      scan.onsuccess = () => {
        const cursor = scan.result;
        if (cursor) {
          const row = cursor.value as Draft;
          if (row.key !== draft.key) {
            if (
              row.uncertain ||
              row.access ||
              row.photos.length ||
              Object.keys(row.values).length
            )
              count++;
            bytes += row.photos.reduce(
              (sum, photo) => sum + (photo.blob?.size ?? 0),
              0,
            );
          }
          cursor.continue();
          return;
        }
        const active =
          draft.uncertain ||
          draft.access ||
          draft.photos.length ||
          Object.keys(draft.values).length;
        if ((active && count >= 40) || bytes > 200 * 1024 * 1024) {
          tx.abort();
          return;
        }
        next = { ...draft, revision: draft.revision + 1 };
        store.put(next);
      };
    };
    tx.oncomplete = () => resolve(next);
    tx.onabort = () =>
      reject(
        new Error(
          "本机草稿版本冲突或容量不足，当前输入保留。请核对已保存版本，或清理已完成的草稿；原结果未知的操作应保留",
        ),
      );
    tx.onerror = () =>
      reject(tx.error ?? new Error("本机草稿保存失败，当前输入保留"));
  });
}
/** 递增空快照而不是删除记录，旧标签页的旧revision无法复活私密材料。 */
export async function clearDraft(draft: Draft): Promise<Draft> {
  return saveDraft({
    ...draft,
    values: {},
    ui: undefined,
    photos: [],
    uncertain: false,
    original: undefined,
    access: undefined,
    requestId: key(),
  });
}
export async function listCapabilities(): Promise<Draft[]> {
  const db = await database();
  return new Promise((resolve, reject) => {
    const rows: Draft[] = [];
    const request = db
      .transaction("drafts")
      .objectStore("drafts")
      .index("ownerKey")
      .openCursor(IDBKeyRange.only(ownerKey(0n)));
    request.onsuccess = () => {
      const cursor = request.result;
      if (!cursor) {
        resolve(rows);
        return;
      }
      const value = cursor.value as Draft;
      if (value.route.startsWith("capability:") && value.access)
        rows.push(value);
      cursor.continue();
    };
    request.onerror = () => reject(request.error);
  });
}
export async function clearOwner(owner: bigint) {
  const db = await database();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction("drafts", "readwrite");
    const store = tx.objectStore("drafts");
    const request = store
      .index("ownerKey")
      .openCursor(IDBKeyRange.only(ownerKey(owner)));
    request.onsuccess = () => {
      const cursor = request.result;
      if (!cursor) return;
      const old = cursor.value as Draft;
      if (!old.route.startsWith("capability:"))
        cursor.update({
          ...old,
          revision: old.revision + 1,
          values: {},
          photos: [],
          uncertain: old.uncertain,
          original: undefined,
          access: undefined,
        });
      cursor.continue();
    };
    tx.oncomplete = () => resolve();
    tx.onabort = () => reject(tx.error);
    tx.onerror = () => reject(tx.error);
  });
}
