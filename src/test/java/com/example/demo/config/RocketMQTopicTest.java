package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RocketMQTopicTest {

	@Test
	void getTopicName_shouldReturnConfiguredTopicName() {
		assertThat(RocketMQTopic.PENDING_TRANSFER.getTopicName()).isEqualTo("pending-transfer");
	}

	@Test
	void fromTopicName_shouldReturnMatchingEnumValue() {
		assertThat(RocketMQTopic.fromTopicName("pending-transfer"))
				.contains(RocketMQTopic.PENDING_TRANSFER);
	}

	@Test
	void fromTopicName_shouldReturnEmptyWhenNoMatch() {
		assertThat(RocketMQTopic.fromTopicName("unknown")).isEmpty();
	}
}
