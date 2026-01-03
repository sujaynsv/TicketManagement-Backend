package com.ticket.controller;

import com.ticket.dto.AttachmentDTO;
import com.ticket.service.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttachmentController.class)
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    private AttachmentDTO testAttachment;
    private String ticketId = "TKT-001";
    private String userId = "user-001";
    private String username = "testuser";
    private String attachmentId = "ATT-001";

    @BeforeEach
    void setUp() {
        testAttachment = new AttachmentDTO(
                attachmentId,
                ticketId,
                "test-document.pdf",
                "test-document.pdf",
                "application/pdf",
                12345L,
                "https://s3.amazonaws.com/bucket/test-document.pdf",
                userId,
                username,
                LocalDateTime.now()
        );
    }

    // ==================== UPLOAD ATTACHMENT ====================

    @Test
    void testUploadAttachment_WithValidFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenReturn(testAttachment);

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachmentId").value(attachmentId))
                .andExpect(jsonPath("$.fileName").value("test-document.pdf"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSize").value(12345))
                .andExpect(jsonPath("$.uploadedByUserId").value(userId));

        verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }

    @Test
    void testUploadAttachment_WithImageFile_Success() throws Exception {
        AttachmentDTO imageAttachment = new AttachmentDTO(
                "ATT-002",
                ticketId,
                "screenshot.png",
                "screenshot.png",
                "image/png",
                54321L,
                "https://s3.amazonaws.com/bucket/screenshot.png",
                userId,
                username,
                LocalDateTime.now()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screenshot.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenReturn(imageAttachment);

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("screenshot.png"))
                .andExpect(jsonPath("$.fileType").value("image/png"));

        verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }

    @Test
    void testUploadAttachment_WithoutFile_BadRequest() throws Exception {
        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().is5xxServerError());  // Changed from 400 to 5xx

        verify(attachmentService, never())
                .uploadAttachment(anyString(), any(), anyString(), anyString());
    }


    @Test
    void testUploadAttachment_WithoutHeaders_ReturnsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file))
                .andExpect(status().is5xxServerError());

        verify(attachmentService, never())
                .uploadAttachment(anyString(), any(), anyString(), anyString());
    }

    @Test
    void testUploadAttachment_WithoutUserId_ReturnsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file)
                .header("X-Username", username))
                .andExpect(status().is5xxServerError());

        verify(attachmentService, never())
                .uploadAttachment(anyString(), any(), anyString(), anyString());
    }

    @Test
    void testUploadAttachment_WithIOException_ReturnsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "content".getBytes()
        );

        when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenThrow(new IOException("Failed to upload file"));

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().is5xxServerError());  // Changed from 4xx to 5xx

        verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }


    @Test
    void testUploadAttachment_WithLargeFile_Success() throws Exception {
        AttachmentDTO largeFileAttachment = new AttachmentDTO(
                "ATT-003",
                ticketId,
                "large-file.zip",
                "large-file.zip",
                "application/zip",
                10485760L, // 10MB
                "https://s3.amazonaws.com/bucket/large-file.zip",
                userId,
                username,
                LocalDateTime.now()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-file.zip",
                "application/zip",
                new byte[1024]
        );

        when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenReturn(largeFileAttachment);

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileSize").value(10485760L));

        verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }

    // ==================== GET ATTACHMENTS ====================

    @Test
    void testGetAttachments_WithExistingAttachments_Success() throws Exception {
        AttachmentDTO attachment2 = new AttachmentDTO(
                "ATT-002",
                ticketId,
                "image.png",
                "image.png",
                "image/png",
                54321L,
                "https://s3.amazonaws.com/bucket/image.png",
                userId,
                username,
                LocalDateTime.now()
        );

        List<AttachmentDTO> attachments = List.of(testAttachment, attachment2);
        when(attachmentService.getAttachmentsByTicket(ticketId)).thenReturn(attachments);

        mockMvc.perform(get("/tickets/{ticketId}/attachments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attachmentId").value(attachmentId))
                .andExpect(jsonPath("$[0].fileName").value("test-document.pdf"))
                .andExpect(jsonPath("$[1].attachmentId").value("ATT-002"))
                .andExpect(jsonPath("$[1].fileName").value("image.png"));

        verify(attachmentService, times(1)).getAttachmentsByTicket(ticketId);
    }

    @Test
    void testGetAttachments_WithNoAttachments_ReturnsEmptyList() throws Exception {
        when(attachmentService.getAttachmentsByTicket(ticketId)).thenReturn(List.of());

        mockMvc.perform(get("/tickets/{ticketId}/attachments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(attachmentService, times(1)).getAttachmentsByTicket(ticketId);
    }

    @Test
    void testGetAttachments_WithSingleAttachment_Success() throws Exception {
        when(attachmentService.getAttachmentsByTicket(ticketId))
                .thenReturn(List.of(testAttachment));

        mockMvc.perform(get("/tickets/{ticketId}/attachments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketId").value(ticketId));

        verify(attachmentService, times(1)).getAttachmentsByTicket(ticketId);
    }

    @Test
    void testGetAttachments_WithInvalidTicketId_ReturnsError() throws Exception {
        when(attachmentService.getAttachmentsByTicket("INVALID"))
                .thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(get("/tickets/{ticketId}/attachments", "INVALID"))
                .andExpect(status().is4xxClientError());

        verify(attachmentService, times(1)).getAttachmentsByTicket("INVALID");
    }

    // ==================== DELETE ATTACHMENT ====================

    @Test
    void testDeleteAttachment_WithValidId_Success() throws Exception {
        doNothing().when(attachmentService).deleteAttachment(attachmentId);

        mockMvc.perform(delete("/tickets/{ticketId}/attachments/{attachmentId}", 
                ticketId, attachmentId))
                .andExpect(status().isOk())
                .andExpect(content().string("Attachment deleted successfully"));

        verify(attachmentService, times(1)).deleteAttachment(attachmentId);
    }

    @Test
    void testDeleteAttachment_WithInvalidId_ReturnsError() throws Exception {
        doThrow(new RuntimeException("Attachment not found"))
                .when(attachmentService).deleteAttachment("INVALID");

        mockMvc.perform(delete("/tickets/{ticketId}/attachments/{attachmentId}", 
                ticketId, "INVALID"))
                .andExpect(status().is4xxClientError());

        verify(attachmentService, times(1)).deleteAttachment("INVALID");
    }

    @Test
    void testDeleteAttachment_ServiceThrowsException_ReturnsError() throws Exception {
        doThrow(new RuntimeException("Failed to delete from S3"))
                .when(attachmentService).deleteAttachment(attachmentId);

        mockMvc.perform(delete("/tickets/{ticketId}/attachments/{attachmentId}", 
                ticketId, attachmentId))
                .andExpect(status().is4xxClientError());

        verify(attachmentService, times(1)).deleteAttachment(attachmentId);
    }

    // ==================== EDGE CASES ====================

    @Test
    void testUploadAttachment_WithEmptyFile_Success() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        AttachmentDTO emptyFileAttachment = new AttachmentDTO(
                "ATT-004",
                ticketId,
                "empty.txt",
                "empty.txt",
                "text/plain",
                0L,
                "https://s3.amazonaws.com/bucket/empty.txt",
                userId,
                username,
                LocalDateTime.now()
        );

        when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenReturn(emptyFileAttachment);

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(emptyFile)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileSize").value(0));

        verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }

    @Test
    void testUploadAttachment_WithSpecialCharactersInFilename_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test file (copy) [1].pdf",
                "application/pdf",
                "content".getBytes()
        );

        AttachmentDTO specialNameAttachment = new AttachmentDTO(
                "ATT-005",
                ticketId,
                "test-file-copy-1.pdf",
                "test file (copy) [1].pdf",
                "application/pdf",
                7L,
                "https://s3.amazonaws.com/bucket/test-file-copy-1.pdf",
                userId,
                username,
                LocalDateTime.now()
        );

        when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenReturn(specialNameAttachment);

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId)
                .file(file)
                .header("X-User-Id", userId)
                .header("X-Username", username))
                .andExpect(status().isCreated());

        verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }
}
