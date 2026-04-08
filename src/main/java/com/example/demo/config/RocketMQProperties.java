package com.example.demo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rocketmq")
@Component
@Validated
public class RocketMQProperties {

	@NotBlank
	private String nameServer;

	@Valid
	@NotNull
	private Producer producer;

	@Valid
	@NotNull
	private Topic topic;

	public String getNameServer() {
		return nameServer;
	}

	public void setNameServer(String nameServer) {
		this.nameServer = nameServer;
	}

	public Producer getProducer() {
		return producer;
	}

	public void setProducer(Producer producer) {
		this.producer = producer;
	}

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}

	public static class Producer {

		@NotBlank
		private String group;

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}
	}

	public static class Topic {

		@NotBlank
		private String events;

		public String getEvents() {
			return events;
		}

		public void setEvents(String events) {
			this.events = events;
		}
	}
}
