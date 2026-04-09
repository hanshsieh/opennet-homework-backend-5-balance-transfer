package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import com.example.demo.entity.UserEntity;

@Repository
/**
 * Repository for user persistence and locking queries.
 */
public interface UserRepository extends JpaRepository<UserEntity, String> {

	/**
	 * Checks whether a user exists by user id.
	 *
	 * @param userId user id
	 * @return true if user exists
	 */
	boolean existsByUserId(String userId);

	/**
	 * Finds a user by user id.
	 *
	 * @param userId user id
	 * @return user entity if found
	 */
	Optional<UserEntity> findByUserId(String userId);

	/**
	 * Locks user rows for update in deterministic order.
	 *
	 * <p>
	 * Locking users in a consistent (sorted) order by user ID prevents deadlocks
	 * when multiple transactions attempt to lock overlapping sets of users.
	 * </p>
	 *
	 * @param userIds user ids to lock
	 * @return locked user entities sorted by user id
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM UserEntity u WHERE u.userId IN :userIds ORDER BY u.userId ASC")
	List<UserEntity> findByUserIdsForUpdate(@Param("userIds") Collection<String> userIds);
}
