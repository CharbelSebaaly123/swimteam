import { createContext, useContext, useMemo, useState } from 'react';
import { api } from './api';

const STORAGE_KEY = 'swimteam.auth';

const AuthContext = createContext(null);

function loadStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadStoredAuth);

  const persist = (next) => {
    setAuth(next);
    if (next) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  };

  const value = useMemo(
    () => ({
      user: auth,
      token: auth?.token ?? null,
      isCoach: auth?.role === 'COACH',
      isMember: auth?.role === 'MEMBER',
      async login(username, password) {
        const data = await api.login({ username, password });
        persist(data);
        return data;
      },
      async signup(username, email, password) {
        const data = await api.signup({ username, email, password });
        persist(data);
        return data;
      },
      logout() {
        persist(null);
      },
    }),
    [auth],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
