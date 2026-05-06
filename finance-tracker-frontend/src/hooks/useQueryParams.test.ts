import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { useQueryParams } from './useQueryParams';

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <BrowserRouter>{children}</BrowserRouter>
);

describe('useQueryParams', () => {
  it('returns default filters', () => {
    const { result } = renderHook(() => useQueryParams(), { wrapper });
    
    expect(result.current.filters).toEqual({
      page: 0,
      size: 10,
      sortBy: 'date',
      sortDir: 'desc',
      type: undefined,
      category: undefined,
      minAmount: undefined,
      maxAmount: undefined,
      startDate: undefined,
      endDate: undefined,
      search: undefined,
    });
  });

  it('updates filters in URL', () => {
    const { result } = renderHook(() => useQueryParams(), { wrapper });
    
    act(() => {
      result.current.updateFilters({ type: 'EXPENSE', category: 'Food' });
    });
    
    expect(result.current.filters.type).toBe('EXPENSE');
    expect(result.current.filters.category).toBe('Food');
  });

  it('resets all filters', () => {
    const { result } = renderHook(() => useQueryParams(), { wrapper });
    
    act(() => {
      result.current.updateFilters({ type: 'EXPENSE', page: 5 });
      result.current.resetFilters();
    });
    
    expect(result.current.filters.type).toBeUndefined();
    expect(result.current.filters.page).toBe(0);
  });

  it('resets page when non-page filter changes', () => {
    const { result } = renderHook(() => useQueryParams(), { wrapper });
    
    act(() => {
      result.current.updateFilters({ page: 5 });
      result.current.updateFilters({ category: 'Food' });
    });
    
    expect(result.current.filters.page).toBe(0);
  });
});