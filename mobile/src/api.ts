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
    const err: ApiError =
      data && typeof data === 'object'
        ? (data as ApiError)
        : { code: res.status, message: text || 'request failed' };
    if (res.status === 401) {
      // Token expired or revoked — clear and let the caller redirect.
      await tokenStore.clear();
      await userStore.clear();
    }
    throw new HttpError(res.status, err);
  }

  return data as T;
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
    list: () => request<Post[]>('/api/v1/posts', { auth: false }),
    create: (body: Partial<Post> & { title: string; content: string }) =>
      request<Post>('/api/v1/posts', { method: 'POST', body }),
    search: (q: string) =>
      request<Post[]>(`/api/v1/search/posts?q=${encodeURIComponent(q)}`, { auth: false }),
  },

  listings: {
    list: () => request<Listing[]>('/api/v1/listings', { auth: false }),
    create: (body: Partial<Listing> & { title: string; price_cents: number }) =>
      request<Listing>('/api/v1/listings', { method: 'POST', body }),
  },

  messages: {
    conversations: () => request<Conversation[]>('/api/v1/messages/conversations'),
    send: (body: { recipient_id: number; content: string }) =>
      request<unknown>('/api/v1/messages', { method: 'POST', body }),
  },

  notifications: {
    unreadCount: () =>
      request<{ count: number }>('/api/v1/notifications/unread-count').catch(() => ({ count: 0 })),
  },
};

export type { Options };
