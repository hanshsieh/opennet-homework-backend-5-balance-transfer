package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
/**
 * Spring Boot application entry point.
 */
public class DemoApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args JVM command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
