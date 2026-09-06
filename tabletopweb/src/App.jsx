import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import ProtectedRoute from './components/ProtectedRoute'
import ShellLayout from './components/ShellLayout'
import Dashboard from './pages/Dashboard'
import LobbyPage from './pages/LobbyPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import SessionPage from './pages/SessionPage'
import VerifyPage from './pages/VerifyPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify" element={<VerifyPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<ShellLayout />}>
              <Route path="/" element={<Dashboard />} />
              <Route path="/sessions" element={<LobbyPage />} />
              <Route path="/sessions/:id" element={<SessionPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}