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
public interface UserRepository extends JpaRepository<UserEntity, String> {

	boolean existsByUserId(String userId);

	Optional<UserEntity> findByUserId(String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM UserEntity u WHERE u.userId IN :userIds ORDER BY u.userId ASC")
	List<UserEntity> findByUserIdsForUpdate(@Param("userIds") Collection<String> userIds);
}
