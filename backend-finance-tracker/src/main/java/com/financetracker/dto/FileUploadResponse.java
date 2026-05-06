package com.financetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long attachmentId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl; // Pre-signed URL
    private String message;
}