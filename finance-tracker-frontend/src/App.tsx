import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Layout } from './components/Layout/Layout';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Transactions } from './pages/Transactions';
import { Analytics } from './pages/Analytics';
import { ManagerPanel } from './pages/ManagerPanel';  // <-- ВАЖНО: импорт!
import { useAuth } from './context/AuthContext';
import { UserRole } from './types';
import { HelmetProvider } from 'react-helmet-async'; // <--- ДОБАВИТЬ ЭТУ СТРОКУ
import { HelmetSEO } from './components/SEO/HelmetSEO';


interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: UserRole[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, requiredRoles }) => {
  const { user, isLoading, hasAnyRole } = useAuth();
  
  if (isLoading) {
    return <div className="flex items-center justify-center h-screen">Загрузка...</div>;
  }
  
  if (!user) {
    return <Navigate to="/login" />;
  }

  if (requiredRoles && !hasAnyRole(requiredRoles)) {
    return <Navigate to="/dashboard" />;
  }
  
  return <>{children}</>;
};

function App() {
  return (
    <HelmetProvider>
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/login" element={
              <>
                <HelmetSEO 
                  title="Вход" 
                  description="Войдите в свой аккаунт Финансового Трекера"
                  noIndex={true}
                />
                <Login />
              </>
            } />
            <Route path="/register" element={
              <>
                <HelmetSEO 
                  title="Регистрация" 
                  description="Создайте новый аккаунт Финансового Трекера"
                  noIndex={true}
                />
                <Register />
              </>
            } />
            
            <Route path="/" element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }>
              <Route index element={<Navigate to="/dashboard" />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="transactions" element={<Transactions />} />
              <Route path="analytics" element={<Analytics />} />
              <Route path="manager" element={
                <ProtectedRoute requiredRoles={['ROLE_MANAGER', 'ROLE_ADMIN']}>
                  <ManagerPanel />
                </ProtectedRoute>
              } />
            </Route>
          </Routes>
        </Router>
      </AuthProvider>
    </HelmetProvider>
  );
}

export default App;