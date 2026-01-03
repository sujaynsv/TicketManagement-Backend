package com.ticket.service;

import com.ticket.dto.AttachmentDTO;
import com.ticket.entity.Attachment;
import com.ticket.entity.Ticket;
import com.ticket.repository.AttachmentRepository;
import com.ticket.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTest {

    @InjectMocks
    private AttachmentService attachmentService;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private TicketService ticketService;

    @Mock
    private MultipartFile mockFile;

    private Attachment testAttachment;
    private Ticket testTicket;
    private LocalDateTime now;
    private String userId = "user-001";
    private String username = "testuser";
    private String ticketId = "TKT-001";

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Setup test ticket
        testTicket = new Ticket();
        testTicket.setTicketId(ticketId);
        testTicket.setAttachmentCount(0);

        // Setup test attachment
        testAttachment = new Attachment();
        testAttachment.setAttachmentId("ATT-001");
        testAttachment.setTicketId(ticketId);
        testAttachment.setFileName("test-file.pdf");
        testAttachment.setOriginalFileName("test-file.pdf");
        testAttachment.setFileType("application/pdf");
        testAttachment.setFileSize(1024L);
        testAttachment.setS3Key("tickets/TKT-001/test-file.pdf");
        testAttachment.setS3Url("https://s3.amazonaws.com/bucket/test-file.pdf");
        testAttachment.setUploadedByUserId(userId);
        testAttachment.setUploadedByUsername(username);
        testAttachment.setUploadedAt(now);
    }

    // ==================== UPLOAD ATTACHMENT TESTS ====================

    @Test
    void testUploadAttachment_WithValidFile_Success() throws IOException {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(mockFile.getOriginalFilename()).thenReturn("test-file.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(s3StorageService.uploadFile(eq(ticketId), any(MultipartFile.class)))
                .thenReturn("tickets/TKT-001/test-file.pdf");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);
        when(s3StorageService.generatePresignedUrl(anyString()))
                .thenReturn("https://presigned-url.com/test-file.pdf");

        // Act
        AttachmentDTO result = attachmentService.uploadAttachment(ticketId, mockFile, userId, username);

        // Assert
        assertNotNull(result);
        assertEquals("test-file.pdf", result.fileName());
        assertEquals("application/pdf", result.fileType());
        assertEquals(1024L, result.fileSize());
        verify(ticketRepository, times(1)).findById(ticketId);
        verify(s3StorageService, times(1)).uploadFile(eq(ticketId), any(MultipartFile.class));
        verify(attachmentRepository, times(1)).save(any(Attachment.class));
        verify(ticketService, times(1)).incrementAttachmentCount(ticketId);
        verify(s3StorageService, times(1)).generatePresignedUrl(anyString());
    }

    @Test
    void testUploadAttachment_WithInvalidTicket_ThrowsException() throws IOException {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            attachmentService.uploadAttachment(ticketId, mockFile, userId, username));
        verify(s3StorageService, never()).uploadFile(anyString(), any(MultipartFile.class));
        verify(attachmentRepository, never()).save(any(Attachment.class));
    }

    @Test
    void testUploadAttachment_CreatesAttachmentWithCorrectFields() throws IOException {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(mockFile.getOriginalFilename()).thenReturn("document.docx");
        when(mockFile.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(mockFile.getSize()).thenReturn(2048L);
        when(s3StorageService.uploadFile(eq(ticketId), any(MultipartFile.class)))
                .thenReturn("tickets/TKT-001/document.docx");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("https://presigned-url.com");

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);

        // Act
        attachmentService.uploadAttachment(ticketId, mockFile, userId, username);

        // Assert
        verify(attachmentRepository).save(attachmentCaptor.capture());
        Attachment savedAttachment = attachmentCaptor.getValue();
        assertEquals(ticketId, savedAttachment.getTicketId());
        assertEquals("document.docx", savedAttachment.getFileName());
        assertEquals("document.docx", savedAttachment.getOriginalFileName());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                savedAttachment.getFileType());
        assertEquals(2048L, savedAttachment.getFileSize());
        assertEquals(userId, savedAttachment.getUploadedByUserId());
        assertEquals(username, savedAttachment.getUploadedByUsername());
        assertNotNull(savedAttachment.getUploadedAt());
    }

    @Test
    void testUploadAttachment_IncrementsTicketAttachmentCount() throws IOException {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getContentType()).thenReturn("text/plain");
        when(mockFile.getSize()).thenReturn(512L);
        when(s3StorageService.uploadFile(anyString(), any(MultipartFile.class)))
                .thenReturn("tickets/TKT-001/test.txt");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("https://url.com");

        // Act
        attachmentService.uploadAttachment(ticketId, mockFile, userId, username);

        // Assert
        verify(ticketService, times(1)).incrementAttachmentCount(ticketId);
    }

    @Test
    void testUploadAttachment_S3UploadFails_ThrowsException() throws IOException {
        // Arrange
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(s3StorageService.uploadFile(eq(ticketId), any(MultipartFile.class)))
                .thenThrow(new IOException("S3 upload failed"));

        // Act & Assert
        assertThrows(IOException.class, () -> 
            attachmentService.uploadAttachment(ticketId, mockFile, userId, username));
        verify(attachmentRepository, never()).save(any(Attachment.class));
        verify(ticketService, never()).incrementAttachmentCount(anyString());
    }


    // ==================== GET ATTACHMENTS TESTS ====================

    @Test
    void testGetAttachmentsByTicket_WithMultipleAttachments_Success() {
        // Arrange
        Attachment attachment1 = new Attachment();
        attachment1.setAttachmentId("ATT-001");
        attachment1.setTicketId(ticketId);
        attachment1.setFileName("file1.pdf");
        attachment1.setS3Key("tickets/TKT-001/file1.pdf");
        attachment1.setFileType("application/pdf");
        attachment1.setFileSize(1024L);
        attachment1.setUploadedByUserId(userId);
        attachment1.setUploadedByUsername(username);
        attachment1.setUploadedAt(now);

        Attachment attachment2 = new Attachment();
        attachment2.setAttachmentId("ATT-002");
        attachment2.setTicketId(ticketId);
        attachment2.setFileName("file2.jpg");
        attachment2.setS3Key("tickets/TKT-001/file2.jpg");
        attachment2.setFileType("image/jpeg");
        attachment2.setFileSize(2048L);
        attachment2.setUploadedByUserId(userId);
        attachment2.setUploadedByUsername(username);
        attachment2.setUploadedAt(now.plusMinutes(5));

        List<Attachment> attachments = List.of(attachment1, attachment2);
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(attachments);
        when(s3StorageService.generatePresignedUrl(anyString()))
                .thenReturn("https://presigned-url.com");

        // Act
        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("file1.pdf", result.get(0).fileName());
        assertEquals("file2.jpg", result.get(1).fileName());
        verify(attachmentRepository, times(1)).findByTicketIdOrderByUploadedAtDesc(ticketId);
        verify(s3StorageService, times(2)).generatePresignedUrl(anyString());
    }

    @Test
    void testGetAttachmentsByTicket_WithNoAttachments_ReturnsEmptyList() {
        // Arrange
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(List.of());

        // Act
        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(attachmentRepository, times(1)).findByTicketIdOrderByUploadedAtDesc(ticketId);
        verify(s3StorageService, never()).generatePresignedUrl(anyString());
    }

    @Test
    void testGetAttachmentsByTicket_GeneratesFreshPresignedUrls() {
        // Arrange
        List<Attachment> attachments = List.of(testAttachment);
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(attachments);
        when(s3StorageService.generatePresignedUrl("tickets/TKT-001/test-file.pdf"))
                .thenReturn("https://fresh-presigned-url.com/test-file.pdf");

        // Act
        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("https://fresh-presigned-url.com/test-file.pdf", result.get(0).downloadUrl());
        verify(s3StorageService, times(1)).generatePresignedUrl("tickets/TKT-001/test-file.pdf");
    }

    @Test
    void testGetAttachmentsByTicket_OrderedByUploadedAtDesc() {
        // Arrange
        Attachment oldAttachment = new Attachment();
        oldAttachment.setAttachmentId("ATT-001");
        oldAttachment.setTicketId(ticketId);
        oldAttachment.setFileName("old.pdf");
        oldAttachment.setS3Key("tickets/TKT-001/old.pdf");
        oldAttachment.setUploadedAt(now.minusHours(2));

        Attachment newAttachment = new Attachment();
        newAttachment.setAttachmentId("ATT-002");
        newAttachment.setTicketId(ticketId);
        newAttachment.setFileName("new.pdf");
        newAttachment.setS3Key("tickets/TKT-001/new.pdf");
        newAttachment.setUploadedAt(now);

        // Repository should return in descending order (newest first)
        List<Attachment> attachments = List.of(newAttachment, oldAttachment);
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(attachments);
        when(s3StorageService.generatePresignedUrl(anyString()))
                .thenReturn("https://presigned-url.com");

        // Act
        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        // Assert
        assertEquals(2, result.size());
        assertEquals("new.pdf", result.get(0).fileName());
        assertEquals("old.pdf", result.get(1).fileName());
    }

    // ==================== DELETE ATTACHMENT TESTS ====================

    @Test
    void testDeleteAttachment_WithValidId_Success() {
        // Arrange
        when(attachmentRepository.findById("ATT-001")).thenReturn(Optional.of(testAttachment));

        // Act
        attachmentService.deleteAttachment("ATT-001");

        // Assert
        verify(attachmentRepository, times(1)).findById("ATT-001");
        verify(s3StorageService, times(1)).deleteFile("tickets/TKT-001/test-file.pdf");
        verify(attachmentRepository, times(1)).deleteById("ATT-001");
    }

    @Test
    void testDeleteAttachment_WithInvalidId_ThrowsException() {
        // Arrange
        when(attachmentRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            attachmentService.deleteAttachment("INVALID"));
        verify(s3StorageService, never()).deleteFile(anyString());
        verify(attachmentRepository, never()).deleteById(anyString());
    }

    @Test
    void testDeleteAttachment_DeletesFromS3First() {
        // Arrange
        when(attachmentRepository.findById("ATT-001")).thenReturn(Optional.of(testAttachment));

        // Act
        attachmentService.deleteAttachment("ATT-001");

        // Assert - Verify order: S3 delete happens before DB delete
        var inOrder = inOrder(s3StorageService, attachmentRepository);
        inOrder.verify(s3StorageService).deleteFile("tickets/TKT-001/test-file.pdf");
        inOrder.verify(attachmentRepository).deleteById("ATT-001");
    }

    @Test
    void testDeleteAttachment_S3DeleteFails_ThrowsException() {
        // Arrange
        when(attachmentRepository.findById("ATT-001")).thenReturn(Optional.of(testAttachment));
        doThrow(new RuntimeException("S3 delete failed"))
                .when(s3StorageService).deleteFile(anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            attachmentService.deleteAttachment("ATT-001"));
        verify(attachmentRepository, never()).deleteById(anyString());
    }

    // ==================== DTO CONVERSION TESTS ====================

    @Test
    void testConvertToDTO_MapsAllFields() {
        // Arrange
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(List.of(testAttachment));
        when(s3StorageService.generatePresignedUrl(anyString()))
                .thenReturn("https://presigned-url.com");

        // Act
        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        // Assert
        assertEquals(1, result.size());
        AttachmentDTO dto = result.get(0);
        assertEquals("ATT-001", dto.attachmentId());
        assertEquals(ticketId, dto.ticketId());
        assertEquals("test-file.pdf", dto.fileName());
        assertEquals("test-file.pdf", dto.originalFileName());
        assertEquals("application/pdf", dto.fileType());
        assertEquals(1024L, dto.fileSize());
        assertEquals(userId, dto.uploadedByUserId());
        assertEquals(username, dto.uploadedByUsername());
        assertNotNull(dto.uploadedAt());
    }

    // ==================== EDGE CASES ====================

    @Test
    void testUploadAttachment_WithLargeFile_Success() throws IOException {
        // Arrange
        long largeFileSize = 100 * 1024 * 1024; // 100 MB
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(mockFile.getOriginalFilename()).thenReturn("large-file.zip");
        when(mockFile.getContentType()).thenReturn("application/zip");
        when(mockFile.getSize()).thenReturn(largeFileSize);
        when(s3StorageService.uploadFile(eq(ticketId), any(MultipartFile.class)))
                .thenReturn("tickets/TKT-001/large-file.zip");
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("https://url.com");

        // Act
        AttachmentDTO result = attachmentService.uploadAttachment(ticketId, mockFile, userId, username);

        // Assert
        assertNotNull(result);
        verify(s3StorageService, times(1)).uploadFile(eq(ticketId), any(MultipartFile.class));
    }

    @Test
    void testUploadAttachment_WithSpecialCharactersInFilename_Success() throws IOException {
        // Arrange
        String specialFileName = "test file (1) [copy].pdf";
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(mockFile.getOriginalFilename()).thenReturn(specialFileName);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(s3StorageService.uploadFile(eq(ticketId), any(MultipartFile.class)))
                .thenReturn("tickets/TKT-001/" + specialFileName);
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("https://url.com");

        // Act
        AttachmentDTO result = attachmentService.uploadAttachment(ticketId, mockFile, userId, username);

        // Assert
        assertNotNull(result);
        verify(mockFile, times(2)).getOriginalFilename(); // Called twice for fileName and originalFileName
    }

    @Test
    void testGetAttachmentsByTicket_WithDifferentFileTypes_Success() {
        // Arrange
        Attachment pdfAttachment = new Attachment();
        pdfAttachment.setAttachmentId("ATT-001");
        pdfAttachment.setTicketId(ticketId);
        pdfAttachment.setFileName("document.pdf");
        pdfAttachment.setFileType("application/pdf");
        pdfAttachment.setS3Key("key1");

        Attachment imageAttachment = new Attachment();
        imageAttachment.setAttachmentId("ATT-002");
        imageAttachment.setTicketId(ticketId);
        imageAttachment.setFileName("screenshot.png");
        imageAttachment.setFileType("image/png");
        imageAttachment.setS3Key("key2");

        Attachment textAttachment = new Attachment();
        textAttachment.setAttachmentId("ATT-003");
        textAttachment.setTicketId(ticketId);
        textAttachment.setFileName("notes.txt");
        textAttachment.setFileType("text/plain");
        textAttachment.setS3Key("key3");

        List<Attachment> attachments = List.of(pdfAttachment, imageAttachment, textAttachment);
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(attachments);
        when(s3StorageService.generatePresignedUrl(anyString()))
                .thenReturn("https://presigned-url.com");

        // Act
        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        // Assert
        assertEquals(3, result.size());
        assertEquals("application/pdf", result.get(0).fileType());
        assertEquals("image/png", result.get(1).fileType());
        assertEquals("text/plain", result.get(2).fileType());
    }
}
