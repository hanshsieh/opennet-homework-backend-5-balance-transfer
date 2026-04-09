package com.example.demo.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.TransferRequest;
import com.example.demo.service.messaging.localargs.PendingTransferLocalArgs;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MessagePublisherTest {

	@Mock
	private TransactionMQProducer producer;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private MessagePublisher publisher;

	@Test
	void sendPendingTransfer_shouldBuildMessageAndLocalArgs() throws Exception {
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(50L)
				.build();
		final var sendResult = new TransactionSendResult();
		when(objectMapper.writeValueAsString(any())).thenReturn("{\"transferId\":\"t1\"}");
		when(producer.sendMessageInTransaction(any(), any())).thenReturn(sendResult);

		final var result = publisher.sendPendingTransfer("t1", request);

		assertThat(result).isSameAs(sendResult);

		final var msgCaptor = ArgumentCaptor.forClass(org.apache.rocketmq.common.message.Message.class);
		final var argsCaptor = ArgumentCaptor.forClass(PendingTransferLocalArgs.class);
		verify(producer).sendMessageInTransaction(msgCaptor.capture(), argsCaptor.capture());
		final var msg = msgCaptor.getValue();
		assertThat(msg.getTopic()).isEqualTo(MessageTopic.PENDING_TRANSFER.getTopicName());
		assertThat(new String(msg.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"transferId\":\"t1\"}");

		final var localArgs = argsCaptor.getValue();
		assertThat(localArgs.getTransferId()).isEqualTo("t1");
		assertThat(localArgs.getFromUserId()).isEqualTo("u1");
		assertThat(localArgs.getToUserId()).isEqualTo("u2");
		assertThat(localArgs.getAmount()).isEqualTo(50L);
	}
}
