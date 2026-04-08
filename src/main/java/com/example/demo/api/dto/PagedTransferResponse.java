package com.example.demo.api.dto;

import java.util.List;

public record PagedTransferResponse(
		List<TransferResponse> content,
		long totalElements,
		int number,
		int size
) {
}
