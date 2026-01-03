package com.ticket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class S3StorageService {
    
    private S3Client s3Client;
    
    private S3Presigner s3Presigner;

    public S3StorageService( S3Client s3Client, S3Presigner s3Presigner){
        this.s3Client=s3Client;
        this.s3Presigner=s3Presigner;
    }
    
    /**
     * Custom exception for invalid file names.
     */
    class InvalidFileNameException extends RuntimeException {
        public InvalidFileNameException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for file size limit exceeded.
     */
    class FileSizeLimitExceededException extends RuntimeException {
        public FileSizeLimitExceededException(String message) {
            super(message);
        }
    }
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "image/jpg",
        "text/plain",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
    );
    
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    
    /**
     * Upload file to S3
     */
    public String uploadFile(String ticketId, MultipartFile file) throws IOException {
        // Validate file
        validateFile(file);
        
        // Generate unique file name
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new InvalidFileNameException("Invalid file name: file name is null or does not contain an extension.");
        }
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        String s3Key = "tickets/" + ticketId + "/" + uniqueFileName;
        
        // Upload to S3
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            return s3Key;
        } catch (S3Exception e) {
            throw new S3StorageException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Custom exception for S3 storage errors.
     */
    public static class S3StorageException extends RuntimeException {
        public S3StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Generate pre-signed URL for file download (valid for 10 minutes)
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .getObjectRequest(b -> b
                        .bucket(bucketName)
                        .key(s3Key)
                    )
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            throw new S3StorageException("Failed to generate pre-signed URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete file from S3
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new S3StorageException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate file type and size
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileNameException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeLimitExceededException("File size exceeds maximum limit of 10 MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType)) {
            throw new InvalidFileNameException("File type not allowed. Allowed types: PDF, PNG, JPG, TXT, DOCX, XLSX");
        }
    }
}
