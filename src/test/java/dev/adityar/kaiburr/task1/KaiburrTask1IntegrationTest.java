package dev.adityar.kaiburr.task1;

import dev.adityar.kaiburr.task1.domain.Task;
import dev.adityar.kaiburr.task1.dto.TaskRequest;
import dev.adityar.kaiburr.task1.dto.TaskResponse;
import dev.adityar.kaiburr.task1.repo.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests with Testcontainers MongoDB
 * Author: Aditya R.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Integration Tests")
class KaiburrTask1IntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TaskRepository taskRepository;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        taskRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should create task successfully")
    void shouldCreateTask() {
        TaskRequest request = TaskRequest.builder()
                .id("task1")
                .name("Integration Test Task")
                .owner("testuser")
                .command("echo hello")
                .build();
        
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(
                baseUrl + "/tasks",
                request,
                TaskResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("task1");
        assertThat(response.getBody().getName()).isEqualTo("Integration Test Task");
        
        // Verify in database
        Task savedTask = taskRepository.findById("task1").orElse(null);
        assertThat(savedTask).isNotNull();
        assertThat(savedTask.getName()).isEqualTo("Integration Test Task");
    }
    
    @Test
    @DisplayName("Should get task by ID")
    void shouldGetTaskById() {
        // Create task first
        Task task = Task.builder()
                .id("task2")
                .name("Get Test Task")
                .owner("testuser")
                .command("echo test")
                .build();
        taskRepository.save(task);
        
        // Get task
        ResponseEntity<TaskResponse> response = restTemplate.getForEntity(
                baseUrl + "/tasks/task2",
                TaskResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("task2");
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent task")
    void shouldReturn404ForNonExistentTask() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/tasks/nonexistent",
                String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTask() {
        // Create task first
        Task task = Task.builder()
                .id("task3")
                .name("Delete Test Task")
                .owner("testuser")
                .command("echo delete")
                .build();
        taskRepository.save(task);
        
        // Delete task
        restTemplate.delete(baseUrl + "/tasks/task3");
        
        // Verify deletion
        assertThat(taskRepository.existsById("task3")).isFalse();
    }
    
    @Test
    @DisplayName("Should search tasks by name")
    void shouldSearchTasksByName() {
        // Create tasks
        taskRepository.save(Task.builder().id("t1").name("Search Task One").owner("user1").command("echo 1").build());
        taskRepository.save(Task.builder().id("t2").name("Search Task Two").owner("user2").command("echo 2").build());
        taskRepository.save(Task.builder().id("t3").name("Different Name").owner("user3").command("echo 3").build());
        
        // Search
        ResponseEntity<TaskResponse[]> response = restTemplate.getForEntity(
                baseUrl + "/tasks/search?name=Search",
                TaskResponse[].class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }
    
    @Test
    @DisplayName("Should validate command before creating task")
    void shouldValidateCommandBeforeCreatingTask() {
        TaskRequest request = TaskRequest.builder()
                .id("task4")
                .name("Invalid Command Task")
                .owner("testuser")
                .command("rm -rf /")  // Invalid command
                .build();
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/tasks",
                request,
                String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    @DisplayName("Should get all tasks with pagination")
    void shouldGetAllTasksWithPagination() {
        // Create multiple tasks
        for (int i = 0; i < 15; i++) {
            taskRepository.save(Task.builder()
                    .id("task" + i)
                    .name("Task " + i)
                    .owner("user")
                    .command("echo " + i)
                    .build());
        }
        
        // Get first page
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/tasks?page=0&size=10",
                String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
