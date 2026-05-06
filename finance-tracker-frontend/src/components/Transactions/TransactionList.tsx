// components/Transactions/TransactionList.tsx
import React, { useState } from 'react';
import { Transaction } from '../../types';
import { Button } from '../UI/Button';

interface TransactionListProps {
  transactions: Transaction[];
  onEdit: (transaction: Transaction) => void;
  onDelete: (id: number) => void;
  onSelect?: (transaction: Transaction) => void;
  selectedTransactionId?: number | null;
}

export const TransactionList: React.FC<TransactionListProps> = ({
  transactions,
  onEdit,
  onDelete,
  onSelect,
  selectedTransactionId,
}) => {
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const handleDeleteClick = (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    setDeleteConfirmId(id);
  };

  const handleConfirmDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (deleteConfirmId !== null) {
      onDelete(deleteConfirmId);
      setDeleteConfirmId(null);
    }
  };

  const handleCancelDelete = (e: React.MouseEvent) => {
  e.stopPropagation();
  setDeleteConfirmId(null);
  };

  const handleRowClick = (transaction: Transaction) => {
    if (onSelect) {
      onSelect(transaction);
    }
  };

  if (transactions.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        Нет транзакций. Добавьте первую транзакцию!
      </div>
    );
  }

  return (
    <>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Дата
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Тип
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Категория
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Сумма
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Описание
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Действия
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {transactions.map((transaction) => (
              <tr 
                key={transaction.id} 
                className={`hover:bg-gray-50 cursor-pointer transition-colors ${
                  selectedTransactionId === transaction.id ? 'bg-blue-50 border-l-4 border-blue-500' : ''
                }`}
                onClick={() => handleRowClick(transaction)}
              >
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {new Date(transaction.date).toLocaleDateString('ru-RU')}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                    transaction.type === 'INCOME' 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-red-100 text-red-800'
                  }`}>
                    {transaction.type === 'INCOME' ? '💰 Доход' : '💸 Расход'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {transaction.category}
                </td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold ${
                  transaction.type === 'INCOME' ? 'text-green-600' : 'text-red-600'
                }`}>
                  {transaction.amount.toLocaleString()} ₽
                </td>
                <td className="px-6 py-4 text-sm text-gray-900 max-w-xs truncate">
                  {transaction.description || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2" onClick={(e) => e.stopPropagation()}>
                  <button 
                    className="text-blue-600 hover:text-blue-900"
                    onClick={() => onEdit(transaction)}
                  >
                    ✏️ Изменить
                  </button>
                  <button 
                    className="text-red-600 hover:text-red-900"
                    onClick={(e) => handleDeleteClick(transaction.id, e)}
                  >
                    🗑️ Удалить
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

    {/* Модальное окно подтверждения удаления */}
    {deleteConfirmId !== null && (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-4">Подтверждение удаления</h3>
          <p className="mb-6">Вы уверены, что хотите удалить эту транзакцию? Это действие нельзя отменить.</p>
          <div className="flex justify-end space-x-3">
            <button
              className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300"
              onClick={handleCancelDelete}
            >
              Отмена
            </button>
            <button
              className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
              onClick={handleConfirmDelete}
            >
              Удалить
            </button>
          </div>
        </div>
      </div>
    )}
    </>
  );
};