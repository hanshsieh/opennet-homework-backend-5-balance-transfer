package com.example.demo.repository;

import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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

	/**
	 * Debits balance if sufficient funds. Returns number of rows updated (0 or 1).
	 */
	public int debitIfSufficient(String userId, long amount) {
		return jdbc.update(
				"UPDATE users SET balance = balance - :amount WHERE user_id = :userId AND balance >= :amount",
				new MapSqlParameterSource()
						.addValue("userId", userId)
						.addValue("amount", amount));
	}

	public int credit(String userId, long amount) {
		return jdbc.update(
				"UPDATE users SET balance = balance + :amount WHERE user_id = :userId",
				new MapSqlParameterSource()
						.addValue("userId", userId)
						.addValue("amount", amount));
	}
}
