package com.ivan.bankapi.repository;

import com.ivan.bankapi.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord,Long> {
    Optional<IdempotencyRecord> findByKeyAndUserId(String key, Long userId);
}
