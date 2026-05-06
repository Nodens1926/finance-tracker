import api from '../utils/api';
import { CategoryExpense } from '../types';

export const analyticsApi = {
  getExpensesByCategory: (startDate?: string, endDate?: string): Promise<CategoryExpense[]> =>
    api.get('/analytics/expenses-by-category', { params: { startDate, endDate } }).then(res => {
      console.log('Analytics API response:', res.data);
      return res.data || [];
    }),
};