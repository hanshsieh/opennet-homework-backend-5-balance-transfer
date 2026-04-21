package com.example.demo.service.messaging.messagelistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.service.TransferSettlementService;
import com.example.demo.service.messaging.payload.PendingTransferPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransferMessageListenerTest {

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private TransferSettlementService settlementService;

	@InjectMocks
	private TransferMessageListener listener;

	@Test
	void consumeMessage_shouldSettleAndReturnSuccess() throws Exception {
		final var msg = new MessageExt();
		msg.setBody("{\"transferId\":\"t1\"}".getBytes(StandardCharsets.UTF_8));
		when(objectMapper.readValue(msg.getBody(), PendingTransferPayload.class))
				.thenReturn(PendingTransferPayload.builder().transferId("t1").build());

		final var result = listener.consumeMessage(List.of(msg), null);

		assertThat(result).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
		verify(settlementService).settle("t1");
	}

	@Test
	void consumeMessage_shouldReturnRetryWhenDeserializationFails() throws Exception {
		final var msg = new MessageExt();
		msg.setBody("not-json".getBytes(StandardCharsets.UTF_8));
		when(objectMapper.readValue(msg.getBody(), PendingTransferPayload.class))
				.thenThrow(new RuntimeException("boom"));

		final var result = listener.consumeMessage(List.of(msg), null);

		assertThat(result).isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);
		verify(settlementService, never()).settle("t1");
	}
}
