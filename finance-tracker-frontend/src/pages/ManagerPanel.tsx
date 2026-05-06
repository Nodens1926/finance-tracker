import React, { useState, useEffect } from 'react';
import { managerApi } from '../api/manager';
import { Card } from '../components/UI/Card';
import { Button } from '../components/UI/Button';
import { Transaction, User } from '../types';
import { useAuth } from '../context/AuthContext';
import { RoleGuard } from '../components/Auth/RoleGuard';

export const ManagerPanel: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [summary, setSummary] = useState<{ totalIncome: number; totalExpense: number; balance: number } | null>(null);
  const [analytics, setAnalytics] = useState<{ category: string; amount: number; percentage?: number }[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingUsers, setIsLoadingUsers] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { user, isAdmin } = useAuth();

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setIsLoadingUsers(true);
    setError(null);
    try {
      const data = await managerApi.getAllUsers();
      console.log('Загружены пользователи:', data);
      setUsers(data);
    } catch (error: any) {
      console.error('Ошибка загрузки пользователей:', error);
      setError(error.response?.data?.error || 'Ошибка загрузки пользователей');
    } finally {
      setIsLoadingUsers(false);
    }
  };

  const loadUserData = async (selectedUser: User) => {
    setIsLoading(true);
    setError(null);
    try {
      console.log('Загрузка данных для пользователя:', selectedUser.username);
      
      const [transactionsData, summaryData, analyticsData] = await Promise.all([
        managerApi.getUserTransactions(selectedUser.id),
        managerApi.getUserSummary(selectedUser.id),
        managerApi.getUserAnalytics(selectedUser.id)
      ]);
      
      console.log('Транзакции:', transactionsData);
      console.log('Summary:', summaryData);
      console.log('Analytics:', analyticsData);
      
      setTransactions(transactionsData || []);
      setSummary(summaryData);
      setAnalytics(analyticsData || []);
      setSelectedUser(selectedUser);
    } catch (error: any) {
      console.error('Ошибка загрузки данных пользователя:', error);
      setError(error.response?.data?.error || 'Ошибка загрузки данных');
    } finally {
      setIsLoading(false);
    }
  };

  const getTotalIncome = () => {
    return transactions
      .filter(t => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0);
  };

  const getTotalExpense = () => {
    return transactions
      .filter(t => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0);
  };

  const getBalance = () => {
    return getTotalIncome() - getTotalExpense();
  };

  return (
    <RoleGuard requiredRoles={['ROLE_MANAGER', 'ROLE_ADMIN']} fallback={
      <div className="p-6">
        <Card>
          <div className="text-center py-8">
            <div className="text-4xl mb-3">🔒</div>
            <p className="text-red-600 font-medium">У вас нет доступа к этой странице</p>
            <p className="text-gray-500 text-sm mt-2">Требуются права менеджера или администратора</p>
          </div>
        </Card>
      </div>
    }>
      <div className="p-6 space-y-6">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            👥 Панель управления
            {isAdmin && <span className="text-xs bg-purple-100 text-purple-700 px-2 py-1 rounded-full">ADMIN</span>}
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            {isAdmin 
              ? 'Полный доступ к данным всех пользователей' 
              : 'Просмотр транзакций и аналитики пользователей'}
          </p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <p className="text-red-600 text-sm">{error}</p>
            <Button variant="outline" size="sm" className="mt-2" onClick={loadUsers}>
              Повторить
            </Button>
          </div>
        )}
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Список пользователей */}
          <Card className="lg:col-span-1">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold">Пользователи</h2>
              <Button 
                variant="outline" 
                size="sm" 
                onClick={loadUsers}
                disabled={isLoadingUsers}
              >
                {isLoadingUsers ? '...' : '🔄'}
              </Button>
            </div>
            
            {isLoadingUsers ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
                <p className="text-xs text-gray-400 mt-2">Загрузка...</p>
              </div>
            ) : users.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <p>Нет пользователей</p>
              </div>
            ) : (
              <div className="space-y-2 max-h-[600px] overflow-y-auto">
                {users.map(u => (
                  <button
                    key={u.id}
                    onClick={() => loadUserData(u)}
                    className={`w-full text-left p-3 rounded-lg transition-all ${
                      selectedUser?.id === u.id 
                        ? 'bg-blue-50 border-l-4 border-blue-500 shadow-sm' 
                        : 'hover:bg-gray-50 border-l-4 border-transparent'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="font-medium text-gray-900">{u.username}</div>
                        <div className="text-sm text-gray-500">{u.email}</div>
                      </div>
                      {u.id === user?.id && (
                        <span className="text-xs bg-gray-100 px-2 py-0.5 rounded-full">Вы</span>
                      )}
                    </div>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {u.roles?.map(role => (
                        <span key={role} className={`text-xs px-2 py-0.5 rounded-full ${
                          role === 'ROLE_ADMIN' ? 'bg-purple-100 text-purple-700' :
                          role === 'ROLE_MANAGER' ? 'bg-blue-100 text-blue-700' :
                          'bg-gray-100 text-gray-600'
                        }`}>
                          {role.replace('ROLE_', '')}
                        </span>
                      ))}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </Card>

          {/* Данные выбранного пользователя */}
          <div className="lg:col-span-2 space-y-6">
            {!selectedUser ? (
              <Card>
                <div className="text-center py-12">
                  <div className="text-5xl mb-4">👈</div>
                  <p className="text-gray-500">Выберите пользователя из списка</p>
                  <p className="text-xs text-gray-400 mt-1">Чтобы просмотреть его транзакции</p>
                </div>
              </Card>
            ) : isLoading ? (
              <Card>
                <div className="text-center py-12">
                  <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600 mx-auto"></div>
                  <p className="text-gray-500 mt-4">Загрузка данных...</p>
                </div>
              </Card>
            ) : (
              <>
                {/* Информация о пользователе */}
                <Card>
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-xl font-bold">{selectedUser.username}</h2>
                      <p className="text-gray-500">{selectedUser.email}</p>
                    </div>
                    <div className="text-right">
                      <div className="text-sm text-gray-500">ID: {selectedUser.id}</div>
                      <div className="text-xs text-gray-400 mt-1">
                        Роли: {selectedUser.roles?.map(r => r.replace('ROLE_', '')).join(', ')}
                      </div>
                    </div>
                  </div>
                </Card>

                {/* Статистика пользователя */}
                <div className="grid grid-cols-3 gap-4">
                  <Card className="text-center">
                    <p className="text-sm text-gray-500">💰 Доходы</p>
                    <p className="text-xl font-bold text-green-600">
                      {getTotalIncome().toLocaleString()} ₽
                    </p>
                  </Card>
                  <Card className="text-center">
                    <p className="text-sm text-gray-500">💸 Расходы</p>
                    <p className="text-xl font-bold text-red-600">
                      {getTotalExpense().toLocaleString()} ₽
                    </p>
                  </Card>
                  <Card className="text-center">
                    <p className="text-sm text-gray-500">⚖️ Баланс</p>
                    <p className={`text-xl font-bold ${getBalance() >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {getBalance().toLocaleString()} ₽
                    </p>
                  </Card>
                </div>

                {/* Транзакции пользователя */}
                <Card>
                  <h3 className="text-lg font-semibold mb-4">📋 Транзакции</h3>
                  {transactions.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">
                      <p>У пользователя нет транзакций</p>
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                          <tr>
                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Дата</th>
                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Тип</th>
                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Категория</th>
                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Сумма</th>
                            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Описание</th>
                          </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                          {transactions.slice(0, 20).map(t => (
                            <tr key={t.id} className="hover:bg-gray-50">
                              <td className="px-4 py-3 text-sm">
                                {new Date(t.date).toLocaleDateString('ru-RU')}
                              </td>
                              <td className="px-4 py-3">
                                <span className={`px-2 py-1 rounded-full text-xs ${
                                  t.type === 'INCOME' 
                                    ? 'bg-green-100 text-green-800' 
                                    : 'bg-red-100 text-red-800'
                                }`}>
                                  {t.type === 'INCOME' ? 'Доход' : 'Расход'}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-sm">{t.category}</td>
                              <td className={`px-4 py-3 text-sm font-bold ${
                                t.type === 'INCOME' ? 'text-green-600' : 'text-red-600'
                              }`}>
                                {t.amount.toLocaleString()} ₽
                              </td>
                              <td className="px-4 py-3 text-sm max-w-xs truncate">
                                {t.description || '-'}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                      {transactions.length > 20 && (
                        <div className="text-center pt-4 text-sm text-gray-400">
                          Показаны первые 20 из {transactions.length} транзакций
                        </div>
                      )}
                    </div>
                  )}
                </Card>

                {/* Аналитика по категориям */}
                {analytics.length > 0 && (
                  <Card>
                    <h3 className="text-lg font-semibold mb-4">📊 Расходы по категориям</h3>
                    <div className="space-y-3">
                      {analytics.slice(0, 10).map(item => {
                        const totalExpenseAmount = analytics.reduce((sum, i) => sum + i.amount, 0);
                        const percentage = totalExpenseAmount > 0 ? (item.amount / totalExpenseAmount) * 100 : 0;
                        return (
                          <div key={item.category}>
                            <div className="flex justify-between text-sm mb-1">
                              <span>{item.category}</span>
                              <span className="font-semibold">{item.amount.toLocaleString()} ₽ ({percentage.toFixed(1)}%)</span>
                            </div>
                            <div className="w-full bg-gray-200 rounded-full h-2">
                              <div className="bg-red-500 h-2 rounded-full" style={{ width: `${percentage}%` }} />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </Card>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </RoleGuard>
  );
};