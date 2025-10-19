package dev.adityar.kaiburr.task2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration.
 * 
 * @author Aditya R
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI kaiburrOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Kaiburr Task 2 — Kubernetes Job Execution API")
                .description("Production-grade REST API for executing commands via Kubernetes Jobs with policy-based validation and security hardening")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Aditya R")
                    .email("contact@example.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
