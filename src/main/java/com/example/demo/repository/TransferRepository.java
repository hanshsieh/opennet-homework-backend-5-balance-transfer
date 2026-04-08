package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.TransferEntity;
import com.example.demo.entity.TransferStatus;

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
				this::mapRow);
		return list.stream().findFirst();
	}

	/**
	 * Locks the transfer row for update (caller must run inside a transaction).
	 */
	public Optional<TransferEntity> findByIdForUpdate(String id) {
		var list = jdbc.query(
				"""
						SELECT id, from_user_id, to_user_id, amount, status, created_at
						FROM transfers WHERE id = :id FOR UPDATE
						""",
				new MapSqlParameterSource("id", id),
				this::mapRow);
		return list.stream().findFirst();
	}

	private TransferEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new TransferEntity(
				rs.getString("id"),
				rs.getString("from_user_id"),
				rs.getString("to_user_id"),
				rs.getLong("amount"),
				TransferStatus.valueOf(rs.getString("status")),
				toInstant(rs.getTimestamp("created_at")));
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
				this::mapRow);
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

	public int updateStatusIf(String id, TransferStatus expected, TransferStatus newStatus) {
		return jdbc.update(
				"""
						UPDATE transfers SET status = :newStatus
						WHERE id = :id AND status = :expected
						""",
				new MapSqlParameterSource()
						.addValue("id", id)
						.addValue("expected", expected.name())
						.addValue("newStatus", newStatus.name()));
	}

	private static Instant toInstant(Timestamp ts) {
		return ts == null ? null : ts.toInstant();
	}
}
