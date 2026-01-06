package com.notification.service;

import com.notification.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class UserServiceTest {

    private UserService userService;
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setup() {
        userService = new UserService();

        // inject @Value field
        ReflectionTestUtils.setField(userService, "authServiceUrl", "http://auth-service"); // [web:567]

        // grab the internally-created RestTemplate and bind a mock server to it
        restTemplate = (RestTemplate) ReflectionTestUtils.getField(userService, "restTemplate");
        assertNotNull(restTemplate);

        mockServer = MockRestServiceServer.createServer(restTemplate); // [web:563]
    }

    @Test
    void getUserById_success_returnsOptionalUser() {
        String url = "http://auth-service/users/u1";

        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"userId\":\"u1\",\"username\":\"john\",\"email\":\"john@mail.com\"}",
                        MediaType.APPLICATION_JSON
                ));

        Optional<UserDTO> opt = userService.getUserById("u1");

        assertTrue(opt.isPresent());
        assertEquals("john@mail.com", opt.get().getEmail());

        mockServer.verify();
    }

    @Test
    void getUserById_serverError_returnsEmpty() {
        String url = "http://auth-service/users/u1";

        mockServer.expect(requestTo(url))
                .andRespond(withServerError());

        Optional<UserDTO> opt = userService.getUserById("u1");

        assertTrue(opt.isEmpty());
        mockServer.verify();
    }

    @Test
    void getUserEmail_success_mapsFromUser() {
        String url = "http://auth-service/users/u1";

        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"userId\":\"u1\",\"username\":\"john\",\"email\":\"john@mail.com\"}",
                        MediaType.APPLICATION_JSON
                ));

        Optional<String> emailOpt = userService.getUserEmail("u1");

        assertTrue(emailOpt.isPresent());
        assertEquals("john@mail.com", emailOpt.get());

        mockServer.verify();
    }

    @Test
    void getUserWithFallback_whenUserPresent_returnsRealUser() {
        String url = "http://auth-service/users/u1";

        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(
                        "{\"userId\":\"u1\",\"username\":\"real\",\"email\":\"real@mail.com\"}",
                        MediaType.APPLICATION_JSON
                ));

        UserDTO user = userService.getUserWithFallback("u1", "ignored");

        assertEquals("u1", user.getUserId());
        assertEquals("real", user.getUsername());
        assertEquals("real@mail.com", user.getEmail());

        mockServer.verify();
    }

    @Test
    void getUserWithFallback_whenServiceFails_returnsFallbackUser() {
        String url = "http://auth-service/users/u1";

        mockServer.expect(requestTo(url))
                .andRespond(withServerError());

        UserDTO user = userService.getUserWithFallback("u1", "fallbackName");

        assertEquals("u1", user.getUserId());
        assertEquals("fallbackName", user.getUsername());
        assertEquals("fallbackName@example.com", user.getEmail());

        mockServer.verify();
    }
}
