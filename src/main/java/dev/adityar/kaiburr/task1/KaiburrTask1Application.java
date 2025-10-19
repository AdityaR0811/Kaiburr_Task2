package dev.adityar.kaiburr.task1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Kaiburr Task 1 - Secure Task Execution Service
 * Author: Aditya R.
 */
@SpringBootApplication
@EnableMongoRepositories
public class KaiburrTask1Application {

    public static void main(String[] args) {
        SpringApplication.run(KaiburrTask1Application.class, args);
    }
}
