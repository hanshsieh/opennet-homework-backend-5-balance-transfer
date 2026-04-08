package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.UserEntity;

@Repository
public class UserRepository {

	private final NamedParameterJdbcTemplate jdbc;

	public UserRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public boolean existsByUserId(String userId) {
		final var count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM users WHERE user_id = :userId",
				new MapSqlParameterSource("userId", userId),
				Integer.class);
		return count != null && count > 0;
	}

	public void insert(String userId, long initialBalance) {
		jdbc.update(
				"INSERT INTO users (user_id, balance) VALUES (:userId, :balance)",
				new MapSqlParameterSource()
						.addValue("userId", userId)
						.addValue("balance", initialBalance));
	}

	public Optional<Long> findBalanceByUserId(String userId) {
		final var list = jdbc.query(
				"SELECT balance FROM users WHERE user_id = :userId",
				new MapSqlParameterSource("userId", userId),
				(rs, rowNum) -> rs.getLong("balance"));
		return list.stream().findFirst();
	}

	public List<UserEntity> findByUserIdsForUpdate(List<String> userIds) {
		final var params = new MapSqlParameterSource()
				.addValue("userIds", userIds);
		return jdbc.query(
				"""
						SELECT user_id, balance, created_at, updated_at
						FROM users
						WHERE user_id IN (:userIds)
						ORDER BY user_id ASC
						FOR UPDATE
						""",
				params,
				(rs, rowNum) -> UserEntity.builder()
						.userId(rs.getString("user_id"))
						.balance(rs.getLong("balance"))
						.createdAt(toInstant(rs.getTimestamp("created_at")))
						.updatedAt(toInstant(rs.getTimestamp("updated_at")))
						.build());
	}

	private static Instant toInstant(Timestamp ts) {
		return ts == null ? null : ts.toInstant();
	}

	public int overwriteBalance(String userId, long balance) {
		return jdbc.update(
				"UPDATE users SET balance = :balance WHERE user_id = :userId",
				new MapSqlParameterSource()
						.addValue("userId", userId)
						.addValue("balance", balance));
	}
}
