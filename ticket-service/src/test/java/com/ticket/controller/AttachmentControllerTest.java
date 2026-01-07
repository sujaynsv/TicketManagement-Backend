package com.ticket.controller;

import com.ticket.dto.AttachmentDTO;
import com.ticket.service.AttachmentService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.config.fail-fast=false"
})
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    private static final String BASE = "/tickets/{ticketId}/attachments";

    @Test
    @DisplayName("POST /upload -> 201 when upload succeeds")
    void uploadAttachment_success_created() throws Exception {
        String ticketId = "T1";
        String userId = "U1";
        String username = "rishi";

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        AttachmentDTO dto = Mockito.mock(AttachmentDTO.class);
        Mockito.when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenReturn(dto);

        mockMvc.perform(
                        multipart(BASE + "/upload", ticketId)
                                .file(file)
                                .header("X-User-Id", userId)
                                .header("X-Username", username)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Mockito.verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }

    @Test
    @DisplayName("POST /upload -> 400 when service throws IOException (your controller catches it)")
    void uploadAttachment_ioException_badRequest() throws Exception {
        String ticketId = "T1";
        String userId = "U1";
        String username = "rishi";

        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        Mockito.when(attachmentService.uploadAttachment(eq(ticketId), any(), eq(userId), eq(username)))
                .thenThrow(new IOException("boom"));

        mockMvc.perform(
                        multipart(BASE + "/upload", ticketId)
                                .file(file)
                                .header("X-User-Id", userId)
                                .header("X-Username", username)
                )
                .andExpect(status().isBadRequest());

        Mockito.verify(attachmentService, times(1))
                .uploadAttachment(eq(ticketId), any(), eq(userId), eq(username));
    }

    @Test
    @DisplayName("POST /upload -> 500 when file part missing (matches your current error response)")
    void uploadAttachment_missingFile_returns500() throws Exception {
        mockMvc.perform(
                        multipart(BASE + "/upload", "T1")
                                .header("X-User-Id", "U1")
                                .header("X-Username", "rishi")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", Matchers.containsString("Required part 'file' is not present")));

        Mockito.verify(attachmentService, never())
                .uploadAttachment(anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /upload -> 500 when X-User-Id missing (matches your current error response)")
    void uploadAttachment_missingUserIdHeader_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                        multipart(BASE + "/upload", "T1")
                                .file(file)
                                .header("X-Username", "rishi")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", Matchers.containsString("Required request header 'X-User-Id'")));

        Mockito.verify(attachmentService, never())
                .uploadAttachment(anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /upload -> 500 when X-Username missing (matches your current error response)")
    void uploadAttachment_missingUsernameHeader_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.txt", MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(
                        multipart(BASE + "/upload", "T1")
                                .file(file)
                                .header("X-User-Id", "U1")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", Matchers.containsString("Required request header 'X-Username'")));

        Mockito.verify(attachmentService, never())
                .uploadAttachment(anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /attachments -> 200 and returns list")
    void getAttachments_success_ok() throws Exception {
        String ticketId = "T1";
        AttachmentDTO dto = Mockito.mock(AttachmentDTO.class);

        Mockito.when(attachmentService.getAttachmentsByTicket(ticketId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get(BASE, ticketId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        Mockito.verify(attachmentService, times(1))
                .getAttachmentsByTicket(ticketId);
    }

    @Test
    @DisplayName("GET /{attachmentId}/download -> 500 when X-User-Id missing (matches your current error response)")
    void downloadAttachment_missingUserIdHeader_returns500() throws Exception {
        mockMvc.perform(get(BASE + "/{attachmentId}/download", "T1", "A1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", Matchers.containsString("Required request header 'X-User-Id'")));

        Mockito.verify(attachmentService, never())
                .canUserAccessTicket(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /{attachmentId}/download -> 403 when access denied")
    void downloadAttachment_accessDenied_forbidden() throws Exception {
        String ticketId = "T1";
        String attachmentId = "A1";
        String userId = "U1";

        Mockito.when(attachmentService.canUserAccessTicket(ticketId, userId))
                .thenReturn(false);

        mockMvc.perform(
                        get(BASE + "/{attachmentId}/download", ticketId, attachmentId)
                                .header("X-User-Id", userId)
                )
                .andExpect(status().isForbidden());

        Mockito.verify(attachmentService, times(1))
                .canUserAccessTicket(ticketId, userId);

        Mockito.verify(attachmentService, never())
                .downloadAttachment(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /{attachmentId}/download -> 200 when allowed (proxy service response)")
    void downloadAttachment_allowed_ok() throws Exception {
        String ticketId = "T1";
        String attachmentId = "A1";
        String userId = "U1";

        byte[] data = "filedata".getBytes(StandardCharsets.UTF_8);

        Mockito.when(attachmentService.canUserAccessTicket(ticketId, userId))
                .thenReturn(true);

        Mockito.when(attachmentService.downloadAttachment(ticketId, attachmentId, userId))
                .thenReturn(
                        ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                                .body(data)
                );

        mockMvc.perform(
                        get(BASE + "/{attachmentId}/download", ticketId, attachmentId)
                                .header("X-User-Id", userId)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(content().bytes(data));

        Mockito.verify(attachmentService, times(1))
                .downloadAttachment(ticketId, attachmentId, userId);
    }

    @Test
    @DisplayName("GET /download/all -> 500 when X-User-Id missing (matches your current error response)")
    void downloadAllAttachments_missingUserIdHeader_returns500() throws Exception {
        mockMvc.perform(get(BASE + "/download/all", "T1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", Matchers.containsString("Required request header 'X-User-Id'")));

        Mockito.verify(attachmentService, never())
                .canUserAccessTicket(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /download/all -> 403 when access denied")
    void downloadAllAttachments_accessDenied_forbidden() throws Exception {
        String ticketId = "T1";
        String userId = "U1";

        Mockito.when(attachmentService.canUserAccessTicket(ticketId, userId))
                .thenReturn(false);

        mockMvc.perform(
                        get(BASE + "/download/all", ticketId)
                                .header("X-User-Id", userId)
                )
                .andExpect(status().isForbidden());

        Mockito.verify(attachmentService, never())
                .downloadAllAttachments(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /download/all -> 200 when allowed (proxy service response)")
    void downloadAllAttachments_allowed_ok() throws Exception {
        String ticketId = "T1";
        String userId = "U1";

        byte[] zip = new byte[]{1, 2, 3, 4};

        Mockito.when(attachmentService.canUserAccessTicket(ticketId, userId))
                .thenReturn(true);

        Mockito.when(attachmentService.downloadAllAttachments(ticketId, userId))
                .thenReturn(
                        ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                                .body(zip)
                );

        mockMvc.perform(
                        get(BASE + "/download/all", ticketId)
                                .header("X-User-Id", userId)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                .andExpect(content().bytes(zip));

        Mockito.verify(attachmentService, times(1))
                .downloadAllAttachments(ticketId, userId);
    }

    @Test
    @DisplayName("DELETE /{attachmentId} -> 200 when service succeeds")
    void deleteAttachment_success_ok() throws Exception {
        String ticketId = "T1";
        String attachmentId = "A1";

        mockMvc.perform(delete(BASE + "/{attachmentId}", ticketId, attachmentId))
                .andExpect(status().isOk());

        Mockito.verify(attachmentService, times(1))
                .deleteAttachment(attachmentId, ticketId);
    }

    @Test
    @DisplayName("DELETE /{attachmentId} -> 500 when service throws")
    void deleteAttachment_serviceThrows_500() throws Exception {
        String ticketId = "T1";
        String attachmentId = "A1";

        Mockito.doThrow(new RuntimeException("boom"))
                .when(attachmentService).deleteAttachment(attachmentId, ticketId);

        mockMvc.perform(delete(BASE + "/{attachmentId}", ticketId, attachmentId))
                .andExpect(status().isInternalServerError());

        Mockito.verify(attachmentService, times(1))
                .deleteAttachment(attachmentId, ticketId);
    }
}