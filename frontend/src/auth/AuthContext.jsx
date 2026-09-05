import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authenticate, getMe } from '../api/endpoints';
import { setUnauthorizedHandler, tokenStorage } from '../api/client';

// The front-end half of the authentication/authorization system.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // Starts true so the app can wait for the token check before deciding whether
  // to redirect; otherwise a refresh would bounce a signed-in user to /login.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  // Lets the axios interceptor clear React state when the server rejects a token
  // mid-session, not just when the user presses "log out".
  useEffect(() => {
    setUnauthorizedHandler(() => setUser(null));
  }, []);

  // A token in localStorage survives a page reload, but it may have expired
  // while the tab was closed. The only way to know is to ask the server.
  useEffect(() => {
    const token = tokenStorage.get();
    if (!token) {
      setLoading(false);
      return;
    }

    getMe()
      .then((response) => setUser(response.data))
      .catch(() => tokenStorage.clear())
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (username, password) => {
    setError(null);
    try {
      const { data } = await authenticate(username, password);
      tokenStorage.set(data.token);
      setUser(data.user);
      return data.user;
    } catch (err) {
      setError(err.apiError?.message ?? 'Login failed');
      throw err;
    }
  }, []);

  const value = useMemo(() => {
    const capabilities = new Set(user?.capabilities ?? []);
    return {
      user,
      loading,
      error,
      login,
      logout,
      setUser,
      isAuthenticated: Boolean(user),
      isAdmin: user?.role === 'ADMIN',
      can: (capability) => capabilities.has(capability),
    };
  }, [user, loading, error, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return context;
}
