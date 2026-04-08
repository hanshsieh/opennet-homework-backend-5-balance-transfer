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
	@JsonProperty("content")
	private List<TransferResponse> content;

	@JsonProperty("totalElements")
	private long totalElements;

	@JsonProperty("number")
	private int number;

	@JsonProperty("size")
	private int size;
}
