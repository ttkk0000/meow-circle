// Minimal, dependency-free fetch client for the Meow Circle Go backend.
// Matches the URL contract defined in internal/platform/api/router.go.
//
// Token lifecycle: we persist it in expo-secure-store (Keychain on iOS,
// EncryptedSharedPreferences on Android) and attach it as Authorization:
// Bearer on every request after login/register.

import Constants from 'expo-constants';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const TOKEN_KEY = 'meow.auth.token';
const USER_KEY = 'meow.auth.user';

// Resolve the API base URL:
//   1. EXPO_PUBLIC_API_URL (recommended, see mobile/.env.example)
//   2. When running in Expo Go on a phone, fall back to the dev-machine
//      LAN IP that Expo already knows (constants.expoConfig.hostUri).
//   3. Sensible default per-platform for bare simulators.
export function resolveBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_URL?.trim();
  if (fromEnv) return fromEnv.replace(/\/$/, '');

  const host = Constants.expoConfig?.hostUri?.split(':')[0];
  if (host) return `http://${host}:8080`;

  if (Platform.OS === 'android') return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

export const BASE_URL = resolveBaseUrl();

// ===== token storage =========================================================

// SecureStore isn't available on web; transparently fall back to the
// platform's storage. We keep the API identical so the rest of the app
// doesn't need to care.
const secureGet = async (k: string): Promise<string | null> => {
  if (Platform.OS === 'web') {
    try {
      return typeof window !== 'undefined' ? window.localStorage.getItem(k) : null;
    } catch {
      return null;
    }
  }
  return SecureStore.getItemAsync(k);
};
const secureSet = async (k: string, v: string): Promise<void> => {
  if (Platform.OS === 'web') {
    try {
      window.localStorage.setItem(k, v);
    } catch {
      /* ignore */
    }
    return;
  }
  await SecureStore.setItemAsync(k, v);
};
const secureDel = async (k: string): Promise<void> => {
  if (Platform.OS === 'web') {
    try {
      window.localStorage.removeItem(k);
    } catch {
      /* ignore */
    }
    return;
  }
  await SecureStore.deleteItemAsync(k);
};

export const tokenStore = {
  get: () => secureGet(TOKEN_KEY),
  set: (t: string) => secureSet(TOKEN_KEY, t),
  clear: () => secureDel(TOKEN_KEY),
};

