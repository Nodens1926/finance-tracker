import React, { useEffect, useState } from 'react';
import { dashboardApi } from '../api/dashboard';
import { analyticsApi } from '../api/analytics';
import { Card } from '../components/UI/Card';
import { Button } from '../components/UI/Button';
import { HelmetSEO } from '../components/SEO/HelmetSEO';
import { ExchangeRateWidget } from '../components/Widgets/ExchangeRateWidget';
import { DashboardSummary, CategoryExpense } from '../types';
import { useNavigate } from 'react-router-dom';

export const Dashboard: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [categoryExpenses, setCategoryExpenses] = useState<CategoryExpense[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [summaryData, categoryData] = await Promise.all([
        dashboardApi.getSummary(),
        analyticsApi.getExpensesByCategory()
      ]);
      setSummary(summaryData);
      setCategoryExpenses(categoryData || []);
    } catch (error: any) {
      console.error('Ошибка загрузки данных:', error);
      setError(error.response?.data?.error || 'Ошибка загрузки данных');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // JSON-LD для структурированных данных (WebApplication)
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "WebApplication",
    "name": "Финансовый Трекер",
    "description": "Приложение для отслеживания личных финансов, доходов и расходов",
    "applicationCategory": "FinanceApplication",
    "operatingSystem": "Web",
    "offers": {
      "@type": "Offer",
      "price": "0",
      "priceCurrency": "RUB"
    }
  };

  const totalExpense = summary?.totalExpense || 0;
  const totalIncome = summary?.totalIncome || 0;
  const balance = summary?.balance || 0;

  return (
    <>
      <HelmetSEO
        title="Дашборд"
        description="Обзор ваших финансов: доходы, расходы, баланс и аналитика по категориям"
        keywords="дашборд, финансы, доходы, расходы, баланс, аналитика"
        canonical="https://yourdomain.com/dashboard"
        ogTitle="Финансовый Дашборд"
        ogDescription="Отслеживайте свои финансы в реальном времени"
        jsonLd={jsonLd}
      />
      
      <div className="space-y-6 p-6">
        {/* Виджет курсов валют - новая интеграция */}
        <ExchangeRateWidget />

        <div>
          <h1 className="text-2xl font-bold">📊 Дашборд</h1>
          <p className="text-gray-500 text-sm mt-1">Обзор ваших финансов</p>
        </div>

        {/* Карточки статистики с lazy loading для иконок */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card className="text-center hover:shadow-lg transition-shadow">
            <div className="flex flex-col items-center">
              <img src="/icons/income.svg" alt="Доходы" className="w-12 h-12 mb-2" loading="lazy" />
              <h3 className="text-lg font-semibold text-gray-600">Доходы</h3>
              <p className="text-3xl font-bold text-green-600 mt-2">
                {totalIncome.toLocaleString()} ₽
              </p>
            </div>
          </Card>
          
          <Card className="text-center hover:shadow-lg transition-shadow">
            <div className="flex flex-col items-center">
              <img src="/icons/expense.svg" alt="Расходы" className="w-12 h-12 mb-2" loading="lazy" />
              <h3 className="text-lg font-semibold text-gray-600">Расходы</h3>
              <p className="text-3xl font-bold text-red-600 mt-2">
                {totalExpense.toLocaleString()} ₽
              </p>
            </div>
          </Card>
          
          <Card className="text-center hover:shadow-lg transition-shadow">
            <div className="flex flex-col items-center">
              <img src="/icons/balance.svg" alt="Баланс" className="w-12 h-12 mb-2" loading="lazy" />
              <h3 className="text-lg font-semibold text-gray-600">Баланс</h3>
              <p className={`text-3xl font-bold mt-2 ${
                balance >= 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {balance.toLocaleString()} ₽
              </p>
            </div>
          </Card>
        </div>

        {/* Остальной код без изменений... */}
        <Card>
          <div className="flex justify-between items-center mb-4">
            <div>
              <h3 className="text-lg font-semibold">📈 Расходы по категориям</h3>
              <p className="text-xs text-gray-400 mt-1">за текущий месяц</p>
            </div>
            <Button variant="outline" size="sm" onClick={() => navigate('/analytics')}>
              Подробнее →
            </Button>
          </div>
          
          {categoryExpenses.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <p>Нет данных о расходах за текущий месяц</p>
            </div>
          ) : (
            <div className="space-y-4">
              {categoryExpenses.slice(0, 5).map((item) => {
                const percentage = totalExpense > 0 ? (item.amount / totalExpense) * 100 : 0;
                return (
                  <div key={item.category} className="space-y-1">
                    <div className="flex justify-between text-sm">
                      <span className="font-medium text-gray-700">{item.category}</span>
                      <span className="font-semibold text-red-600">
                        {item.amount.toLocaleString()} ₽
                        <span className="ml-2 text-gray-400 text-xs">
                          ({percentage.toFixed(1)}%)
                        </span>
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-red-500 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${Math.min(percentage, 100)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>
    </>
  );
};