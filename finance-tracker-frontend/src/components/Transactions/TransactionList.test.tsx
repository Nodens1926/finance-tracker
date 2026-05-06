import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TransactionList } from './TransactionList';

const mockTransactions = [
  { id: 1, amount: 100, type: 'INCOME' as const, category: 'Salary', date: '2024-01-01', description: 'Test 1' },
  { id: 2, amount: 50, type: 'EXPENSE' as const, category: 'Food', date: '2024-01-02', description: 'Test 2' },
];

describe('TransactionList', () => {
  const defaultProps = {
    transactions: mockTransactions,
    onEdit: vi.fn(),
    onDelete: vi.fn(),
  };

  it('renders transactions list', () => {
    render(<TransactionList {...defaultProps} />);
    
    expect(screen.getByText('Salary')).toBeInTheDocument();
    expect(screen.getByText('Food')).toBeInTheDocument();
    expect(screen.getByText('100 ₽')).toBeInTheDocument();
    expect(screen.getByText('50 ₽')).toBeInTheDocument();
  });

  it('shows empty state when no transactions', () => {
    render(<TransactionList {...defaultProps} transactions={[]} />);
    
    expect(screen.getByText('Нет транзакций. Добавьте первую транзакцию!')).toBeInTheDocument();
  });

  it('calls onEdit when edit button clicked', () => {
    render(<TransactionList {...defaultProps} />);
    
    const editButtons = screen.getAllByText('✏️ Изменить');
    fireEvent.click(editButtons[0]);
    
    expect(defaultProps.onEdit).toHaveBeenCalledWith(mockTransactions[0]);
  });

  it('shows confirmation dialog before delete', () => {
    render(<TransactionList {...defaultProps} />);
    
    const deleteButtons = screen.getAllByText('🗑️ Удалить');
    fireEvent.click(deleteButtons[0]);
    
    expect(screen.getByText('Подтверждение удаления')).toBeInTheDocument();
    expect(screen.getByText('Вы уверены, что хотите удалить эту транзакцию?')).toBeInTheDocument();
  });

  it('calls onDelete after confirmation', () => {
    render(<TransactionList {...defaultProps} />);
    
    const deleteButtons = screen.getAllByText('🗑️ Удалить');
    fireEvent.click(deleteButtons[0]);
    
    const confirmButton = screen.getByText('Удалить');
    fireEvent.click(confirmButton);
    
    expect(defaultProps.onDelete).toHaveBeenCalledWith(1);
  });

  it('closes confirmation modal when cancel clicked', () => {
    render(<TransactionList {...defaultProps} />);
    
    const deleteButtons = screen.getAllByText('🗑️ Удалить');
    fireEvent.click(deleteButtons[0]);
    
    expect(screen.getByText('Подтверждение удаления')).toBeInTheDocument();
    
    const cancelButton = screen.getByText('Отмена');
    fireEvent.click(cancelButton);
    
    expect(screen.queryByText('Подтверждение удаления')).not.toBeInTheDocument();
  });

  it('highlights selected transaction', () => {
    render(<TransactionList {...defaultProps} selectedTransactionId={1} onSelect={vi.fn()} />);
    
    const rows = document.querySelectorAll('tbody tr');
    expect(rows[0].className).toContain('bg-blue-50');
  });
});