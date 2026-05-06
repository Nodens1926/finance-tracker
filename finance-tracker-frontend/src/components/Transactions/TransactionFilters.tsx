// components/Transactions/TransactionFilters.tsx
import React, { useState, useEffect } from 'react';
import { Button } from '../UI/Button';
import { Input } from '../UI/Input';
import { Select } from '../UI/Select';
import { TransactionFilterParams } from '../../types';

interface TransactionFiltersProps {
  filters: TransactionFilterParams;
  onFilterChange: (filters: Partial<TransactionFilterParams>) => void;
  onReset: () => void;
}

const categories = [
  { value: '', label: 'Все категории' },
  { value: 'Еда', label: 'Еда' },
  { value: 'Транспорт', label: 'Транспорт' },
  { value: 'Жилье', label: 'Жилье' },
  { value: 'Аренда', label: 'Аренда' },
  { value: 'Ипотека', label: 'Ипотека' },
  { value: 'Коммунальные', label: 'Коммунальные' },
  { value: 'Развлечения', label: 'Развлечения' },
  { value: 'Здоровье', label: 'Здоровье' },
  { value: 'Образование', label: 'Образование' },
  { value: 'Одежда', label: 'Одежда' },
  { value: 'Техника', label: 'Техника' },
  { value: 'Ресторан', label: 'Ресторан' },
  { value: 'Кафе', label: 'Кафе' },
  { value: 'Путешествия', label: 'Путешествия' },
  { value: 'Спорт', label: 'Спорт' },
  { value: 'Подарки', label: 'Подарки' },
  { value: 'Автомобиль', label: 'Автомобиль' },
  { value: 'Бензин', label: 'Бензин' },
  { value: 'Связь', label: 'Связь' },
  { value: 'Косметика', label: 'Косметика' },
  { value: 'Дети', label: 'Дети' },
  { value: 'Фриланс', label: 'Фриланс' },
  { value: 'Инвестиции', label: 'Инвестиции' },
  { value: 'Бонус', label: 'Бонус' },
  { value: 'Зарплата', label: 'Зарплата' },
  { value: 'Другое', label: 'Другое' }
];

const transactionTypes = [
  { value: '', label: 'Все типы' },
  { value: 'INCOME', label: 'Доходы' },
  { value: 'EXPENSE', label: 'Расходы' },
];

const sortOptions = [
  { value: 'date', label: 'По дате' },
  { value: 'amount', label: 'По сумме' },
  { value: 'category', label: 'По категории' },
  { value: 'id', label: 'По ID' },
];

const sortDirections = [
  { value: 'desc', label: 'По убыванию ↓' },
  { value: 'asc', label: 'По возрастанию ↑' },
];

const pageSizes = [
  { value: '5', label: '5' },
  { value: '10', label: '10' },
  { value: '20', label: '20' },
  { value: '50', label: '50' },
  { value: '100', label: '100' },
];

export const TransactionFilters: React.FC<TransactionFiltersProps> = ({
  filters,
  onFilterChange,
  onReset,
}) => {
  const [localFilters, setLocalFilters] = useState(filters);

  // Синхронизация локального состояния с пропсами
  useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  const handleChange = (key: keyof TransactionFilterParams, value: any) => {
    const newFilters = { ...localFilters, [key]: value };
    // При изменении любых фильтров кроме page, сбрасываем страницу на 0
    if (key !== 'page') {
      newFilters.page = 0;
    }
    setLocalFilters(newFilters);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFilterChange(localFilters);
  };

  const handleReset = () => {
    setLocalFilters({
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
    onReset();
  };

  return (
    <div className="bg-white rounded-lg shadow p-6 mb-6">
      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {/* Поиск (1 параметр) */}
          <Input
            label="🔍 Поиск"
            placeholder="Поиск по описанию или категории..."
            value={localFilters.search || ''}
            onChange={(e) => handleChange('search', e.target.value || undefined)}
          />

          {/* Тип транзакции (2 параметр) */}
          <Select
            label="Тип"
            value={localFilters.type || ''}
            options={transactionTypes}
            onChange={(e) => handleChange('type', e.target.value || undefined)}
          />

          {/* Категория (3 параметр) */}
          <Select
            label="Категория"
            value={localFilters.category || ''}
            options={categories}
            onChange={(e) => handleChange('category', e.target.value || undefined)}
          />

          {/* Минимальная сумма (4 параметр) */}
          <Input
            label="Мин. сумма (₽)"
            type="number"
            step="0.01"
            placeholder="0"
            value={localFilters.minAmount !== undefined ? localFilters.minAmount : ''}
            onChange={(e) => handleChange('minAmount', e.target.value ? parseFloat(e.target.value) : undefined)}
          />

          {/* Максимальная сумма (5 параметр) */}
          <Input
            label="Макс. сумма (₽)"
            type="number"
            step="0.01"
            placeholder="100000"
            value={localFilters.maxAmount !== undefined ? localFilters.maxAmount : ''}
            onChange={(e) => handleChange('maxAmount', e.target.value ? parseFloat(e.target.value) : undefined)}
          />

          {/* Начальная дата (6 параметр) */}
          <Input
            label="Дата от"
            type="date"
            value={localFilters.startDate || ''}
            onChange={(e) => handleChange('startDate', e.target.value || undefined)}
          />

          {/* Конечная дата (7 параметр) */}
          <Input
            label="Дата до"
            type="date"
            value={localFilters.endDate || ''}
            onChange={(e) => handleChange('endDate', e.target.value || undefined)}
          />

          {/* Сортировка по */}
          <Select
            label="Сортировать по"
            value={localFilters.sortBy || 'date'}
            options={sortOptions}
            onChange={(e) => handleChange('sortBy', e.target.value)}
          />

          {/* Направление сортировки */}
          <Select
            label="Направление"
            value={localFilters.sortDir || 'desc'}
            options={sortDirections}
            onChange={(e) => handleChange('sortDir', e.target.value as 'asc' | 'desc')}
          />

          {/* Размер страницы */}
          <Select
            label="Записей на странице"
            value={String(localFilters.size || 10)}
            options={pageSizes}
            onChange={(e) => handleChange('size', parseInt(e.target.value))}
          />

          {/* Текущая страница (информационно) */}
          <div className="flex items-end">
            <div className="text-sm text-gray-500 pb-2">
              Страница: {(localFilters.page || 0) + 1}
            </div>
          </div>
        </div>

        <div className="flex justify-end space-x-3 mt-4">
          <Button type="button" variant="outline" onClick={handleReset}>
            🗑️ Сбросить все
          </Button>
          <Button type="submit" variant="primary">
            🔍 Применить фильтры
          </Button>
        </div>
      </form>
    </div>
  );
};