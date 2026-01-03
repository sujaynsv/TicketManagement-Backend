package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.CommentDTO;
import com.ticket.dto.CreateCommentRequest;
import com.ticket.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    private CommentDTO testComment;
    private CreateCommentRequest createRequest;
    private String ticketId = "TKT-001";
    private String userId = "user-001";
    private String username = "testuser";
    private String commentId = "CMT-001";

    @BeforeEach
    void setUp() {
        testComment = new CommentDTO(
                commentId,
                ticketId,
                userId,
                username,
                "This is a test comment",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        createRequest = new CreateCommentRequest("This is a test comment", false);
    }

    // ==================== ADD COMMENT ====================

    @Test
    void testAddComment_WithValidRequest_Success() throws Exception {
        when(commentService.addComment(eq(ticketId), any(CreateCommentRequest.class), eq(userId), eq(username)))
                .thenReturn(testComment);

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(commentId))
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.commentText").value("This is a test comment"))
                .andExpect(jsonPath("$.isInternal").value(false));

        verify(commentService, times(1))
                .addComment(eq(ticketId), any(CreateCommentRequest.class), eq(userId), eq(username));
    }

    @Test
    void testAddComment_WithInternalComment_Success() throws Exception {
        CreateCommentRequest internalRequest = new CreateCommentRequest("Internal note", true);
        CommentDTO internalComment = new CommentDTO(
                "CMT-002",
                ticketId,
                userId,
                username,
                "Internal note",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(commentService.addComment(eq(ticketId), any(CreateCommentRequest.class), eq(userId), eq(username)))
                .thenReturn(internalComment);

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(internalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentText").value("Internal note"))
                .andExpect(jsonPath("$.isInternal").value(true));

        verify(commentService, times(1))
                .addComment(eq(ticketId), any(CreateCommentRequest.class), eq(userId), eq(username));
    }

    @Test
    void testAddComment_WithLongText_Success() throws Exception {
        String longText = "A".repeat(500);
        CreateCommentRequest longRequest = new CreateCommentRequest(longText, false);
        CommentDTO longComment = new CommentDTO(
                "CMT-003",
                ticketId,
                userId,
                username,
                longText,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(commentService.addComment(eq(ticketId), any(CreateCommentRequest.class), eq(userId), eq(username)))
                .thenReturn(longComment);

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentText").value(longText));

        verify(commentService, times(1))
                .addComment(eq(ticketId), any(CreateCommentRequest.class), eq(userId), eq(username));
    }

    @Test
    void testAddComment_WithoutHeaders_ReturnsError() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is5xxServerError());

        verify(commentService, never())
                .addComment(anyString(), any(), anyString(), anyString());
    }

    @Test
    void testAddComment_WithoutUserId_ReturnsError() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .header("X-Username", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is5xxServerError());

        verify(commentService, never())
                .addComment(anyString(), any(), anyString(), anyString());
    }

    @Test
    void testAddComment_WithoutUsername_ReturnsError() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is5xxServerError());

        verify(commentService, never())
                .addComment(anyString(), any(), anyString(), anyString());
    }

    @Test
    void testAddComment_WithInvalidTicket_ReturnsError() throws Exception {
        when(commentService.addComment(eq("INVALID"), any(CreateCommentRequest.class), eq(userId), eq(username)))
                .thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(post("/tickets/{ticketId}/comments", "INVALID")
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is4xxClientError());

        verify(commentService, times(1))
                .addComment(eq("INVALID"), any(CreateCommentRequest.class), eq(userId), eq(username));
    }

    @Test
    void testAddComment_WithEmptyBody_ReturnsError() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(commentService, never())
                .addComment(anyString(), any(), anyString(), anyString());
    }

    // ==================== GET COMMENTS ====================

    @Test
    void testGetComments_WithIncludeInternalFalse_Success() throws Exception {
        CommentDTO publicComment1 = new CommentDTO(
                "CMT-001",
                ticketId,
                userId,
                username,
                "Public comment 1",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CommentDTO publicComment2 = new CommentDTO(
                "CMT-002",
                ticketId,
                "user-002",
                "otheruser",
                "Public comment 2",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(commentService.getCommentsByTicket(ticketId, false))
                .thenReturn(List.of(publicComment1, publicComment2));

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId)
                .param("includeInternal", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].commentId").value("CMT-001"))
                .andExpect(jsonPath("$[0].isInternal").value(false))
                .andExpect(jsonPath("$[1].commentId").value("CMT-002"))
                .andExpect(jsonPath("$[1].isInternal").value(false));

        verify(commentService, times(1)).getCommentsByTicket(ticketId, false);
    }

    @Test
    void testGetComments_WithIncludeInternalTrue_Success() throws Exception {
        CommentDTO publicComment = new CommentDTO(
                "CMT-001",
                ticketId,
                userId,
                username,
                "Public comment",
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CommentDTO internalComment = new CommentDTO(
                "CMT-002",
                ticketId,
                "agent-001",
                "agent",
                "Internal note",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(commentService.getCommentsByTicket(ticketId, true))
                .thenReturn(List.of(publicComment, internalComment));

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId)
                .param("includeInternal", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isInternal").value(false))
                .andExpect(jsonPath("$[1].isInternal").value(true));

        verify(commentService, times(1)).getCommentsByTicket(ticketId, true);
    }

    @Test
    void testGetComments_WithDefaultParam_Success() throws Exception {
        when(commentService.getCommentsByTicket(ticketId, false))
                .thenReturn(List.of(testComment));

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(commentService, times(1)).getCommentsByTicket(ticketId, false);
    }

    @Test
    void testGetComments_WithNoComments_ReturnsEmptyList() throws Exception {
        when(commentService.getCommentsByTicket(ticketId, false)).thenReturn(List.of());

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commentService, times(1)).getCommentsByTicket(ticketId, false);
    }

    @Test
    void testGetComments_WithInvalidTicket_ReturnsError() throws Exception {
        when(commentService.getCommentsByTicket("INVALID", false))
                .thenThrow(new RuntimeException("Ticket not found"));

        mockMvc.perform(get("/tickets/{ticketId}/comments", "INVALID"))
                .andExpect(status().is4xxClientError());

        verify(commentService, times(1)).getCommentsByTicket("INVALID", false);
    }

    @Test
    void testGetComments_WithMultipleComments_Success() throws Exception {
        List<CommentDTO> comments = List.of(
                new CommentDTO("CMT-001", ticketId, userId, username, "Comment 1", false, LocalDateTime.now(), LocalDateTime.now()),
                new CommentDTO("CMT-002", ticketId, userId, username, "Comment 2", false, LocalDateTime.now(), LocalDateTime.now()),
                new CommentDTO("CMT-003", ticketId, userId, username, "Comment 3", false, LocalDateTime.now(), LocalDateTime.now())
        );

        when(commentService.getCommentsByTicket(ticketId, false)).thenReturn(comments);

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].commentText").value("Comment 1"))
                .andExpect(jsonPath("$[1].commentText").value("Comment 2"))
                .andExpect(jsonPath("$[2].commentText").value("Comment 3"));

        verify(commentService, times(1)).getCommentsByTicket(ticketId, false);
    }
}
