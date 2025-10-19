package dev.adityar.kaiburr.task2.repo;

import dev.adityar.kaiburr.task2.domain.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Task persistence operations.
 * 
 * @author Aditya R
 */
@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    
    /**
     * Search tasks by name substring (case-insensitive).
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Task> findByNameContaining(String nameSubstring);
}
