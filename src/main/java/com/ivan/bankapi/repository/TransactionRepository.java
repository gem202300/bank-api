package com.ivan.bankapi.repository;

import com.ivan.bankapi.model.Transaction;
import com.ivan.bankapi.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    Page<Transaction> findByAccountIdAndType(Long accountId, TransactionType type, Pageable pageable);

    Page<Transaction> findByAccountIdAndCreatedAtBetween(
            Long accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Transaction> findByAccountIdAndTypeAndCreatedAtBetween(
            Long accountId, TransactionType type, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Transaction> findByTransferGroupId(UUID transferGroupId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.account.id = :accountId
              AND t.type IN :types
              AND t.createdAt >= :startOfDay
              AND t.createdAt < :endOfDay
            """)
    BigDecimal sumOutgoingForDay(
            @Param("accountId") Long accountId,
            @Param("types") List<TransactionType> types,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}