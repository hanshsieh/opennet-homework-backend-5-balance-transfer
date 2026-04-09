package com.example.demo.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedTransferResponse {
	@JsonProperty("items")
	private List<TransferResponse> items;

	@JsonProperty("total")
	private long total;

	@JsonProperty("pageNumber")
	private int pageNumber;

	@JsonProperty("pageSize")
	private int pageSize;
}
