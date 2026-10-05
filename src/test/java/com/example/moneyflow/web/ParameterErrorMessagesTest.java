package com.example.moneyflow.web;

import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks that 400 messages name the parameter that actually failed. */
@WebMvcTest(ParameterErrorMessagesTest.ProbeController.class)
@Import(ParameterErrorMessagesTest.ProbeController.class)
class ParameterErrorMessagesTest {

    /** A test-only endpoint with parameters the real controller doesn't have. */
    @RestController
    static class ProbeController {

        @GetMapping("/probe")
        void probe(@RequestParam @Size(max = 3, message = "must be at most 3 characters") String code,
                   @RequestParam int count) {
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void namesTheParameterThatFailedValidation() throws Exception {
        mockMvc.perform(get("/probe").param("code", "abcd").param("count", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("code must be at most 3 characters"));
    }

    @Test
    void namesTheParameterWithTheWrongType() throws Exception {
        mockMvc.perform(get("/probe").param("code", "abc").param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("count has an invalid value"));
    }
}
