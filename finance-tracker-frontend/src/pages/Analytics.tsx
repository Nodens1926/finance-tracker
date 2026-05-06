import React, { useState, useEffect } from 'react';
import { analyticsApi } from '../api/analytics';
import { Card } from '../components/UI/Card';
import { Button } from '../components/UI/Button';
import { Input } from '../components/UI/Input';
import { CategoryExpense } from '../types';

export const Analytics: React.FC = () => {
  const [expenses, setExpenses] = useState<CategoryExpense[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    fetchExpenses();
  }, []);

  const fetchExpenses = async () => {
    setIsLoading(true);
    try {
      const data = await analyticsApi.getExpensesByCategory(startDate || undefined, endDate || undefined);
      setExpenses(data);
    } catch (error) {
      console.error('Ошибка загрузки аналитики:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchExpenses();
  };

  const total = expenses.reduce((sum, item) => sum + item.amount, 0);

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold">Аналитика расходов</h1>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Начальная дата"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
            <Input
              label="Конечная дата"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
            <div className="flex items-end">
              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? 'Загрузка...' : 'Применить фильтр'}
              </Button>
            </div>
          </div>
        </form>

        {isLoading ? (
          <div className="text-center py-8">Загрузка...</div>
        ) : expenses.length === 0 ? (
          <div className="text-center py-8 text-gray-500">Нет данных за выбранный период</div>
        ) : (
          <>
            <div className="mb-4">
              <h3 className="text-lg font-semibold mb-2">Общая сумма расходов</h3>
              <p className="text-3xl font-bold text-red-600">
                {total.toLocaleString()} ₽
              </p>
            </div>

            <div className="space-y-4">
              {expenses.map((item) => {
                const percentage = total > 0 ? (item.amount / total) * 100 : 0;
                return (
                  <div key={item.category} className="space-y-2">
                    <div className="flex justify-between">
                      <span className="font-medium">{item.category}</span>
                      <span className="font-bold">
                        {item.amount.toLocaleString()} ₽
                        <span className="ml-2 text-gray-500 text-sm">
                          ({percentage.toFixed(1)}%)
                        </span>
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-red-600 h-2 rounded-full"
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </Card>
    </div>
  );
};