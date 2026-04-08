package com.example.demo.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.TransferEntity;
import com.example.demo.domain.TransferStatus;

@Repository
public class TransferRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public TransferRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void insert(String id, String fromUserId, String toUserId, long amount, TransferStatus status) {
		jdbc.update(
				"""
						INSERT INTO transfers (id, from_user_id, to_user_id, amount, status)
						VALUES (:id, :fromUserId, :toUserId, :amount, :status)
						""",
				new MapSqlParameterSource()
						.addValue("id", id)
						.addValue("fromUserId", fromUserId)
						.addValue("toUserId", toUserId)
						.addValue("amount", amount)
						.addValue("status", status.name()));
	}

	public Optional<TransferEntity> findById(String id) {
		var list = jdbc.query(
				"""
						SELECT id, from_user_id, to_user_id, amount, status, created_at
						FROM transfers WHERE id = :id
						""",
				new MapSqlParameterSource("id", id),
				(rs, rowNum) -> new TransferEntity(
						rs.getString("id"),
						rs.getString("from_user_id"),
						rs.getString("to_user_id"),
						rs.getLong("amount"),
						TransferStatus.valueOf(rs.getString("status")),
						toInstant(rs.getTimestamp("created_at"))));
		return list.stream().findFirst();
	}

	public long countByUserInvolved(String userId) {
		Long total = jdbc.queryForObject(
				"""
						SELECT COUNT(*) FROM transfers
						WHERE from_user_id = :userId OR to_user_id = :userId
						""",
				new MapSqlParameterSource("userId", userId),
				Long.class);
		return total == null ? 0L : total;
	}

	public PagedTransfers findByUserInvolved(String userId, int page, int size) {
		long total = countByUserInvolved(userId);
		int offset = page * size;
		var params = new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("limit", size)
				.addValue("offset", offset);
		List<TransferEntity> content = jdbc.query(
				"""
						SELECT id, from_user_id, to_user_id, amount, status, created_at
						FROM transfers
						WHERE from_user_id = :userId OR to_user_id = :userId
						ORDER BY created_at DESC
						LIMIT :limit OFFSET :offset
						""",
				params,
				(rs, rowNum) -> new TransferEntity(
						rs.getString("id"),
						rs.getString("from_user_id"),
						rs.getString("to_user_id"),
						rs.getLong("amount"),
						TransferStatus.valueOf(rs.getString("status")),
						toInstant(rs.getTimestamp("created_at"))));
		return new PagedTransfers(content, total);
	}

	public record PagedTransfers(List<TransferEntity> content, long totalElements) {
	}

	public int updateStatus(String id, TransferStatus newStatus) {
		return jdbc.update(
				"UPDATE transfers SET status = :status WHERE id = :id",
				new MapSqlParameterSource()
						.addValue("id", id)
						.addValue("status", newStatus.name()));
	}

	private static Instant toInstant(Timestamp ts) {
		return ts == null ? null : ts.toInstant();
	}
}
