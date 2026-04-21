package com.example.demo.service.messaging.transactionlistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;

import com.example.demo.service.messaging.MessageTopic;

class AppTransactionListenerTest {

	@Test
	void constructor_shouldThrowWhenDuplicateTopicListenersProvided() {
		final var first = mock(TopicTransactionListener.class);
		final var second = mock(TopicTransactionListener.class);
		when(first.topic()).thenReturn(MessageTopic.PENDING_TRANSFER);
		when(second.topic()).thenReturn(MessageTopic.PENDING_TRANSFER);

		assertThatThrownBy(() -> new AppTransactionListener(List.of(first, second)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void executeLocalTransaction_shouldDelegateToTopicListener() {
		final var delegate = mock(TopicTransactionListener.class);
		when(delegate.topic()).thenReturn(MessageTopic.PENDING_TRANSFER);
		when(delegate.executeLocalTransaction(any(), any()))
				.thenReturn(LocalTransactionState.COMMIT_MESSAGE);
		final var listener = new AppTransactionListener(List.of(delegate));
		final var msg = new Message(MessageTopic.PENDING_TRANSFER.getTopicName(), new byte[0]);

		final var result = listener.executeLocalTransaction(msg, "arg");

		assertThat(result).isEqualTo(LocalTransactionState.COMMIT_MESSAGE);
		verify(delegate).executeLocalTransaction(msg, "arg");
	}

	@Test
	void executeLocalTransaction_shouldRollbackWhenTopicUnsupported() {
		final var delegate = mock(TopicTransactionListener.class);
		when(delegate.topic()).thenReturn(MessageTopic.PENDING_TRANSFER);
		final var listener = new AppTransactionListener(List.of(delegate));
		final var msg = new Message("unknown-topic", new byte[0]);

		final var result = listener.executeLocalTransaction(msg, "arg");

		assertThat(result).isEqualTo(LocalTransactionState.ROLLBACK_MESSAGE);
		verify(delegate, never()).executeLocalTransaction(any(), any());
	}

	@Test
	void checkLocalTransaction_shouldDelegateToTopicListener() {
		final var delegate = mock(TopicTransactionListener.class);
		when(delegate.topic()).thenReturn(MessageTopic.PENDING_TRANSFER);
		when(delegate.checkLocalTransaction(any()))
				.thenReturn(LocalTransactionState.COMMIT_MESSAGE);
		final var listener = new AppTransactionListener(List.of(delegate));
		final var msg = new MessageExt();
		msg.setTopic(MessageTopic.PENDING_TRANSFER.getTopicName());

		final var result = listener.checkLocalTransaction(msg);

		assertThat(result).isEqualTo(LocalTransactionState.COMMIT_MESSAGE);
		verify(delegate).checkLocalTransaction(msg);
	}

	@Test
	void checkLocalTransaction_shouldReturnUnknownWhenTopicUnsupported() {
		final var delegate = mock(TopicTransactionListener.class);
		when(delegate.topic()).thenReturn(MessageTopic.PENDING_TRANSFER);
		final var listener = new AppTransactionListener(List.of(delegate));
		final var msg = new MessageExt();
		msg.setTopic("unknown-topic");

		final var result = listener.checkLocalTransaction(msg);

		assertThat(result).isEqualTo(LocalTransactionState.UNKNOW);
		verify(delegate, never()).checkLocalTransaction(any());
	}
}
