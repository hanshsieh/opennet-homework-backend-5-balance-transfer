package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.demo.dto.PagedTransferResponse;
import com.example.demo.dto.TransferRequest;
import com.example.demo.dto.TransferResponse;
import com.example.demo.service.TransferService;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

	@Mock
	private TransferService transferService;

	@Test
	void createTransfer_shouldReturnCreatedResponseWithTransferId() {
		final var controller = new TransferController(transferService);
		final var request = TransferRequest.builder()
				.fromUserId("u1")
				.toUserId("u2")
				.amount(99L)
				.build();
		when(transferService.createTransfer(request)).thenReturn("t1");

		final var response = controller.createTransfer(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo("t1");
		verify(transferService).createTransfer(request);
	}

	@Test
	void listTransfers_shouldDelegateToServiceWithGivenParameters() {
		final var controller = new TransferController(transferService);
		final var transfer = TransferResponse.builder()
				.id("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(80L)
				.status("PENDING")
				.createdAt(Instant.now())
				.build();
		final var expected = PagedTransferResponse.builder()
				.items(List.of(transfer))
				.total(1L)
				.pageNumber(0)
				.pageSize(20)
				.build();
		when(transferService.listTransfers("u1", 0, 20)).thenReturn(expected);

		final var response = controller.listTransfers("u1", 0, 20);

		assertThat(response).isSameAs(expected);
		verify(transferService).listTransfers("u1", 0, 20);
	}

	@Test
	void cancel_shouldReturnCancelledTransferFromService() {
		final var controller = new TransferController(transferService);
		final var expected = TransferResponse.builder()
				.id("t1")
				.fromUserId("u1")
				.toUserId("u2")
				.amount(80L)
				.status("CANCELLED")
				.createdAt(Instant.now())
				.build();
		when(transferService.cancelTransfer("t1")).thenReturn(expected);

		final var response = controller.cancel("t1");

		assertThat(response).isSameAs(expected);
		verify(transferService).cancelTransfer("t1");
	}
}
