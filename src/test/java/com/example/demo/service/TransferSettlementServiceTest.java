package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TransferSettlementServiceTest {

	@Mock
	private TransferRepository transferRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserCacheService cacheEvictor;

	@InjectMocks
	private TransferSettlementService transferSettlementService;

	@Test
	void settle_shouldSkipWhenTransferMissing() {
		when(transferRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

		transferSettlementService.settle("missing");

		verify(userRepository, never()).findByUserIdsForUpdate(List.of("u1", "u2"));
		verify(cacheEvictor, never()).evictBalance("u1");
		verify(cacheEvictor, never()).evictBalance("u2");
	}

	@Test
	void settle_shouldFailWhenInsufficientBalance() {
		final var transfer = TransferEntity.builder()
				.id("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(100L)
				.status(TransferStatus.PENDING)
				.build();
		final var fromUser = UserEntity.builder().userId("u1").balance(50L).build();
		final var toUser = UserEntity.builder().userId("u2").balance(20L).build();
		when(transferRepository.findByIdForUpdate("t1")).thenReturn(Optional.of(transfer));
		when(userRepository.findByUserIdsForUpdate(List.of("u1", "u2"))).thenReturn(List.of(fromUser, toUser));

		transferSettlementService.settle("t1");

		assertThat(transfer.getStatus()).isEqualTo(TransferStatus.FAILED);
		verify(cacheEvictor, never()).evictBalance("u1");
		verify(cacheEvictor, never()).evictBalance("u2");
	}

	@Test
	void settle_shouldSettleAndEvictCachesWhenBalanceEnough() {
		final var transfer = TransferEntity.builder()
				.id("t2")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(40L)
				.status(TransferStatus.PENDING)
				.build();
		final var fromUser = UserEntity.builder().userId("u1").balance(100L).build();
		final var toUser = UserEntity.builder().userId("u2").balance(20L).build();
		when(transferRepository.findByIdForUpdate("t2")).thenReturn(Optional.of(transfer));
		when(userRepository.findByUserIdsForUpdate(List.of("u1", "u2"))).thenReturn(List.of(fromUser, toUser));

		transferSettlementService.settle("t2");

		assertThat(transfer.getStatus()).isEqualTo(TransferStatus.SETTLED);
		assertThat(fromUser.getBalance()).isEqualTo(60L);
		assertThat(toUser.getBalance()).isEqualTo(60L);
		verify(cacheEvictor).evictBalance("u1");
		verify(cacheEvictor).evictBalance("u2");
	}
}
