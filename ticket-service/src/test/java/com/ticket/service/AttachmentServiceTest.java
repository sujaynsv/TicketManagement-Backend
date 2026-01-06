package com.ticket.service;

import com.ticket.dto.AttachmentDTO;
import com.ticket.entity.Attachment;
import com.ticket.exception.TicketNotFoundException;
import com.ticket.repository.AttachmentRepository;
import com.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

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
    private String userId = "user-001";
    private String username = "testuser";
    private String ticketId = "TKT-001";

    @BeforeEach
    void setUp() {
        testAttachment = new Attachment();
        testAttachment.setAttachmentId("ATT-001");
        testAttachment.setTicketId(ticketId);
        testAttachment.setFileName("test-file.pdf");
        testAttachment.setOriginalFileName("test-file.pdf");
        testAttachment.setFileType("application/pdf");
        testAttachment.setFileSize(1024L);
        testAttachment.setS3Key("tickets/TKT-001/test-file.pdf");
        testAttachment.setUploadedByUserId(userId);
        testAttachment.setUploadedByUsername(username);
        testAttachment.setUploadedAt(LocalDateTime.now());
    }

    @Test
    void testUploadAttachment_WithValidFile_Success() throws Exception {
        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        when(mockFile.getOriginalFilename()).thenReturn("test-file.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("https://presigned-url.com");

        AttachmentDTO result = attachmentService.uploadAttachment(ticketId, mockFile, userId, username);

        assertNotNull(result);
        verify(attachmentRepository, times(1)).save(any(Attachment.class));
    }

    @Test
    void testUploadAttachment_WithInvalidTicket_ThrowsException() {
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        assertThrows(TicketNotFoundException.class, () ->
                attachmentService.uploadAttachment(ticketId, mockFile, userId, username));
    }

    @Test
    void testGetAttachmentsByTicket_WithMultipleAttachments_Success() {
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(List.of(testAttachment));
        when(s3StorageService.generatePresignedUrl(anyString())).thenReturn("https://presigned-url.com");

        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAttachmentsByTicket_WithNoAttachments_ReturnsEmptyList() {
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId))
                .thenReturn(List.of());

        List<AttachmentDTO> result = attachmentService.getAttachmentsByTicket(ticketId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }
}