package com.ticket.dto;

import jakarta.validation.constraints.Email;

public record UpdateUserRequest(
        @Email(message = "Email must be valid")
        String email,
        String firstName,
        String lastName
) {}
