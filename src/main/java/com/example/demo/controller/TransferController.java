package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.example.demo.dto.CreateTransferResponse;
import com.example.demo.dto.PagedTransferResponse;
import com.example.demo.dto.TransferRequest;
import com.example.demo.dto.TransferResponse;
import com.example.demo.service.TransferService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/transfers")
/**
 * Exposes transfer-related HTTP endpoints.
 */
public class TransferController {

	private final TransferService transferService;

	/**
	 * Creates a transfer controller.
	 *
	 * @param transferService transfer application service
	 */
	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping
	/**
	 * Creates a transfer request.
	 *
	 * @param request transfer request
	 * @return created transfer id wrapped in response body
	 */
	public ResponseEntity<CreateTransferResponse> createTransfer(@Validated @RequestBody TransferRequest request) {
		final var transferId = transferService.createTransfer(request);
		final var body = CreateTransferResponse.builder()
				.id(transferId)
				.build();
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping
	/**
	 * Lists transfers for one user with pagination.
	 *
	 * @param userId user id
	 * @param page zero-based page number
	 * @param size page size
	 * @return paged transfer response
	 */
	public PagedTransferResponse listTransfers(
			@RequestParam @NotBlank String userId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(0) @Max(100) int size) {
		return transferService.listTransfers(userId, page, size);
	}

	@PostMapping("/{transferId}/cancel")
	/**
	 * Cancels a pending transfer.
	 *
	 * @param transferId transfer id
	 * @return transfer response after cancellation
	 */
	public TransferResponse cancel(@PathVariable @NotBlank String transferId) {
		return transferService.cancelTransfer(transferId);
	}
}
