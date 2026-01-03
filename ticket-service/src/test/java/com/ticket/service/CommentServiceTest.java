package com.ticket.service;

import com.ticket.dto.CommentDTO;
import com.ticket.dto.CreateCommentRequest;
import com.ticket.entity.Comment;
import com.ticket.entity.Ticket;
import com.ticket.event.CommentAddedEvent;
import com.ticket.repository.CommentRepository;
import com.ticket.repository.TicketRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private EventPublisherService eventPublisher;

    private Ticket testTicket;
    private Comment testComment;
    private LocalDateTime now;
    private String ticketId = "TKT-001";
    private String userId = "user-001";
    private String username = "testuser";

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Setup test ticket
        testTicket = new Ticket();
        testTicket.setTicketId(ticketId);
        testTicket.setTicketNumber("TKT-20240101-00001");
        testTicket.setTitle("Test Ticket");

        // Setup test comment
        testComment = new Comment();
        testComment.setCommentId("CMT-001");
        testComment.setTicketId(ticketId);
        testComment.setUserId(userId);
        testComment.setUsername(username);
        testComment.setCommentText("Test comment");
        testComment.setIsInternal(false);
        testComment.setCreatedAt(now);
        testComment.setUpdatedAt(now);
    }

    // ==================== ADD COMMENT TESTS ====================

    @Test
    void testAddComment_WithValidRequest_Success() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Test comment", false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        CommentDTO result = commentService.addComment(ticketId, request, userId, username);

        // Assert
        assertNotNull(result);
        assertEquals("Test comment", result.commentText());
        assertEquals(userId, result.userId());
        assertEquals(username, result.username());
        assertFalse(result.isInternal());
        verify(ticketRepository, times(1)).findById(ticketId);
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(ticketService, times(1)).incrementCommentCount(ticketId);
        verify(eventPublisher, times(1)).publishCommentAdded(any(CommentAddedEvent.class));
    }

    @Test
    void testAddComment_WithInternalComment_Success() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Internal comment", true);
        testComment.setIsInternal(true);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        CommentDTO result = commentService.addComment(ticketId, request, userId, username);

        // Assert
        assertNotNull(result);
        assertTrue(result.isInternal());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void testAddComment_WithNullIsInternal_DefaultsToFalse() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Comment", null);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

        // Act
        commentService.addComment(ticketId, request, userId, username);

        // Assert
        verify(commentRepository).save(commentCaptor.capture());
        Comment savedComment = commentCaptor.getValue();
        assertFalse(savedComment.getIsInternal());
    }

    @Test
    void testAddComment_WithInvalidTicket_ThrowsException() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Comment", false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            commentService.addComment(ticketId, request, userId, username));
        verify(commentRepository, never()).save(any(Comment.class));
        verify(ticketService, never()).incrementCommentCount(anyString());
        verify(eventPublisher, never()).publishCommentAdded(any(CommentAddedEvent.class));
    }

    @Test
    void testAddComment_CreatesCommentWithCorrectFields() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("New comment text", false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

        // Act
        commentService.addComment(ticketId, request, userId, username);

        // Assert
        verify(commentRepository).save(commentCaptor.capture());
        Comment savedComment = commentCaptor.getValue();
        assertEquals(ticketId, savedComment.getTicketId());
        assertEquals(userId, savedComment.getUserId());
        assertEquals(username, savedComment.getUsername());
        assertEquals("New comment text", savedComment.getCommentText());
        assertNotNull(savedComment.getCreatedAt());
        assertNotNull(savedComment.getUpdatedAt());
    }

    @Test
    void testAddComment_IncrementsCommentCount() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Comment", false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        commentService.addComment(ticketId, request, userId, username);

        // Assert
        verify(ticketService, times(1)).incrementCommentCount(ticketId);
    }

    @Test
    void testAddComment_PublishesEvent() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Test comment", false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        ArgumentCaptor<CommentAddedEvent> eventCaptor = ArgumentCaptor.forClass(CommentAddedEvent.class);

        // Act
        commentService.addComment(ticketId, request, userId, username);

        // Assert
        verify(eventPublisher).publishCommentAdded(eventCaptor.capture());
        CommentAddedEvent event = eventCaptor.getValue();
        assertEquals("CMT-001", event.getCommentId());
        assertEquals(ticketId, event.getTicketId());
        assertEquals("TKT-20240101-00001", event.getTicketNumber());
        assertEquals(userId, event.getUserId());
        assertEquals(username, event.getUsername());
        assertEquals("Test comment", event.getCommentText()); // Now matches request text
    }


    // ==================== GET COMMENTS TESTS ====================

    @Test
    void testGetCommentsByTicket_IncludeInternal_ReturnsAllComments() {
        // Arrange
        Comment internalComment = new Comment();
        internalComment.setCommentId("CMT-002");
        internalComment.setTicketId(ticketId);
        internalComment.setCommentText("Internal comment");
        internalComment.setIsInternal(true);
        internalComment.setUserId(userId);
        internalComment.setUsername(username);
        internalComment.setCreatedAt(now);
        internalComment.setUpdatedAt(now);

        List<Comment> comments = List.of(testComment, internalComment);
        when(commentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)).thenReturn(comments);

        // Act
        List<CommentDTO> result = commentService.getCommentsByTicket(ticketId, true);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(commentRepository, times(1)).findByTicketIdOrderByCreatedAtDesc(ticketId);
        verify(commentRepository, never()).findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(anyString());
    }

    @Test
    void testGetCommentsByTicket_ExcludeInternal_ReturnsPublicCommentsOnly() {
        // Arrange
        List<Comment> publicComments = List.of(testComment);
        when(commentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(ticketId))
                .thenReturn(publicComments);

        // Act
        List<CommentDTO> result = commentService.getCommentsByTicket(ticketId, false);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isInternal());
        verify(commentRepository, times(1)).findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(ticketId);
        verify(commentRepository, never()).findByTicketIdOrderByCreatedAtDesc(anyString());
    }

    @Test
    void testGetCommentsByTicket_WithNoComments_ReturnsEmptyList() {
        // Arrange
        when(commentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(ticketId))
                .thenReturn(List.of());

        // Act
        List<CommentDTO> result = commentService.getCommentsByTicket(ticketId, false);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testGetCommentsByTicket_OrderedByCreatedAtDesc() {
        // Arrange
        Comment comment1 = new Comment();
        comment1.setCommentId("CMT-001");
        comment1.setTicketId(ticketId);
        comment1.setCommentText("First comment");
        comment1.setIsInternal(false);
        comment1.setCreatedAt(now.minusHours(2));

        Comment comment2 = new Comment();
        comment2.setCommentId("CMT-002");
        comment2.setTicketId(ticketId);
        comment2.setCommentText("Second comment");
        comment2.setIsInternal(false);
        comment2.setCreatedAt(now.minusHours(1));

        Comment comment3 = new Comment();
        comment3.setCommentId("CMT-003");
        comment3.setTicketId(ticketId);
        comment3.setCommentText("Third comment");
        comment3.setIsInternal(false);
        comment3.setCreatedAt(now);

        // Repository returns newest first
        List<Comment> comments = List.of(comment3, comment2, comment1);
        when(commentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtDesc(ticketId))
                .thenReturn(comments);

        // Act
        List<CommentDTO> result = commentService.getCommentsByTicket(ticketId, false);

        // Assert
        assertEquals(3, result.size());
        assertEquals("Third comment", result.get(0).commentText());
        assertEquals("Second comment", result.get(1).commentText());
        assertEquals("First comment", result.get(2).commentText());
    }

    // ==================== DTO CONVERSION TESTS ====================

    @Test
    void testConvertToDTO_MapsAllFields() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest("Test", false);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        CommentDTO result = commentService.addComment(ticketId, request, userId, username);

        // Assert
        assertEquals("CMT-001", result.commentId());
        assertEquals(ticketId, result.ticketId());
        assertEquals(userId, result.userId());
        assertEquals(username, result.username());
        assertEquals("Test comment", result.commentText());
        assertFalse(result.isInternal());
        assertNotNull(result.createdAt());
        assertNotNull(result.updatedAt());
    }

    // ==================== EDGE CASES ====================

    @Test
    void testAddComment_WithLongCommentText_Success() {
        // Arrange
        String longText = "A".repeat(1000);
        CreateCommentRequest request = new CreateCommentRequest(longText, false);
        testComment.setCommentText(longText);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        CommentDTO result = commentService.addComment(ticketId, request, userId, username);

        // Assert
        assertNotNull(result);
        assertEquals(1000, result.commentText().length());
    }

    @Test
    void testAddComment_MultipleComments_AllSucceed() {
        // Arrange
        CreateCommentRequest request1 = new CreateCommentRequest("Comment 1", false);
        CreateCommentRequest request2 = new CreateCommentRequest("Comment 2", true);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(testTicket));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        commentService.addComment(ticketId, request1, userId, username);
        commentService.addComment(ticketId, request2, userId, username);

        // Assert
        verify(commentRepository, times(2)).save(any(Comment.class));
        verify(ticketService, times(2)).incrementCommentCount(ticketId);
        verify(eventPublisher, times(2)).publishCommentAdded(any(CommentAddedEvent.class));
    }
}
