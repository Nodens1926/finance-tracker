// src/components/Widgets/ExchangeRateWidget.tsx
import React, { useState, useCallback } from 'react';
import { Card } from '../UI/Card';
import { Button } from '../UI/Button';

// Фиксированный список валют (вместо динамической загрузки)
const CURRENCIES = ['USD', 'EUR', 'GBP', 'RUB', 'CNY', 'JPY', 'CHF', 'CAD', 'AUD'];

export const ExchangeRateWidget: React.FC = () => {
  const [amount, setAmount] = useState<number>(100);
  const [fromCurrency, setFromCurrency] = useState<string>('USD');
  const [toCurrency, setToCurrency] = useState<string>('EUR');
  const [result, setResult] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const convert = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    setResult(null);

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 8000);

      const response = await fetch(
        `http://localhost:8080/api/exchange-rate/convert?amount=${amount}&from=${fromCurrency}&to=${toCurrency}`,
        { signal: controller.signal }
      );

      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new Error('API request failed');
      }

      const data = await response.json();
      setResult(data.convertedAmount);
    } catch (err) {
      console.error('Exchange rate error:', err);
      setError('Сервис курсов валют временно недоступен');
      
      // Fallback курсы
      const fallbackRates: Record<string, number> = { USD: 1, EUR: 0.92, GBP: 0.79, RUB: 92.5, CNY: 7.2, JPY: 150, CHF: 0.88, CAD: 1.35, AUD: 1.48 };
      const fromRate = fallbackRates[fromCurrency] || 1;
      const toRate = fallbackRates[toCurrency] || 1;
      const fallbackResult = amount * (toRate / fromRate);
      setResult(parseFloat(fallbackResult.toFixed(2)));
    } finally {
      setIsLoading(false);
    }
  }, [amount, fromCurrency, toCurrency]);

  return (
    <Card className="bg-gradient-to-r from-blue-50 to-indigo-50">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold flex items-center gap-2">
            💱 Конвертер валют
          </h3>
          <p className="text-xs text-gray-500">Актуальные курсы от ExchangeRate.host</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Сумма</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              min="0"
              step="10"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Из</label>
              <select
                value={fromCurrency}
                onChange={(e) => setFromCurrency(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                {CURRENCIES.map(curr => (
                  <option key={curr} value={curr}>{curr}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">В</label>
              <select
                value={toCurrency}
                onChange={(e) => setToCurrency(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                {CURRENCIES.map(curr => (
                  <option key={curr} value={curr}>{curr}</option>
                ))}
              </select>
            </div>
          </div>

          <Button onClick={convert} disabled={isLoading || amount <= 0} className="w-full">
            {isLoading ? 'Конвертация...' : 'Конвертировать →'}
          </Button>
        </div>

        <div className="bg-white rounded-lg p-4 flex flex-col justify-center items-center text-center">
          {isLoading ? (
            <div className="text-gray-500">Загрузка курса...</div>
          ) : error ? (
            <div className="text-red-600">
              <div className="text-2xl mb-1">⚠️</div>
              <p className="text-sm">{error}</p>
              <p className="text-xs text-gray-400 mt-2">Используются резервные курсы</p>
            </div>
          ) : result !== null ? (
            <>
              <div className="text-3xl font-bold text-gray-800">
                {result.toFixed(2)} {toCurrency}
              </div>
              <div className="text-sm text-gray-500 mt-2">
                {amount} {fromCurrency}
              </div>
              {error && (
                <div className="text-xs text-amber-600 mt-2">офлайн-оценка</div>
              )}
            </>
          ) : (
            <div className="text-gray-400">
              <div className="text-2xl mb-1">💱</div>
              <p className="text-sm">Введите сумму и нажмите "Конвертировать"</p>
            </div>
          )}
        </div>
      </div>
    </Card>
  );
};