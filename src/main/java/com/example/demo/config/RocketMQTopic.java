package com.example.demo.config;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RocketMQTopic {

	PENDING_TRANSFER("pending-transfer");

	private final String topicName;

	public static RocketMQTopic fromTopicName(String topicName) {
		return Arrays.stream(values())
				.filter(t -> t.topicName.equals(topicName))
				.findFirst()
				.orElse(null);
	}
}
