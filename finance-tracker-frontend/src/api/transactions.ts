// api/transactions.ts
import api from '../utils/api';
import { Transaction, TransactionFilterParams, Page } from '../types';

export const transactionsApi = {
  // Старый метод - для обратной совместимости (загружает все)
  getAll: (startDate?: string, endDate?: string): Promise<Transaction[]> =>
    api.get('/transactions', { params: { startDate, endDate } }).then(res => res.data),
  
  // НОВЫЙ МЕТОД: Фильтрация с пагинацией и сортировкой
  getFiltered: (params: TransactionFilterParams): Promise<Page<Transaction>> =>
    api.get('/transactions/filtered', { params }).then(res => res.data),
  
  create: (transaction: Omit<Transaction, 'id'>): Promise<Transaction> =>
    api.post('/transactions', transaction).then(res => res.data),
  
  update: (id: number, transaction: Partial<Transaction>): Promise<Transaction> =>
    api.put(`/transactions/${id}`, transaction).then(res => res.data),
  
  delete: async (id: number): Promise<void> => {
    await api.delete(`/transactions/${id}`);
  },
  
  export: (): Promise<Blob> =>
    api.get('/transactions/export', { responseType: 'blob' }).then(res => res.data),
};