package com.ticket.client;

import com.ticket.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service",
    url = "${services.auth-service.url:http://localhost:8081}"
)
public interface UserServiceClient {
    
    @GetMapping("/users/{userId}")
    UserDTO getUserById(@PathVariable("userId") String userId);
}
