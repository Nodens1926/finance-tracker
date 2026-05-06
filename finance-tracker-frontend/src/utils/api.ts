import axios from 'axios';
import { authApi } from '../api/auth';

const API_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Флаг для предотвращения множественных обновлений
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: string) => void;
  reject: (reason?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
};

// Request interceptor - добавляем токен
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - обрабатываем 401 и обновляем токен
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Если ошибка не 401 или запрос уже на обновление - отклоняем
    if (error.response?.status !== 401 || originalRequest.url?.includes('/auth/refresh')) {
      // Если токен истек и это не запрос на обновление - редирект на логин
      if (error.response?.status === 401 && !originalRequest.url?.includes('/auth/')) {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('expires_in');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    if (isRefreshing) {
      // Если уже обновляем токен, добавляем запрос в очередь
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        })
        .catch(err => Promise.reject(err));
    }

    isRefreshing = true;

    try {
      const refreshToken = localStorage.getItem('refresh_token');
      if (!refreshToken) {
        throw new Error('No refresh token');
      }

      const response = await authApi.refreshToken(refreshToken);
      
      localStorage.setItem('access_token', response.accessToken);
      localStorage.setItem('refresh_token', response.refreshToken);
      localStorage.setItem('expires_in', response.expiresIn.toString());
      
      api.defaults.headers.common.Authorization = `Bearer ${response.accessToken}`;
      originalRequest.headers.Authorization = `Bearer ${response.accessToken}`;
      
      processQueue(null, response.accessToken);
      
      return api(originalRequest);
    } catch (refreshError: any) {
      // Если ошибка "Token reuse detected" или другие ошибки авторизации
      if (refreshError.response?.data?.error?.includes('Token reuse') ||
          refreshError.response?.data?.error?.includes('permissions have changed')) {
        console.error('Security alert:', refreshError.response?.data?.error);
      }
      
      processQueue(refreshError, null);
      
      // Очищаем данные и редиректим на логин
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('expires_in');
      localStorage.removeItem('user');
      window.location.href = '/login';
      
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;