import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, setAuthToken, setUnauthorizedHandler } from '../lib/api'
import { AuthContext } from './authContext'
import { clearSession, loadStoredSession, saveSession } from './authStore'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => loadStoredSession()?.user ?? null)
  const [loading, setLoading] = useState(() => !!loadStoredSession())

  const handleUnauthorized = useCallback(() => {
    clearSession()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(handleUnauthorized)
    return () => setUnauthorizedHandler(null)
  }, [handleUnauthorized])

  useEffect(() => {
    const stored = loadStoredSession()
    if (!stored) return
    setAuthToken(stored.token)
    api('/api/users/me')
      .then((me) => setUser(me))
      .catch(() => {
        // invalid/expired token -> handleUnauthorized clears the session
      })
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async ({ identifier, password }) => {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: { identifier, password },
    })
    saveSession(data.token, data.user)
    setAuthToken(data.token)
    setUser(data.user)
    return data
  }, [])

  const register = useCallback(async (payload) => {
    return api('/api/auth/register', { method: 'POST', body: payload })
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setAuthToken(null)
    setUser(null)
  }, [])

  const refreshMe = useCallback(async () => {
    const me = await api('/api/users/me')
    setUser(me)
    return me
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: !!user,
      login,
      register,
      logout,
      refreshMe,
    }),
    [user, loading, login, register, logout, refreshMe],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}