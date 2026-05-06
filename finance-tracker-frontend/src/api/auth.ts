import api from '../utils/api';
import { LoginRequest, LoginResponse, RefreshRequest, LogoutRequest, User } from '../types';

export const authApi = {
  login: (credentials: LoginRequest): Promise<LoginResponse> =>
    api.post('/auth/signin', credentials).then(res => res.data),
  
  register: (userData: {
    username: string;
    email: string;
    password: string;
  }): Promise<string> =>
    api.post('/auth/signup', userData).then(res => res.data),
  
  refreshToken: (refreshToken: string): Promise<LoginResponse> =>  // <-- ДОБАВИТЬ
    api.post('/auth/refresh', { refreshToken } as RefreshRequest).then(res => res.data),
  
  logout: (refreshToken: string, logoutAllDevices = false): Promise<void> => {  // <-- ДОБАВИТЬ
    const accessToken = localStorage.getItem('access_token');
    return api.post('/auth/logout', {
      refreshToken,
      logoutAllDevices
    } as LogoutRequest, {
      headers: { Authorization: `Bearer ${accessToken}` }
    }).then(res => res.data);
  },
  
  getCurrentUser: (): Promise<User> =>  // <-- ДОБАВИТЬ
    api.get('/auth/me').then(res => res.data),
};