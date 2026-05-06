// pages/Transactions.tsx
import React, { useState, useEffect, useCallback } from 'react';
import { transactionsApi } from '../api/transactions';
import { attachmentsApi } from '../api/attachments';
import { Button } from '../components/UI/Button';
import { Card } from '../components/UI/Card';
import { TransactionForm } from '../components/Transactions/TransactionForm';
import { TransactionFilters } from '../components/Transactions/TransactionFilters';
import { TransactionList } from '../components/Transactions/TransactionList';
import { FileUploader } from '../components/Transactions/FileUploader';
import { AttachmentList } from '../components/Transactions/AttachmentList';
import { Pagination } from '../components/UI/Pagination';
import { useQueryParams } from '../hooks/useQueryParams';
import { Transaction, Attachment, Page } from '../types';

export const Transactions: React.FC = () => {
  const { filters, updateFilters, resetFilters } = useQueryParams();
  
  const [pageData, setPageData] = useState<Page<Transaction> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
  const [selectedTransactionId, setSelectedTransactionId] = useState<number | null>(null);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [isLoadingAttachments, setIsLoadingAttachments] = useState(false);

    const fetchTransactions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      console.log('Загрузка транзакций с фильтрами:', filters);
      const data = await transactionsApi.getFiltered(filters);
      console.log('Получены транзакции:', data);
      setPageData(data);
    } catch (err: any) {
      console.error('Ошибка загрузки транзакций:', err);
      setError(err.response?.data?.error || 'Ошибка загрузки транзакций');
    } finally {
      setIsLoading(false);
    }
  }, [filters]);

  // Обновление после CRUD операций
  const handleFormSuccess = () => {
    fetchTransactions(); // Перезагружаем список
    handleFormClose();
  };

  const handleDelete = async (id: number) => {
    try {
      await transactionsApi.delete(id);
      await fetchTransactions(); // Перезагружаем после удаления
      if (selectedTransactionId === id) {
        setSelectedTransactionId(null);
        setAttachments([]);
      }
    } catch (error) {
      console.error('Ошибка удаления:', error);
    }
  };

  // Загрузка вложений для выбранной транзакции
  const fetchAttachments = useCallback(async (transactionId: number) => {
    setIsLoadingAttachments(true);
    try {
      const data = await attachmentsApi.getByTransaction(transactionId);
      setAttachments(data);
    } catch (error) {
      console.error('Ошибка загрузки вложений:', error);
    } finally {
      setIsLoadingAttachments(false);
    }
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  // При выборе транзакции загружаем её вложения
  const handleSelectTransaction = (transaction: Transaction) => {
    setSelectedTransactionId(transaction.id);
    fetchAttachments(transaction.id);
  };

  const handlePageChange = (page: number) => {
    updateFilters({ page });
  };

  const handleEdit = (transaction: Transaction) => {
    setEditingTransaction(transaction);
    setShowForm(true);
  };

  const handleFormClose = () => {
    setShowForm(false);
    setEditingTransaction(null);
  };

  const handleUploadSuccess = () => {
    if (selectedTransactionId) {
      fetchAttachments(selectedTransactionId);
    }
  };

  const handleAttachmentDeleted = (attachmentId: number) => {
    setAttachments(prev => prev.filter(a => a.id !== attachmentId));
  };

  if (isLoading && !pageData) {
    return (
      <div className="p-6">
        <div className="flex justify-center items-center h-64">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-500">Загрузка транзакций...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error && !pageData) {
    return (
      <div className="p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <p className="text-red-600 mb-4">{error}</p>
          <Button onClick={fetchTransactions}>Попробовать снова</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">📊 Транзакции</h1>
          <p className="text-gray-500 text-sm mt-1">Управление доходами и расходами</p>
        </div>
        <Button 
          variant="primary"
          onClick={() => {
            setEditingTransaction(null);
            setShowForm(true);
          }}
          className="flex items-center gap-2"
        >
          <span>+</span> Добавить транзакцию
        </Button>
      </div>

      {/* Компонент фильтрации */}
      <TransactionFilters
        filters={filters}
        onFilterChange={updateFilters}
        onReset={resetFilters}
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Левая колонка - список транзакций */}
        <div className="lg:col-span-2">
          <Card>
            {pageData && (
              <>
                <div className="flex justify-between items-center mb-4 pb-2 border-b">
                  <div className="text-sm text-gray-500">
                    Найдено: <span className="font-semibold text-gray-700">{pageData.totalElements}</span> транзакций
                  </div>
                  <div className="text-xs text-gray-400">
                    Страница {pageData.number + 1} из {pageData.totalPages}
                  </div>
                </div>
                
                <TransactionList
                  transactions={pageData.content}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onSelect={handleSelectTransaction}
                  selectedTransactionId={selectedTransactionId}
                />
                
                {pageData.totalPages > 1 && (
                  <Pagination
                    currentPage={filters.page || 0}
                    totalPages={pageData.totalPages}
                    onPageChange={handlePageChange}
                  />
                )}
              </>
            )}
          </Card>
        </div>

        {/* Правая колонка - файлы для выбранной транзакции */}
        <div className="lg:col-span-1">
          {selectedTransactionId ? (
            <div className="space-y-4">
              <Card>
                <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                  📎 Загрузка файлов
                </h3>
                <FileUploader
                  transactionId={selectedTransactionId}
                  onUploadSuccess={handleUploadSuccess}
                />
              </Card>

              <Card>
                <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                  📁 Прикрепленные файлы
                  {attachments.length > 0 && (
                    <span className="text-sm bg-gray-100 px-2 py-0.5 rounded-full">
                      {attachments.length}
                    </span>
                  )}
                </h3>
                {isLoadingAttachments ? (
                  <div className="text-center py-4">
                    <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mx-auto"></div>
                    <p className="text-xs text-gray-400 mt-2">Загрузка...</p>
                  </div>
                ) : (
                  <AttachmentList
                    transactionId={selectedTransactionId}
                    attachments={attachments}
                    onAttachmentDeleted={handleAttachmentDeleted}
                  />
                )}
              </Card>
            </div>
          ) : (
            <Card>
              <div className="text-center py-8">
                <div className="text-4xl mb-3">📋</div>
                <p className="text-gray-500">Выберите транзакцию,</p>
                <p className="text-gray-500 text-sm">чтобы управлять файлами</p>
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Модальное окно создания/редактирования */}
      {showForm && (
        <TransactionForm
          transaction={editingTransaction}
          onClose={handleFormClose}
          onSuccess={handleFormSuccess}
        />
      )}
    </div>
  );
};