package com.ticket.controller;

import com.ticket.dto.ErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // Standalone setup + explicitly attach the @RestControllerAdvice [web:817]
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --------- tests ---------

    @Test
    void handleRuntimeException_returns400_andErrorResponse() throws Exception {
        mockMvc.perform(post("/__test/runtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Operation Failed"))
                .andExpect(jsonPath("$.message").value("boom"))
                .andExpect(jsonPath("$.path").value("/__test/runtime"));
    }

    @Test
    void handleGlobalException_returns500_andErrorResponse() throws Exception {
        mockMvc.perform(post("/__test/exception")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("checked"))
                .andExpect(jsonPath("$.path").value("/__test/exception"));
    }

    @Test
    void handleValidationException_returns400_andErrorResponse() throws Exception {
        // Body violates @NotBlank -> MethodArgumentNotValidException [web:819]
        mockMvc.perform(post("/__test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                // message contains "text:" (field name) [your handler formats "field: message"]
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("text")))
                .andExpect(jsonPath("$.path").value("/__test/validate"));
    }

    // --------- dummy controller + dto just for testing advice ---------

    @RestController
    static class DummyController {

        @PostMapping("/__test/runtime")
        public ErrorResponse runtime() {
            throw new RuntimeException("boom");
        }

        @PostMapping("/__test/exception")
        public ErrorResponse checked() throws Exception {
            throw new Exception("checked");
        }

        @PostMapping("/__test/validate")
        public String validate(@Valid @RequestBody DummyRequest req) {
            return "ok";
        }
    }

    static class DummyRequest {
        @NotBlank(message = "must not be blank")
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
