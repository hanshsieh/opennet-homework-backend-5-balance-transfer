package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rocketmq")
public class RocketMQProperties {

	private String nameServer = "127.0.0.1:9876";

	private Producer producer = new Producer();

	private Topic topic = new Topic();

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

		private String group = "default-producer-group";

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}
	}

	public static class Topic {

		private String events = "balance-transfer-events";

		public String getEvents() {
			return events;
		}

		public void setEvents(String events) {
			this.events = events;
		}
	}
}
