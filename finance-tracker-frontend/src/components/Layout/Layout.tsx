import React from 'react';
import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../UI/Button';

export const Layout: React.FC = () => {
  const { user, logout, isAdmin, isManager } = useAuth();
  const navigate = useNavigate();

  // Добавьте диагностику
  console.log('Layout render:', { 
    user: user?.username, 
    isAdmin, 
    isManager,
    roles: user?.roles 
  });

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-blue-600">Финансовый Трекер</h1>
              <div className="ml-10 flex items-baseline space-x-4">
                <Link
                  to="/dashboard"
                  className="text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium"
                >
                  Дашборд
                </Link>
                <Link
                  to="/transactions"
                  className="text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium"
                >
                  Транзакции
                </Link>
                <Link
                  to="/analytics"
                  className="text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium"
                >
                  Аналитика
                </Link>
                
                {/* Кнопка для менеджера и админа */}
                {(isManager || isAdmin) && (
                  <Link
                    to="/manager"
                    className="text-purple-600 hover:text-purple-800 px-3 py-2 rounded-md text-sm font-medium border border-purple-200"
                  >
                    👥 Панель менеджера
                  </Link>
                )}
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="text-right">
                <span className="text-gray-700 block">{user?.username}</span>
                <span className="text-xs text-gray-500">
                  {isAdmin ? 'Администратор' : isManager ? 'Менеджер' : 'Пользователь'}
                </span>
              </div>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                Выйти
              </Button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
};