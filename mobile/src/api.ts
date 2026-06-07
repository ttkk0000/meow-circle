// Minimal, dependency-free fetch client for the M&D Go backend.
// Matches the URL contract defined in internal/platform/api/router.go.
//
// Token lifecycle: we persist it in expo-secure-store (Keychain on iOS,
// EncryptedSharedPreferences on Android) and attach it as Authorization:
// Bearer on every request after login/register.

import Constants from 'expo-constants';
import * as SecureStore from 'expo-secure-store';
import {Platform} from 'react-native';

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

  // Android emulator (including most AVD-based setups) reaches host machine
  // through 10.0.2.2. Prefer this before hostUri to avoid LAN-IP mismatches.
  if (Platform.OS === 'android' && !Constants.isDevice) {
    return 'http://10.0.2.2:8080';
  }

  const host = Constants.expoConfig?.hostUri?.split(':')[0];
  if (host) return `http://${host}:8080`;

  if (Platform.OS === 'android') return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

export const BASE_URL = resolveBaseUrl();

/** Media `url` from the API may be a path; prefix with API origin when needed. */
export function resolveMediaUrl(url?: string | null): string | undefined {
  if (url == null) return undefined;
  const u = String(url).trim();
  if (!u) return undefined;
  if (u.startsWith('http://') || u.startsWith('https://')) return u;
  return `${BASE_URL}${u.startsWith('/') ? u : `/${u}`}`;
}

// ===== token storage =========================================================

// SecureStore isn't available on web; transparently fall back to the
// platform's storage. We keep the API identical so the rest of the app
// doesn't need to care.
function getWebStorage():
  | {
      getItem: (key: string) => string | null;
      setItem: (key: string, value: string) => void;
      removeItem: (key: string) => void;
    }
  | null {
  const maybeGlobal = globalThis as {
    localStorage?: {
      getItem: (key: string) => string | null;
      setItem: (key: string, value: string) => void;
      removeItem: (key: string) => void;
    };
  };
  return maybeGlobal.localStorage ?? null;
}

