package com.example.demo.service.messaging;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
/**
 * Declares RocketMQ topics used by this application.
 */
public enum MessageTopic {

	PENDING_TRANSFER("pending-transfer");

	private final String topicName;

	/**
	 * Resolves enum value from raw topic name.
	 *
	 * @param topicName topic name from RocketMQ metadata
	 * @return matched topic enum if present
	 */
	public static Optional<MessageTopic> fromTopicName(String topicName) {
		return Arrays.stream(values())
				.filter(t -> t.topicName.equals(topicName))
				.findFirst();
	}
}