export const userStore = {
  get: async (): Promise<User | null> => {
    const raw = await secureGet(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  },
  set: (u: User) => secureSet(USER_KEY, JSON.stringify(u)),
  clear: () => secureDel(USER_KEY),
};

// ===== domain types (mirrors internal/domain/models.go) ======================

export interface User {
  id: number;
  username: string;
  nickname: string;
  avatar_url?: string;
  bio?: string;
  created_at: string;
}

export interface Post {
  id: number;
  author_id: number;
  title: string;
  content: string;
  category: 'daily_share' | 'help' | string;
  tags: string[];
  media_ids?: number[];
  created_at: string;
  last_reply_at?: string;
}

export interface Listing {
  id: number;
  seller_id: number;
  type: 'product' | 'service' | 'adopt' | string;
  title: string;
  description: string;
  price_cents: number;
  currency: string;
  media_ids?: number[];
  created_at: string;
}

export interface Conversation {
  peer: User;
  last_message: string;
  last_sender_id: number;
  unread_count: number;
  updated_at: string;
}

export interface Message {
  id: number;
  sender_id: number;
  recipient_id: number;
  content: string;
  read: boolean;
  created_at: string;
}

export interface ApiError {
  code: number;
  message: string;
  retry_after?: number;
}

// ===== request helper ========================================================

type Options = Omit<RequestInit, 'body'> & { body?: unknown; auth?: boolean };

export class HttpError extends Error {
  constructor(public status: number, public payload: ApiError) {
    super(payload.message || `HTTP ${status}`);
  }
}

/** Go handlers use `writeOK` / `writeCreated`: `{ code, message, data }`. */
function unwrapEnvelope(parsed: unknown): unknown {
  if (parsed === null || typeof parsed !== 'object') return parsed;
  const o = parsed as Record<string, unknown>;
  if (typeof o.code === 'number' && o.code === 0 && 'data' in o) {
    return o.data;
  }
  return parsed;
}

async function request<T>(path: string, opts: Options = {}): Promise<T> {
  const { auth = true, body, headers, ...rest } = opts;
  const h: Record<string, string> = {
    Accept: 'application/json',
    ...((headers as Record<string, string>) ?? {}),
  };

  if (body !== undefined) h['Content-Type'] = 'application/json';

  if (auth) {
    const token = await tokenStore.get();
    if (token) h.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    headers: h,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const data = text ? safeJson(text) : null;

  if (!res.ok) {
    const raw = data && typeof data === 'object' ? (data as Record<string, unknown>) : null;
    const err: ApiError = raw
      ? {
          code: typeof raw.code === 'number' ? raw.code : res.status,
          message: typeof raw.message === 'string' ? raw.message : text || 'request failed',
          retry_after: typeof raw.retry_after === 'number' ? raw.retry_after : undefined,
        }
      : { code: res.status, message: text || 'request failed' };
    if (res.status === 401) {
      // Token expired or revoked — clear and let the caller redirect.
      await tokenStore.clear();
      await userStore.clear();
    }
    throw new HttpError(res.status, err);
  }

  return unwrapEnvelope(data) as T;
}

function safeJson(s: string): unknown {
  try {
    return JSON.parse(s);
  } catch {
    return null;
  }
}

// ===== endpoints =============================================================

export const api = {
  health: () => request<{ status: string; store: string }>('/healthz', { auth: false }),

  auth: {
    register: (body: { username: string; password: string; nickname?: string }) =>
      request<{ token: string; user: User }>('/api/v1/auth/register', {
        method: 'POST',
        body,
        auth: false,
      }),
    login: (body: { username: string; password: string }) =>
      request<{ token: string; user: User }>('/api/v1/auth/login', {
        method: 'POST',
        body,
        auth: false,
      }),
    me: () => request<User>('/api/v1/auth/me'),
  },

  posts: {
    list: () =>
      request<{ items: Post[]; total: number; page: number; page_size: number }>(
        '/api/v1/posts',
        { auth: false },
      ).then((p) => p.items),
    create: (body: Partial<Post> & { title: string; content: string }) =>
      request<Post>('/api/v1/posts', { method: 'POST', body }),
    search: (q: string) =>
      request<{ posts: Post[]; listings: Listing[]; query: string; type: string }>(
        `/api/v1/search?q=${encodeURIComponent(q)}&type=post`,
        { auth: false },
      ).then((r) => r.posts),
  },

  listings: {
    list: () =>
      request<{ items: Listing[]; total: number; page: number; page_size: number }>(
        '/api/v1/listings',
        { auth: false },
      ).then((p) => p.items),
    create: (body: Partial<Listing> & { title: string; price_cents: number }) =>
      request<Listing>('/api/v1/listings', { method: 'POST', body }),
  },

  messages: {
    conversations: () =>
      request<{ items: Conversation[] }>('/api/v1/me/conversations').then((d) => d.items),
    withPeer: (peerId: number) =>
      request<{ peer: User; messages: Message[] }>(`/api/v1/me/conversations/${peerId}`),
    send: (body: { recipient_id: number; content: string }) =>
      request<Message>('/api/v1/messages', { method: 'POST', body }),
  },

  notifications: {
    unreadCount: () =>
      request<{ items: unknown[]; unread_count: number }>('/api/v1/notifications?unread=true')
        .then((d) => ({ count: d.unread_count }))
        .catch(() => ({ count: 0 })),
  },
};

export type { Options };
