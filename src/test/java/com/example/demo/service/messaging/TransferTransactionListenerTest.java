package com.example.demo.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.repository.TransferRepository;
import com.example.demo.service.messaging.localargs.PendingTransferLocalArgs;
import com.example.demo.service.messaging.payload.PendingTransferPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransferTransactionListenerTest {

	@Mock
	private TransferRepository transferRepository;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private TransferTransactionListener listener;

	@Test
	void topic_shouldReturnPendingTransferTopic() {
		assertThat(listener.topic()).isEqualTo(MessageTopic.PENDING_TRANSFER);
	}

	@Test
	void executeLocalTransaction_shouldCommitWhenSaveSucceeds() {
		final var args = PendingTransferLocalArgs.builder()
				.transferId("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(11L)
				.build();

		final var result = listener.executeLocalTransaction(
				new Message(MessageTopic.PENDING_TRANSFER.getTopicName(), new byte[0]), args);

		assertThat(result).isEqualTo(LocalTransactionState.COMMIT_MESSAGE);
		final var captor = ArgumentCaptor.forClass(TransferEntity.class);
		verify(entityManager).persist(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.getId()).isEqualTo("t1");
		assertThat(saved.getFromUserId()).isEqualTo("u1");
		assertThat(saved.getToUserId()).isEqualTo("u2");
		assertThat(saved.getAmount()).isEqualTo(11L);
		assertThat(saved.getStatus()).isEqualTo(TransferStatus.PENDING);
	}

	@Test
	void executeLocalTransaction_shouldRollbackWhenSaveFails() {
		doThrow(new RuntimeException("db down")).when(entityManager).persist(any());
		final var args = PendingTransferLocalArgs.builder()
				.transferId("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(11L)
				.build();

		final var result = listener.executeLocalTransaction(
				new Message(MessageTopic.PENDING_TRANSFER.getTopicName(), new byte[0]), args);

		assertThat(result).isEqualTo(LocalTransactionState.ROLLBACK_MESSAGE);
	}

	@Test
	void checkLocalTransaction_shouldCommitWhenTransferExists() throws Exception {
		final var msg = new MessageExt();
		msg.setBody("{\"transferId\":\"t1\"}".getBytes(StandardCharsets.UTF_8));
		when(objectMapper.readValue(msg.getBody(), PendingTransferPayload.class))
				.thenReturn(PendingTransferPayload.builder().transferId("t1").build());
		when(transferRepository.findById("t1")).thenReturn(Optional.of(TransferEntity.builder().id("t1").build()));

		final var result = listener.checkLocalTransaction(msg);

		assertThat(result).isEqualTo(LocalTransactionState.COMMIT_MESSAGE);
	}

	@Test
	void checkLocalTransaction_shouldRollbackWhenTransferMissing() throws Exception {
		final var msg = new MessageExt();
		msg.setBody("{\"transferId\":\"missing\"}".getBytes(StandardCharsets.UTF_8));
		when(objectMapper.readValue(msg.getBody(), PendingTransferPayload.class))
				.thenReturn(PendingTransferPayload.builder().transferId("missing").build());
		when(transferRepository.findById("missing")).thenReturn(Optional.empty());

		final var result = listener.checkLocalTransaction(msg);

		assertThat(result).isEqualTo(LocalTransactionState.ROLLBACK_MESSAGE);
	}

	@Test
	void checkLocalTransaction_shouldReturnUnknownWhenExceptionThrown() throws Exception {
		final var msg = new MessageExt();
		msg.setBody("invalid".getBytes(StandardCharsets.UTF_8));
		when(objectMapper.readValue(msg.getBody(), PendingTransferPayload.class))
				.thenThrow(new RuntimeException("boom"));

		final var result = listener.checkLocalTransaction(msg);

		assertThat(result).isEqualTo(LocalTransactionState.UNKNOW);
	}
}
