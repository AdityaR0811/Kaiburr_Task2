package dev.adityar.kaiburr.task1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adityar.kaiburr.task1.domain.Task;
import dev.adityar.kaiburr.task1.domain.TaskExecution;
import dev.adityar.kaiburr.task1.dto.*;
import dev.adityar.kaiburr.task1.repo.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService
 * Author: Aditya R.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {
    
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private CommandValidator validator;
    
    @Mock
    private CommandRunner commandRunner;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private TaskService taskService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taskService, "auditLogFile", "test-audit.log.jsonl");
    }
    
    @Test
    @DisplayName("Should create new task successfully")
    void shouldCreateNewTask() {
        TaskRequest request = TaskRequest.builder()
                .id("task1")
                .name("Test Task")
                .owner("testuser")
                .command("echo hello")
                .build();
        
        CommandValidator.ValidationResult validationResult = 
                new CommandValidator.ValidationResult(true, List.of());
        
        when(validator.validate(anyString())).thenReturn(validationResult);
        when(taskRepository.findById("task1")).thenReturn(Optional.empty());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        TaskResponse response = taskService.upsertTask(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("task1");
        assertThat(response.getName()).isEqualTo("Test Task");
        assertThat(response.getOwner()).isEqualTo("testuser");
        assertThat(response.getCommand()).isEqualTo("echo hello");
        
        verify(taskRepository).save(any(Task.class));
    }
    
    @Test
    @DisplayName("Should update existing task")
    void shouldUpdateExistingTask() {
        Task existingTask = Task.builder()
                .id("task1")
                .name("Old Name")
                .owner("olduser")
                .command("echo old")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        
        TaskRequest request = TaskRequest.builder()
                .id("task1")
                .name("New Name")
                .owner("newuser")
                .command("echo new")
                .build();
        
        CommandValidator.ValidationResult validationResult = 
                new CommandValidator.ValidationResult(true, List.of());
        
        when(validator.validate(anyString())).thenReturn(validationResult);
        when(taskRepository.findById("task1")).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        TaskResponse response = taskService.upsertTask(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getOwner()).isEqualTo("newuser");
        assertThat(response.getCommand()).isEqualTo("echo new");
    }
    
    @Test
    @DisplayName("Should reject task with invalid command")
    void shouldRejectTaskWithInvalidCommand() {
        TaskRequest request = TaskRequest.builder()
                .id("task1")
                .name("Test Task")
                .owner("testuser")
                .command("rm -rf /")
                .build();
        
        CommandValidator.ValidationResult validationResult = 
                new CommandValidator.ValidationResult(false, List.of("Command contains denied token: rm"));
        
        when(validator.validate(anyString())).thenReturn(validationResult);
        
        assertThatThrownBy(() -> taskService.upsertTask(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Command validation failed");
        
        verify(taskRepository, never()).save(any(Task.class));
    }
    
    @Test
    @DisplayName("Should get all tasks with pagination")
    void shouldGetAllTasksWithPagination() {
        Task task1 = Task.builder().id("task1").name("Task 1").build();
        Task task2 = Task.builder().id("task2").name("Task 2").build();
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(Arrays.asList(task1, task2), pageable, 2);
        
        when(taskRepository.findAll(pageable)).thenReturn(taskPage);
        
        Page<TaskResponse> response = taskService.getAllTasks(pageable);
        
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Should get task by ID")
    void shouldGetTaskById() {
        Task task = Task.builder()
                .id("task1")
                .name("Test Task")
                .owner("testuser")
                .command("echo hello")
                .build();
        
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        
        TaskResponse response = taskService.getTaskById("task1");
        
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("task1");
        assertThat(response.getName()).isEqualTo("Test Task");
    }
    
    @Test
    @DisplayName("Should throw exception when task not found")
    void shouldThrowExceptionWhenTaskNotFound() {
        when(taskRepository.findById("nonexistent")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> taskService.getTaskById("nonexistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");
    }
    
    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTask() {
        when(taskRepository.existsById("task1")).thenReturn(true);
        
        taskService.deleteTask("task1");
        
        verify(taskRepository).deleteById("task1");
    }
    
    @Test
    @DisplayName("Should throw exception when deleting non-existent task")
    void shouldThrowExceptionWhenDeletingNonExistentTask() {
        when(taskRepository.existsById("nonexistent")).thenReturn(false);
        
        assertThatThrownBy(() -> taskService.deleteTask("nonexistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Task not found");
    }
    
    @Test
    @DisplayName("Should search tasks by name")
    void shouldSearchTasksByName() {
        Task task1 = Task.builder().id("task1").name("Test Task 1").build();
        Task task2 = Task.builder().id("task2").name("Test Task 2").build();
        
        when(taskRepository.findByNameContainingIgnoreCase("test"))
                .thenReturn(Arrays.asList(task1, task2));
        
        List<TaskResponse> response = taskService.searchTasksByName("test");
        
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).contains("Test");
        assertThat(response.get(1).getName()).contains("Test");
    }
    
    @Test
    @DisplayName("Should throw exception when no tasks found in search")
    void shouldThrowExceptionWhenNoTasksFoundInSearch() {
        when(taskRepository.findByNameContainingIgnoreCase("nonexistent"))
                .thenReturn(List.of());
        
        assertThatThrownBy(() -> taskService.searchTasksByName("nonexistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No tasks found");
    }
    
    @Test
    @DisplayName("Should execute task and record execution")
    void shouldExecuteTaskAndRecordExecution() throws Exception {
        Task task = Task.builder()
                .id("task1")
                .name("Test Task")
                .owner("testuser")
                .command("echo hello")
                .build();
        
        TaskExecution execution = TaskExecution.builder()
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(1))
                .durationMs(1000L)
                .exitCode(0)
                .stdout("hello\n")
                .stderr("")
                .correlationId("test-correlation-id")
                .build();
        
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(commandRunner.execute(anyString(), anyString())).thenReturn(execution);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        TaskExecutionResponse response = taskService.executeTask("task1");
        
        assertThat(response).isNotNull();
        assertThat(response.getExitCode()).isZero();
        assertThat(response.getStdout()).isEqualTo("hello\n");
        assertThat(response.getCorrelationId()).isEqualTo("test-correlation-id");
        
        verify(commandRunner).execute("echo hello", anyString());
        verify(taskRepository).save(any(Task.class));
    }
    
    @Test
    @DisplayName("Should validate command")
    void shouldValidateCommand() {
        CommandValidationRequest request = CommandValidationRequest.builder()
                .command("echo hello")
                .build();
        
        CommandValidator.ValidationResult validationResult = 
                new CommandValidator.ValidationResult(true, List.of());
        
        when(validator.validate("echo hello")).thenReturn(validationResult);
        
        CommandValidationResponse response = taskService.validateCommand(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getValid()).isTrue();
        assertThat(response.getCommand()).isEqualTo("echo hello");
        assertThat(response.getMessage()).isEqualTo("Command is valid");
    }
    
    @Test
    @DisplayName("Should return validation failures")
    void shouldReturnValidationFailures() {
        CommandValidationRequest request = CommandValidationRequest.builder()
                .command("rm -rf /")
                .build();
        
        CommandValidator.ValidationResult validationResult = 
                new CommandValidator.ValidationResult(false, 
                        Arrays.asList("Command contains denied token: rm", "Binary 'rm' is not in allowlist"));
        
        when(validator.validate("rm -rf /")).thenReturn(validationResult);
        
        CommandValidationResponse response = taskService.validateCommand(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getValid()).isFalse();
        assertThat(response.getViolations()).hasSize(2);
        assertThat(response.getMessage()).isEqualTo("Command validation failed");
    }
}
