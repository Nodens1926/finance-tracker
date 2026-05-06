import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/auth';
import { User, LoginRequest, UserRole, LoginResponse } from '../types';

interface AuthContextType {
  user: User | null;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  logoutAllDevices: () => Promise<void>;
  refreshToken: () => Promise<void>;
  isLoading: boolean;
  hasRole: (roles: UserRole | UserRole[]) => boolean;
  hasAnyRole: (roles: UserRole[]) => boolean;
  isAdmin: boolean;
  isManager: boolean;
  accessToken: string | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshTimeout, setRefreshTimeout] = useState<number | null>(null);

  // Загрузка данных при старте
  useEffect(() => {
    const loadStoredData = async () => {
      const storedAccessToken = localStorage.getItem('access_token');
      const storedRefreshToken = localStorage.getItem('refresh_token');
      const storedExpiresIn = localStorage.getItem('expires_in');
      const storedUser = localStorage.getItem('user');

      if (storedAccessToken && storedRefreshToken && storedUser) {
        try {
          setAccessToken(storedAccessToken);
          setRefreshToken(storedRefreshToken);
          
          // Парсим пользователя
          const parsedUser = JSON.parse(storedUser);
          console.log('Загружен пользователь из localStorage:', parsedUser); // ДИАГНОСТИКА
          setUser(parsedUser);

          // Запускаем таймер обновления токена
          if (storedExpiresIn) {
            scheduleTokenRefresh(parseInt(storedExpiresIn));
          }
        } catch (e) {
          console.error('Ошибка загрузки данных:', e);
          clearStoredData();
        }
      }
      setIsLoading(false);
    };

    loadStoredData();
  }, []);

  // Очистка таймера при размонтировании
  useEffect(() => {
    return () => {
      if (refreshTimeout) {
        clearTimeout(refreshTimeout);
      }
    };
  }, [refreshTimeout]);

  const scheduleTokenRefresh = (expiresIn: number) => {
    // Обновляем за 1 минуту до истечения
    const timeout = (expiresIn - 60) * 1000;
    
    if (refreshTimeout) {
      clearTimeout(refreshTimeout);
    }

    const timeoutId = setTimeout(async () => {
      await refreshAccessToken();
    }, timeout);

    setRefreshTimeout(timeoutId);
  };

  const refreshAccessToken = async () => {
    const currentRefreshToken = localStorage.getItem('refresh_token');
    if (!currentRefreshToken) return;

    try {
      const response = await authApi.refreshToken(currentRefreshToken);
      setTokens(response);
    } catch (error) {
      console.error('Ошибка обновления токена:', error);
      await logout();
    }
  };

  const setTokens = (response: LoginResponse) => {
    setAccessToken(response.accessToken);
    setRefreshToken(response.refreshToken);
    
    localStorage.setItem('access_token', response.accessToken);
    localStorage.setItem('refresh_token', response.refreshToken);
    localStorage.setItem('expires_in', response.expiresIn.toString());

    scheduleTokenRefresh(response.expiresIn);
  };

  const clearStoredData = () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('expires_in');
    localStorage.removeItem('user');
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
  };

  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);
    console.log('=== LOGIN RESPONSE ===');
    console.log('Full response:', response);
    console.log('Roles from response:', response.roles);
    console.log('=====================');
    
    setTokens(response);
    
    // ВАЖНО: Сохраняем роли из ответа
    const userData: User = {
      id: response.id,
      username: response.username,
      email: response.email,
      roles: response.roles || ['ROLE_USER']  // <-- КЛЮЧЕВОЕ ПОЛЕ
    };
    
    console.log('Saving user with roles:', userData);
    
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  const logout = async () => {
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken, false);
      } catch (error) {
        console.error('Ошибка при выходе:', error);
      }
    }
    clearStoredData();
    if (refreshTimeout) {
      clearTimeout(refreshTimeout);
    }
  };

  const logoutAllDevices = async () => {
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken, true);
      } catch (error) {
        console.error('Ошибка при выходе со всех устройств:', error);
      }
    }
    clearStoredData();
    if (refreshTimeout) {
      clearTimeout(refreshTimeout);
    }
  };

  const refreshToken_ = async () => {
    await refreshAccessToken();
  };

  const hasRole = (roles: UserRole | UserRole[]): boolean => {
    if (!user?.roles) {
      console.log('hasRole: нет ролей у пользователя', user); // ДИАГНОСТИКА
      return false;
    }
    
    const roleList = Array.isArray(roles) ? roles : [roles];
    const result = user.roles.some(userRole => 
      roleList.includes(userRole as UserRole)
    );
    
    console.log('hasRole:', { userRoles: user.roles, required: roleList, result }); // ДИАГНОСТИКА
    return result;
  };

  const hasAnyRole = (roles: UserRole[]): boolean => {
    return hasRole(roles);
  };

  // Вычисляем isAdmin и isManager на основе ролей пользователя
  const isAdmin = user?.roles?.includes('ROLE_ADMIN') || false;
  const isManager = user?.roles?.includes('ROLE_MANAGER') || false;

  console.log('AuthProvider render:', { user, isAdmin, isManager }); // ДИАГНОСТИКА

  return (
    <AuthContext.Provider value={{ 
      user, 
      accessToken,
      login, 
      logout,
      logoutAllDevices,
      refreshToken: refreshToken_,
      isLoading,
      hasRole,
      hasAnyRole,
      isAdmin,
      isManager
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};