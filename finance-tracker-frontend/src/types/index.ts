// types/index.ts

export interface User {
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
  logoutAllDevices?: boolean;
}

export interface Transaction {
  id: number;
  amount: number;
  type: 'INCOME' | 'EXPENSE';
  category: string;
  date: string;
  description?: string;
}

// НОВЫЙ ТИП: Для пагинированного ответа
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// НОВЫЙ ТИП: Для параметров фильтрации
export interface TransactionFilterParams {
  type?: string;
  category?: string;
  minAmount?: number;
  maxAmount?: number;
  startDate?: string;
  endDate?: string;
  search?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

// НОВЫЙ ТИП: Для вложений
export interface Attachment {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  downloadUrl: string;
  description?: string;
  uploadedAt: string;
}

// НОВЫЙ ТИП: Для ответа при загрузке файла
export interface FileUploadResponse {
  attachmentId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  downloadUrl: string;
  message: string;
}

export interface CategoryExpense {
  category: string;
  amount: number;
  percentage?: number;
}

export interface DashboardSummary {
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

export type UserRole = 'ROLE_USER' | 'ROLE_MANAGER' | 'ROLE_ADMIN';