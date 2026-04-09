package com.example.demo.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
/**
 * Represents the UserEntity class.
 */
public class UserEntity {
	@Column(name = "created_at", nullable = false, updatable = false)
	@CreationTimestamp
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	@UpdateTimestamp
	private Instant updatedAt;

	@Id
	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "balance", nullable = false)
	private long balance;
}
