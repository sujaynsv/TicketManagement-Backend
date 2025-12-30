package com.ticket.controller;

import com.ticket.dto.CommentDTO;
import com.ticket.dto.CreateCommentRequest;
import com.ticket.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    /**
     * Add comment to ticket
     */
    @PostMapping
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable String ticketId,
            @Valid @RequestBody CreateCommentRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) {
        CommentDTO comment = commentService.addComment(ticketId, request, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }
    
    /**
     * Get all comments for ticket
     * Query param: includeInternal (true for staff, false for customers)
     */
    @GetMapping
    public ResponseEntity<List<CommentDTO>> getComments(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "false") boolean includeInternal) {
        List<CommentDTO> comments = commentService.getCommentsByTicket(ticketId, includeInternal);
        return ResponseEntity.ok(comments);
    }
}
