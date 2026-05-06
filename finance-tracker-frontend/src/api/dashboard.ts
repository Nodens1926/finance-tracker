import api from '../utils/api';
import { DashboardSummary } from '../types';

export const dashboardApi = {
  getSummary: (): Promise<DashboardSummary> =>
    api.get('/dashboard/summary').then(res => {
      console.log('Dashboard API response:', res.data);
      return res.data;
    }),
};