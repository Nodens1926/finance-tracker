// components/Transactions/AttachmentList.tsx
import React, { useState } from 'react';
import { Attachment } from '../../types';
import { attachmentsApi } from '../../api/attachments';
import { Button } from '../UI/Button';

interface AttachmentListProps {
  transactionId: number;
  attachments: Attachment[];
  onAttachmentDeleted: (attachmentId: number) => void;
  canDelete?: boolean;
}

const getFileIcon = (fileType: string) => {
  if (fileType.startsWith('image/')) return '🖼️';
  if (fileType === 'application/pdf') return '📄';
  if (fileType.includes('word')) return '📝';
  return '📎';
};

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

export const AttachmentList: React.FC<AttachmentListProps> = ({
  transactionId,
  attachments,
  onAttachmentDeleted,
  canDelete = true,
}) => {
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const handleDownload = async (attachment: Attachment) => {
    setDownloadingId(attachment.id);
    try {
      const url = attachment.downloadUrl;
      window.open(url, '_blank');
    } catch (error) {
      console.error('Ошибка скачивания:', error);
    } finally {
      setDownloadingId(null);
    }
  };

  const handleDelete = async (attachmentId: number) => {
    if (!confirm('Удалить этот файл?')) return;
    
    setDeletingId(attachmentId);
    try {
      await attachmentsApi.delete(attachmentId);
      onAttachmentDeleted(attachmentId);
    } catch (error) {
      console.error('Ошибка удаления:', error);
    } finally {
      setDeletingId(null);
    }
  };

  if (attachments.length === 0) {
    return (
      <div className="text-center text-gray-500 py-4">
        Нет прикрепленных файлов
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <h4 className="font-medium text-gray-700">Прикрепленные файлы:</h4>
      <div className="space-y-2">
        {attachments.map((attachment) => (
          <div
            key={attachment.id}
            className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <div className="flex items-center space-x-3 flex-1">
              <span className="text-2xl">{getFileIcon(attachment.fileType)}</span>
              <div className="flex-1 min-w-0">
                <p className="font-medium text-gray-900 truncate">
                  {attachment.fileName}
                </p>
                <div className="flex items-center space-x-2 text-xs text-gray-500">
                  <span>{formatFileSize(attachment.fileSize)}</span>
                  {attachment.description && (
                    <>
                      <span>•</span>
                      <span className="truncate">{attachment.description}</span>
                    </>
                  )}
                  {attachment.uploadedAt && (
                    <>
                      <span>•</span>
                      <span>
                        {new Date(attachment.uploadedAt).toLocaleDateString()}
                      </span>
                    </>
                  )}
                </div>
              </div>
            </div>
            <div className="flex space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleDownload(attachment)}
                disabled={downloadingId === attachment.id}
              >
                {downloadingId === attachment.id ? '...' : '📥'}
              </Button>
              {canDelete && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleDelete(attachment.id)}
                  disabled={deletingId === attachment.id}
                  className="text-red-600 hover:text-red-700"
                >
                  {deletingId === attachment.id ? '...' : '🗑️'}
                </Button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};