const secureGet = async (k: string): Promise<string | null> => {
  if (Platform.OS === 'web') {
    try {
      return getWebStorage()?.getItem(k) ?? null;
    } catch {
      return null;
    }
  }
  return SecureStore.getItemAsync(k);
};
const secureSet = async (k: string, v: string): Promise<void> => {
  if (Platform.OS === 'web') {
    try {
      getWebStorage()?.setItem(k, v);
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
      getWebStorage()?.removeItem(k);
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

export interface MediaItem {
  id: number;
  owner_id: number;
  kind: string;
  mime: string;
  size: number;
  filename: string;
  url: string;
  status: string;
  created_at: string;
}

export interface MediaUploadInput {
  uri: string;
  name?: string | null;
  type?: string | null;
}

export interface Comment {
  id: number;
  post_id: number;
  author_id: number;
  content: string;
  created_at: string;
}

/** GET /api/v1/posts enriched card (Stitch feed). */
export interface PostFeedItem {
  post: Post;
  author: User;
  like_count: number;
  liked: boolean;
  first_media?: MediaItem | null;
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

export interface ListingDetailData {
  listing: Listing;
  media: MediaItem[];
}

export type OrderStatus =
  | 'pending_payment'
  | 'paid'
  | 'cancelled'
  | 'shipped'
  | 'refunded'
  | 'completed'
  | string;

export interface Order {
  id: number;
  buyer_id: number;
  seller_id: number;
  listing_id: number;
  listing_title?: string;
  amount_cents: number;
  currency: string;
  status: OrderStatus;
  payment_method?: string | null;
  payment_tx_id?: string | null;
  note?: string | null;
  created_at: string;
  updated_at: string;
  paid_at?: string | null;
  shipped_at?: string | null;
  completed_at?: string | null;
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

export interface ActionCounts {
  unread_notifications: number;
  unread_messages: number;
  buyer_order_actions: number;
  seller_order_actions: number;
  order_actions: number;
  total: number;
  generated_at: string;
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

async function uploadRequest<T>(path: string, file: MediaUploadInput): Promise<T> {
  const h: Record<string, string> = {
    Accept: 'application/json',
  };
  const token = await tokenStore.get();
  if (token) h.Authorization = `Bearer ${token}`;

  const name = file.name?.trim() || inferFilename(file.uri);
  const type = file.type?.trim() || inferMimeFromFilename(name);
  const form = new FormData();
  form.append(
    'file',
    {
      uri: file.uri,
      name,
      type,
    } as unknown as Blob,
  );

  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: h,
    body: form,
  });
  const text = await res.text();
  const data = text ? safeJson(text) : null;

  if (!res.ok) {
    const raw = data && typeof data === 'object' ? (data as Record<string, unknown>) : null;
    const err: ApiError = raw
      ? {
          code: typeof raw.code === 'number' ? raw.code : res.status,
          message: typeof raw.message === 'string' ? raw.message : text || 'upload failed',
        }
      : { code: res.status, message: text || 'upload failed' };
    if (res.status === 401) {
      await tokenStore.clear();
      await userStore.clear();
    }
    throw new HttpError(res.status, err);
  }

  return unwrapEnvelope(data) as T;
}

function inferFilename(uri: string): string {
  const clean = uri.split('?')[0] ?? uri;
  const last = clean.split('/').filter(Boolean).pop();
  return last && last.includes('.') ? last : `mnd-media-${Date.now()}.jpg`;
}

function inferMimeFromFilename(name: string): string {
  const lower = name.toLowerCase();
  if (lower.endsWith('.png')) return 'image/png';
  if (lower.endsWith('.webp')) return 'image/webp';
  if (lower.endsWith('.gif')) return 'image/gif';
  if (lower.endsWith('.mp4')) return 'video/mp4';
  if (lower.endsWith('.webm')) return 'video/webm';
  if (lower.endsWith('.mov')) return 'video/quicktime';
  return 'image/jpeg';
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
    register: (body: {
      username: string;
      password: string;
      nickname?: string;
      phone?: string;
      sms_code?: string;
    }) =>
      request<{ token: string; user: User }>('/api/v1/auth/register', {
        method: 'POST',
        body,
        auth: false,
      }),
    sendVerificationCode: (body: { phone: string }) =>
      request<{ ok: boolean }>('/api/v1/auth/send-verification-code', {
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
    list: (opts?: { filter?: 'rec' | 'new' | 'follow' }) => {
      const filter = opts?.filter ?? 'rec';
      return request<{
        items: PostFeedItem[];
        total: number;
        page: number;
        page_size: number;
        filter?: string;
      }>(`/api/v1/posts?page=1&page_size=40&filter=${encodeURIComponent(filter)}`).then((p) => p.items);
    },
    get: (id: number) =>
      request<{
        post: Post;
        media: MediaItem[];
        comments: Comment[];
        like_count: number;
        liked: boolean;
        author: User;
        following_author?: boolean;
      }>(`/api/v1/posts/${id}`),
    my: () => request<{ items: Post[] }>('/api/v1/me/posts').then((p) => p.items),
    create: (body: Partial<Post> & { title: string; content: string }) =>
      request<Post>('/api/v1/posts', { method: 'POST', body }),
    addComment: (postId: number, content: string) =>
      request<Comment>(`/api/v1/posts/${postId}/comments`, { method: 'POST', body: { content } }),
    toggleLike: (postId: number) =>
      request<{ liked: boolean; like_count: number }>(`/api/v1/posts/${postId}/like`, {
        method: 'POST',
      }),
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
    get: (id: number) => request<ListingDetailData>(`/api/v1/listings/${id}`, { auth: false }),
    create: (body: Partial<Listing> & { title: string; price_cents: number }) =>
      request<Listing>('/api/v1/listings', { method: 'POST', body }),
  },

  orders: {
    list: (role: 'buyer' | 'seller') =>
      request<{ items: Order[]; role?: string }>(
        `/api/v1/me/orders?role=${encodeURIComponent(role)}`,
      ).then((p) => p.items),
    get: (id: number) => request<Order>(`/api/v1/orders/${id}`),
    create: (listingId: number, note = '') =>
      request<Order>('/api/v1/orders', {
        method: 'POST',
        body: { listing_id: listingId, note },
      }),
    pay: (orderId: number, method = 'mock') =>
      request<Order>(`/api/v1/orders/${orderId}/pay`, {
        method: 'POST',
        body: { method },
      }),
    cancel: (orderId: number) =>
      request<Order>(`/api/v1/orders/${orderId}/cancel`, { method: 'POST' }),
    ship: (orderId: number) =>
      request<Order>(`/api/v1/orders/${orderId}/ship`, { method: 'POST' }),
    complete: (orderId: number) =>
      request<Order>(`/api/v1/orders/${orderId}/complete`, { method: 'POST' }),
    refund: (orderId: number) =>
      request<Order>(`/api/v1/orders/${orderId}/refund`, { method: 'POST' }),
  },

  messages: {
    conversations: () =>
      request<{ items: Conversation[] }>('/api/v1/me/conversations').then((d) => d.items),
    withPeer: (peerId: number) =>
      request<{ peer: User; messages: Message[] }>(`/api/v1/me/conversations/${peerId}`),
    send: (body: { recipient_id: number; content: string }) =>
      request<Message>('/api/v1/messages', { method: 'POST', body }),
  },

  media: {
    upload: (file: MediaUploadInput) => uploadRequest<MediaItem>('/api/v1/media', file),
  },

  notifications: {
    unreadCount: () =>
      request<{ items: unknown[]; unread_count: number }>('/api/v1/notifications?unread=true')
        .then((d) => ({ count: d.unread_count }))
        .catch(() => ({ count: 0 })),
  },

  actionCounts: {
    get: () => request<ActionCounts>('/api/v1/me/action-counts'),
  },

  /** POST follow / DELETE unfollow (auth). */
  followUser: (userId: number) =>
    request<{ following: boolean; user_id: number }>(`/api/v1/me/follow/${userId}`, {
      method: 'POST',
    }),
  unfollowUser: (userId: number) =>
    request<{ following: boolean; user_id: number }>(`/api/v1/me/follow/${userId}`, {
      method: 'DELETE',
    }),
};

export type { Options };
