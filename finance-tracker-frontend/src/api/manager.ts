import api from '../utils/api';
import { Transaction, User } from '../types';

export const managerApi = {
  // Получить всех пользователей (только для ADMIN)
  getAllUsers: (): Promise<User[]> => 
    api.get('/admin/users').then(res => res.data),
  
  // Получить транзакции конкретного пользователя (для MANAGER и ADMIN)
  getUserTransactions: (userId: number): Promise<Transaction[]> => 
    api.get(`/manager/users/${userId}/transactions`).then(res => res.data),
  
  // Получить аналитику пользователя (для MANAGER и ADMIN)
  getUserAnalytics: (userId: number, startDate?: string, endDate?: string): Promise<{ category: string; amount: number }[]> =>
    api.get(`/manager/users/${userId}/analytics`, { params: { startDate, endDate } }).then(res => res.data),
  
  // Получить summary пользователя (для MANAGER и ADMIN)
  getUserSummary: (userId: number): Promise<{ totalIncome: number; totalExpense: number; balance: number }> =>
    api.get(`/manager/users/${userId}/summary`).then(res => res.data),
};