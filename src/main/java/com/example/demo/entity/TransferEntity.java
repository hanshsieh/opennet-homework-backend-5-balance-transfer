package com.example.demo.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transfers")
/**
 * Represents the TransferEntity class.
 */
public class TransferEntity {
	@Column(name = "created_at", nullable = false, updatable = false)
	@CreationTimestamp
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	@UpdateTimestamp
	private Instant updatedAt;

	@Id
	@Column(name = "id", nullable = false, length = 36)
	private String id;

	@Column(name = "from_user_id", nullable = false, length = 64)
	private String fromUserId;

	@Column(name = "to_user_id", nullable = false, length = 64)
	private String toUserId;

	@Column(name = "amount", nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private TransferStatus status;
}
