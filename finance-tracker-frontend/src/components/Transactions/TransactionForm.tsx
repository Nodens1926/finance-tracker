import React, { useState, useEffect } from 'react';
import { transactionsApi } from '../../api/transactions';
import { Button } from '../UI/Button';
import { Input } from '../UI/Input';
import { Transaction } from '../../types';

interface TransactionFormProps {
  onClose: () => void;
  onSuccess: () => void;
  transaction?: Transaction | null;
}

const categories = {
  INCOME: ['Зарплата', 'Фриланс', 'Инвестиции', 'Бонус', 'Подарки', 'Другое'],
  EXPENSE: [
    'Еда', 'Транспорт', 'Жилье', 'Аренда', 'Ипотека', 'Коммунальные',
    'Развлечения', 'Здоровье', 'Образование', 'Одежда', 'Техника',
    'Ресторан', 'Кафе', 'Путешествия', 'Спорт', 'Подарки', 'Автомобиль',
    'Бензин', 'Связь', 'Косметика', 'Дети', 'Другое'
  ]
};

export const TransactionForm: React.FC<TransactionFormProps> = ({ 
  onClose, 
  onSuccess, 
  transaction 
}) => {
  const [formData, setFormData] = useState({
    amount: '',
    type: 'EXPENSE' as 'INCOME' | 'EXPENSE',
    category: '',
    date: new Date().toISOString().split('T')[0],
    description: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Заполняем форму данными при редактировании
  useEffect(() => {
    if (transaction) {
      setFormData({
        amount: transaction.amount.toString(),
        type: transaction.type,
        category: transaction.category,
        date: transaction.date,
        description: transaction.description || '',
      });
    }
  }, [transaction]);

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};
    
    if (!formData.amount || parseFloat(formData.amount) <= 0) {
      errors.amount = 'Сумма должна быть больше 0';
    }
    
    if (!formData.category) {
      errors.category = 'Выберите категорию';
    }
    
    if (!formData.date) {
      errors.date = 'Выберите дату';
    }
    
    const selectedDate = new Date(formData.date);
    if (selectedDate > new Date()) {
      errors.date = 'Дата не может быть в будущем';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: name === 'amount' ? value.replace(',', '.') : value,
    });
    // Очищаем ошибку валидации для этого поля
    if (validationErrors[name]) {
      setValidationErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleTypeChange = (type: 'INCOME' | 'EXPENSE') => {
    setFormData({
      ...formData,
      type,
      // Сбрасываем категорию при смене типа
      category: '',
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    try {
      const amount = parseFloat(formData.amount);
      const transactionData = {
        amount,
        type: formData.type,
        category: formData.category,
        date: formData.date,
        description: formData.description,
      };

      if (transaction) {
        await transactionsApi.update(transaction.id, transactionData);
      } else {
        await transactionsApi.create(transactionData);
      }
      
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.error || (transaction ? 'Ошибка при обновлении' : 'Ошибка при создании'));
    } finally {
      setIsLoading(false);
    }
  };

  const availableCategories = categories[formData.type];

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full" onClick={(e) => e.stopPropagation()}>
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold">
              {transaction ? '✏️ Редактировать транзакцию' : '➕ Добавить транзакцию'}
            </h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 text-2xl"
            >
              &times;
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Тип *
              </label>
              <div className="flex space-x-2">
                <button
                  type="button"
                  onClick={() => handleTypeChange('INCOME')}
                  className={`flex-1 py-2 px-4 rounded-lg border transition-colors ${
                    formData.type === 'INCOME'
                      ? 'bg-green-100 border-green-500 text-green-700 font-medium'
                      : 'bg-gray-100 border-gray-300 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  💰 Доход
                </button>
                <button
                  type="button"
                  onClick={() => handleTypeChange('EXPENSE')}
                  className={`flex-1 py-2 px-4 rounded-lg border transition-colors ${
                    formData.type === 'EXPENSE'
                      ? 'bg-red-100 border-red-500 text-red-700 font-medium'
                      : 'bg-gray-100 border-gray-300 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  💸 Расход
                </button>
              </div>
            </div>

            <Input
              label="Сумма *"
              type="number"
              step="0.01"
              name="amount"
              value={formData.amount}
              onChange={handleChange}
              placeholder="0.00"
              error={validationErrors.amount}
              required
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Категория *
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  validationErrors.category ? 'border-red-500' : 'border-gray-300'
                }`}
                required
              >
                <option value="">Выберите категорию</option>
                {availableCategories.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
              {validationErrors.category && (
                <p className="mt-1 text-sm text-red-600">{validationErrors.category}</p>
              )}
            </div>

            <Input
              label="Дата *"
              type="date"
              name="date"
              value={formData.date}
              onChange={handleChange}
              error={validationErrors.date}
              required
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Описание
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={3}
                placeholder="Дополнительная информация..."
              />
            </div>

            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            )}

            <div className="flex justify-end space-x-3 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isLoading}
              >
                Отмена
              </Button>
              <Button
                type="submit"
                variant="primary"
                disabled={isLoading}
              >
                {isLoading 
                  ? (transaction ? 'Обновление...' : 'Сохранение...') 
                  : (transaction ? '💾 Обновить' : '✅ Сохранить')}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};