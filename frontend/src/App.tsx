import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './features/identity/auth/AuthContext'
import { RequireAuth } from './features/identity/auth/RequireAuth'
import { DemoPage } from './features/demo/DemoPage'
import { LoginPage } from './features/identity/login/LoginPage'
import { TicketsPage } from './features/tickets/TicketsPage'

const AuthenticatedRoutes = () => (
  <AuthProvider>
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <TicketsPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  </AuthProvider>
)

export const App = () => (
  <BrowserRouter>
    <Routes>
      <Route path="/demo" element={<DemoPage />} />
      <Route path="/*" element={<AuthenticatedRoutes />} />
    </Routes>
  </BrowserRouter>
)
