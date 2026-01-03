package com.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3StorageServiceTest {

    @InjectMocks
    private S3StorageService s3StorageService;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private String bucketName = "test-bucket";
    private String ticketId = "TKT-001";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3StorageService, "bucketName", bucketName);
    }

    // ==================== UPLOAD FILE TESTS ====================

    @Test
    void testUploadFile_WithValidPDF_Success() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(5 * 1024 * 1024L); // 5 MB
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("document.pdf");
        when(mockFile.getBytes()).thenReturn(new byte[100]);

        // Act
        String result = s3StorageService.uploadFile(ticketId, mockFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("tickets/TKT-001/"));
        assertTrue(result.endsWith(".pdf"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadFile_WithValidImage_Success() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(2 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn("image/png");
        when(mockFile.getOriginalFilename()).thenReturn("screenshot.png");
        when(mockFile.getBytes()).thenReturn(new byte[100]);

        // Act
        String result = s3StorageService.uploadFile(ticketId, mockFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("tickets/TKT-001/"));
        assertTrue(result.endsWith(".png"));
    }

    @Test
    void testUploadFile_WithEmptyFile_ThrowsException() {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.uploadFile(ticketId, mockFile));
        assertEquals("File is empty", exception.getMessage());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadFile_WithOversizedFile_ThrowsException() {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(15 * 1024 * 1024L); // 15 MB (exceeds 10 MB limit)
        // Remove: when(mockFile.getContentType()).thenReturn("application/pdf");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.uploadFile(ticketId, mockFile));
        assertTrue(exception.getMessage().contains("File size exceeds maximum limit"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadFile_WithInvalidFileType_ThrowsException() {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn("application/zip"); // Not allowed

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.uploadFile(ticketId, mockFile));
        assertTrue(exception.getMessage().contains("File type not allowed"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadFile_WithNullContentType_ThrowsException() {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.uploadFile(ticketId, mockFile));
        assertTrue(exception.getMessage().contains("File type not allowed"));
    }

    @Test
    void testUploadFile_S3Exception_ThrowsRuntimeException() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getBytes()).thenReturn(new byte[100]);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.uploadFile(ticketId, mockFile));
        assertTrue(exception.getMessage().contains("Failed to upload file to S3"));
    }

    // ==================== GENERATE PRESIGNED URL TESTS ====================

    @Test
    void testGeneratePresignedUrl_Success() throws Exception {
        // Arrange
        String s3Key = "tickets/TKT-001/file.pdf";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/presigned-url";
        
        when(presignedGetObjectRequest.url()).thenReturn(new URL(expectedUrl));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);

        // Act
        String result = s3StorageService.generatePresignedUrl(s3Key);

        // Assert
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void testGeneratePresignedUrl_S3Exception_ThrowsRuntimeException() {
        // Arrange
        String s3Key = "tickets/TKT-001/file.pdf";
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(S3Exception.builder().message("Presign error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.generatePresignedUrl(s3Key));
        assertTrue(exception.getMessage().contains("Failed to generate pre-signed URL"));
    }

    // ==================== DELETE FILE TESTS ====================

    @Test
    void testDeleteFile_Success() {
        // Arrange
        String s3Key = "tickets/TKT-001/file.pdf";

        // Act
        s3StorageService.deleteFile(s3Key);

        // Assert
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFile_S3Exception_ThrowsRuntimeException() {
        // Arrange
        String s3Key = "tickets/TKT-001/file.pdf";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Delete error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            s3StorageService.deleteFile(s3Key));
        assertTrue(exception.getMessage().contains("Failed to delete file from S3"));
    }

    // ==================== FILE VALIDATION TESTS ====================

    @Test
    void testUploadFile_WithAllAllowedFileTypes_Success() throws IOException {
        // Test each allowed file type
        String[] allowedTypes = {
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        };

        for (String contentType : allowedTypes) {
            // Arrange
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L);
            when(mockFile.getContentType()).thenReturn(contentType);
            when(mockFile.getOriginalFilename()).thenReturn("test.file");
            when(mockFile.getBytes()).thenReturn(new byte[100]);

            // Act
            String result = s3StorageService.uploadFile(ticketId, mockFile);

            // Assert
            assertNotNull(result);
        }

        // Verify s3Client was called for each file type
        verify(s3Client, times(allowedTypes.length))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadFile_GeneratesUniqueKeys() throws IOException {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("document.pdf");
        when(mockFile.getBytes()).thenReturn(new byte[100]);

        // Act
        String result1 = s3StorageService.uploadFile(ticketId, mockFile);
        String result2 = s3StorageService.uploadFile(ticketId, mockFile);

        // Assert - Each upload should generate unique key
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1, result2); // Keys should be different due to UUID
    }

    @Test
    void testUploadFile_AtMaxSizeLimit_Success() throws IOException {
        // Arrange - exactly 10 MB
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(10 * 1024 * 1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getOriginalFilename()).thenReturn("large.pdf");
        when(mockFile.getBytes()).thenReturn(new byte[100]);

        // Act
        String result = s3StorageService.uploadFile(ticketId, mockFile);

        // Assert
        assertNotNull(result);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
