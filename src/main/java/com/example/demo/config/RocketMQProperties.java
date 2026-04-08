package com.example.demo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rocketmq")
@Component
@Validated
@Data
public class RocketMQProperties {

	@NotBlank
	private String nameServer;

	@Valid
	@NotNull
	private Producer producer;

	@Valid
	@NotNull
	private Topics topics;

	@Valid
	@NotNull
	private Consumer consumer;

	@Data
	public static class Producer {

		@NotBlank
		private String group;
	}

	@Data
	public static class Topics {

		@NotBlank
		private String pendingTransfer;
	}

	@Data
	public static class Consumer {

		@NotBlank
		private String group;
	}
}
