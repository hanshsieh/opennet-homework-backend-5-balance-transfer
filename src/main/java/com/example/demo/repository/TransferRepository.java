package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.TransferEntity;
import jakarta.persistence.LockModeType;

@Repository
/**
 * Repository for transfer persistence and history queries.
 */
public interface TransferRepository extends JpaRepository<TransferEntity, String> {

	/**
	 * Locks the transfer row for update (caller must run inside a transaction).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT t FROM TransferEntity t WHERE t.id = :id")
	Optional<TransferEntity> findByIdForUpdate(@Param("id") String id);

	/**
	 * Finds transfer history for one user with pagination.
	 *
	 * @param userId user id
	 * @param pageable pagination request
	 * @return transfer entities ordered by creation time descending
	 */
	@Query("SELECT t FROM TransferEntity t WHERE t.fromUserId = :userId OR t.toUserId = :userId ORDER BY t.createdAt DESC")
	List<TransferEntity> findByUserId(@Param("userId") String userId,
			Pageable pageable);

	/**
	 * Counts transfer history records for one user.
	 *
	 * @param userId user id
	 * @return total transfer count
	 */
	@Query("SELECT COUNT(*) FROM TransferEntity t WHERE t.fromUserId = :userId OR t.toUserId = :userId")
	long countByUserId(@Param("userId") String userId);
}
