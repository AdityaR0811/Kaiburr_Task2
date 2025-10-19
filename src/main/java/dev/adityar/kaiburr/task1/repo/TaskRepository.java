package dev.adityar.kaiburr.task1.repo;

import dev.adityar.kaiburr.task1.domain.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Task entity
 * Author: Aditya R.
 */
@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    
    /**
     * Case-insensitive search by name containing substring
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Task> findByNameContainingIgnoreCase(String name);
}
