// api/attachments.ts
import api from '../utils/api';
import { Attachment, FileUploadResponse } from '../types';

export const attachmentsApi = {
  // Загрузка файла
  upload: async (
    transactionId: number, 
    file: File, 
    description?: string,
    onProgress?: (progress: number) => void
  ): Promise<FileUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (description) {
      formData.append('description', description);
    }

    const response = await api.post(`/transactions/${transactionId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percentCompleted);
        }
      },
    });
    return response.data;
  },

  // Получить все вложения транзакции
  getByTransaction: (transactionId: number): Promise<Attachment[]> =>
    api.get(`/transactions/${transactionId}/attachments`).then(res => res.data),

  // Получить URL для скачивания
  getDownloadUrl: (attachmentId: number): Promise<{ downloadUrl: string }> =>
    api.get(`/transactions/attachments/${attachmentId}/download`).then(res => res.data),

  // Удалить вложение
  delete: (attachmentId: number): Promise<void> =>
    api.delete(`/transactions/attachments/${attachmentId}`),
};