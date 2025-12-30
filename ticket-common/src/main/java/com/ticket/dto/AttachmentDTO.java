package com.ticket.dto;

import java.time.LocalDateTime;

public record AttachmentDTO(
    String attachmentId,
    String ticketId,
    String fileName,
    String originalFileName,
    String fileType,
    Long fileSize,
    String downloadUrl, // Pre-signed S3 URL
    String uploadedByUserId,
    String uploadedByUsername,
    LocalDateTime uploadedAt
) {}
