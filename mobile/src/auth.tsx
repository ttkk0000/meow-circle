// Lightweight auth context. Nothing fancy — a single source of truth for
// the currently-logged-in user and the actions that mutate it. Heavier
// state-management (Zustand, Redux, TanStack Query, …) can slot in later.

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, HttpError, tokenStore, userStore, type User } from './api';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, nickname?: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const cached = await userStore.get();
        if (cached) setUser(cached);

        const token = await tokenStore.get();
        if (!token) return;

        // Verify the token is still valid; clears it on 401.
        const fresh = await api.auth.me().catch((e) => {
          if (e instanceof HttpError && e.status === 401) return null;
          throw e;
        });
        if (fresh) {
          setUser(fresh);
          await userStore.set(fresh);
        } else if (cached) {
          setUser(null);
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const { token, user } = await api.auth.login({ username, password });
    await tokenStore.set(token);
    await userStore.set(user);
    setUser(user);
  }, []);

  const register = useCallback(
    async (username: string, password: string, nickname?: string) => {
      const { token, user } = await api.auth.register({ username, password, nickname });
      await tokenStore.set(token);
      await userStore.set(user);
      setUser(user);
    },
    [],
  );

  const logout = useCallback(async () => {
    await tokenStore.clear();
    await userStore.clear();
    setUser(null);
  }, []);

  const value = useMemo<AuthState>(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
