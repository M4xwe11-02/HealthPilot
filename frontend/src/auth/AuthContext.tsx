import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, CurrentUser, LoginPayload, RegisterPayload } from '../api/auth';
import { AUTH_TOKEN_EVENT, clearAuthToken, getAuthToken, setAuthToken } from '../api/request';

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
}

const AUTH_USER_KEY = 'health-guardian.auth.user';
const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(AUTH_USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    localStorage.removeItem(AUTH_USER_KEY);
    return null;
  }
}

function storeUser(user: CurrentUser | null): void {
  if (user) {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(AUTH_USER_KEY);
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(() => readStoredUser());
  const [loading, setLoading] = useState(() => Boolean(getAuthToken()));

  useEffect(() => {
    let cancelled = false;

    async function refreshCurrentUser() {
      const token = getAuthToken();
      if (!token) {
        setUser(null);
        storeUser(null);
        setLoading(false);
        return;
      }

      try {
        const currentUser = await authApi.me();
        if (!cancelled) {
          setUser(currentUser);
          storeUser(currentUser);
        }
      } catch {
        if (!cancelled) {
          setUser(null);
          storeUser(null);
          clearAuthToken();
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    refreshCurrentUser();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onTokenChange = () => {
      if (!getAuthToken()) {
        setUser(null);
        storeUser(null);
      }
    };

    window.addEventListener(AUTH_TOKEN_EVENT, onTokenChange);
    return () => window.removeEventListener(AUTH_TOKEN_EVENT, onTokenChange);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    isAuthenticated: Boolean(user && getAuthToken()),
    async login(payload) {
      const response = await authApi.login(payload);
      setAuthToken(response.token);
      setUser(response.user);
      storeUser(response.user);
    },
    async register(payload) {
      const response = await authApi.register(payload);
      setAuthToken(response.token);
      setUser(response.user);
      storeUser(response.user);
    },
    async logout() {
      try {
        await authApi.logout();
      } finally {
        clearAuthToken();
        setUser(null);
        storeUser(null);
      }
    },
  }), [loading, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
