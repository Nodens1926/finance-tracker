// components/Transactions/FileUploader.tsx
import React, { useState, useRef } from 'react';
import { attachmentsApi } from '../../api/attachments';
import { Button } from '../UI/Button';
import { FileUploadResponse } from '../../types';

interface FileUploaderProps {
  transactionId: number;
  onUploadSuccess: (attachment: FileUploadResponse) => void;
  onUploadError?: (error: string) => void;
}

const ALLOWED_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
];
const MAX_SIZE = 10 * 1024 * 1024; // 10MB

export const FileUploader: React.FC<FileUploaderProps> = ({
  transactionId,
  onUploadSuccess,
  onUploadError,
}) => {
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): boolean => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      setError('Недопустимый тип файла. Разрешены: изображения, PDF, документы Word');
      return false;
    }
    
    if (file.size > MAX_SIZE) {
      setError('Файл слишком большой. Максимальный размер: 10MB');
      return false;
    }
    
    return true;
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && validateFile(file)) {
      setSelectedFile(file);
      setError(null);
    } else {
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Выберите файл');
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);
    setError(null);

    try {
      const response = await attachmentsApi.upload(
        transactionId,
        selectedFile,
        description || undefined,
        (progress) => setUploadProgress(progress)
      );
      
      onUploadSuccess(response);
      
      // Сброс формы
      setSelectedFile(null);
      setDescription('');
      setUploadProgress(0);
      if (fileInputRef.current) fileInputRef.current.value = '';
      
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || 'Ошибка загрузки файла';
      setError(errorMessage);
      if (onUploadError) onUploadError(errorMessage);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="border-2 border-dashed border-gray-300 rounded-lg p-6">
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Выберите файл
          </label>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*,.pdf,.doc,.docx"
            onChange={handleFileSelect}
            disabled={isUploading}
            className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
          />
          <p className="mt-1 text-xs text-gray-500">
            Допустимые форматы: изображения, PDF, Word (до 10MB)
          </p>
        </div>

        {selectedFile && (
          <div className="bg-gray-50 p-3 rounded">
            <p className="text-sm font-medium">{selectedFile.name}</p>
            <p className="text-xs text-gray-500">
              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
            </p>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Описание (опционально)
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Дополнительная информация о файле..."
            disabled={isUploading}
            rows={2}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {isUploading && (
          <div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Загрузка: {uploadProgress}%
            </p>
          </div>
        )}

        {error && (
          <div className="text-red-600 text-sm bg-red-50 p-3 rounded">
            {error}
          </div>
        )}

        <div className="flex justify-end">
          <Button
            onClick={handleUpload}
            disabled={!selectedFile || isUploading}
            variant="primary"
          >
            {isUploading ? 'Загрузка...' : 'Загрузить файл'}
          </Button>
        </div>
      </div>
    </div>
  );
};