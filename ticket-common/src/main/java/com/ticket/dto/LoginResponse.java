package com.ticket.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
	private String token;
	private String username;
	private String email;
	private String role;
	private String userId;
}
