package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.demo.service.messaging.MessageTopic;

class RocketMQTopicTest {

	@Test
	void getTopicName_shouldReturnConfiguredTopicName() {
		assertThat(MessageTopic.PENDING_TRANSFER.getTopicName()).isEqualTo("pending-transfer");
	}

	@Test
	void fromTopicName_shouldReturnMatchingEnumValue() {
		assertThat(MessageTopic.fromTopicName("pending-transfer"))
				.contains(MessageTopic.PENDING_TRANSFER);
	}

	@Test
	void fromTopicName_shouldReturnEmptyWhenNoMatch() {
		assertThat(MessageTopic.fromTopicName("unknown")).isEmpty();
	}
}
