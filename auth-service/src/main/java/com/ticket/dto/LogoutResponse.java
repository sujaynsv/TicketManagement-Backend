package com.ticket.dto;

import java.time.LocalDateTime;

public record LogoutResponse(
        String message,
        String username,
        LocalDateTime loggedOutAt
) {}
