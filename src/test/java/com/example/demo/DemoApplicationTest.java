package com.example.demo;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class DemoApplicationTest {

	@Test
	void main_shouldRunSpringApplication() {
		final String[] args = { "--spring.main.web-application-type=none" };
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			DemoApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(DemoApplication.class, args));
		}
	}
}
