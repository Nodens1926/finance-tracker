// hooks/useQueryParams.ts
import { useSearchParams } from 'react-router-dom';
import { useMemo, useCallback } from 'react';
import { TransactionFilterParams } from '../types';

export const useQueryParams = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Получение параметров из URL
  const filters: TransactionFilterParams = useMemo(() => {
    const page = searchParams.get('page');
    const size = searchParams.get('size');
    const sortBy = searchParams.get('sortBy');
    const sortDir = searchParams.get('sortDir') as 'asc' | 'desc' | null;
    const type = searchParams.get('type');
    const category = searchParams.get('category');
    const minAmount = searchParams.get('minAmount');
    const maxAmount = searchParams.get('maxAmount');
    const startDate = searchParams.get('startDate');
    const endDate = searchParams.get('endDate');
    const search = searchParams.get('search');

    return {
      page: page ? parseInt(page) : 0,
      size: size ? parseInt(size) : 10,
      sortBy: sortBy || 'date',
      sortDir: sortDir || 'desc',
      type: type || undefined,
      category: category || undefined,
      minAmount: minAmount ? parseFloat(minAmount) : undefined,
      maxAmount: maxAmount ? parseFloat(maxAmount) : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      search: search || undefined,
    };
  }, [searchParams]);

  // Обновление параметров в URL
  const updateFilters = useCallback((newFilters: Partial<TransactionFilterParams>) => {
    setSearchParams((prev) => {
      const params = new URLSearchParams(prev);
      
      Object.entries(newFilters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.set(key, String(value));
        } else {
          params.delete(key);
        }
      });
      
      // Сброс страницы при изменении любых фильтров кроме page
      if (Object.keys(newFilters).some(k => k !== 'page')) {
        params.set('page', '0');
      }
      
      return params;
    });
  }, [setSearchParams]);

  // Сброс всех фильтров
  const resetFilters = useCallback(() => {
    setSearchParams(new URLSearchParams({
      page: '0',
      size: '10',
      sortBy: 'date',
      sortDir: 'desc',
    }));
  }, [setSearchParams]);

  return { filters, updateFilters, resetFilters };
};