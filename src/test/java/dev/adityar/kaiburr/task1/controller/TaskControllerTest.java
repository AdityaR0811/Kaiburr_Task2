package dev.adityar.kaiburr.task1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adityar.kaiburr.task1.dto.TaskRequest;
import dev.adityar.kaiburr.task1.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for TaskController
 * Author: Aditya R.
 */
@WebMvcTest(TaskController.class)
@DisplayName("TaskController Tests")
class TaskControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TaskService taskService;
    
    @Test
    @DisplayName("Should return correlation ID in response header")
    void shouldReturnCorrelationIdInHeader() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .id("task1")
                .name("Test Task")
                .owner("testuser")
                .command("echo hello")
                .build();
        
        mockMvc.perform(put("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }
    
    @Test
    @DisplayName("Should validate request body")
    void shouldValidateRequestBody() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .id("")  // Invalid: empty ID
                .name("Test Task")
                .owner("testuser")
                .command("echo hello")
                .build();
        
        mockMvc.perform(put("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }
    
    @Test
    @DisplayName("Should accept pagination parameters")
    void shouldAcceptPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk());
    }
}
