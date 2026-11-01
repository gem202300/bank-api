package com.ivan.bankapi.service;

import com.ivan.bankapi.dto.*;
import com.ivan.bankapi.exception.BusinessException;
import com.ivan.bankapi.exception.ResourceNotFoundException;
import com.ivan.bankapi.model.*;
import com.ivan.bankapi.model.enums.AccountStatus;
import com.ivan.bankapi.model.enums.TransactionType;
import com.ivan.bankapi.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("5000.00");

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          IdempotencyRecordRepository idempotencyRecordRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getOwnerId()));
        String currency = request.getCurrency() != null ? request.getCurrency() : "PLN";
        Account account = new Account(generateAccountNumber(), owner, currency);
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(Long accountId, LocalDateTime from, LocalDateTime to) {
        Account account = getAccount(accountId);

        LocalDateTime fromDate = from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime toDate = to != null ? to : LocalDateTime.now();

        List<Transaction> txs = transactionRepository
                .findByAccountIdAndCreatedAtBetween(
                        accountId, fromDate, toDate,
                        org.springframework.data.domain.Pageable.unpaged()
                )
                .getContent();

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;

        for (Transaction t : txs) {
            switch (t.getType()) {
                case DEPOSIT, TRANSFER_IN -> totalIn = totalIn.add(t.getAmount());
                case WITHDRAWAL, TRANSFER_OUT -> totalOut = totalOut.add(t.getAmount());
            }
        }

        List<TransactionResponse> items = txs.stream().map(this::toTxResponse).toList();

        return new StatementResponse(
                account.getId(),
                account.getAccountNumber(),
                fromDate,
                toDate,
                account.getBalance(),
                totalIn,
                totalOut,
                items
        );
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findByOwnerId(Long ownerId) {
        return accountRepository.findByOwnerId(ownerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        return toResponse(getAccount(id));
    }

    @Transactional
    public AccountResponse deposit(Long accountId, DepositRequest request) {
        Account account = getAccount(accountId);
        requireActive(account);
        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);
        transactionRepository.save(new Transaction(
                TransactionType.DEPOSIT, request.getAmount(), account, null, null,
                request.getDescription(), newBalance));
        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, WithdrawRequest request) {
        Account account = getAccount(accountId);
        requireActive(account);
        checkDailyLimit(account.getId(), request.getAmount());

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient funds");
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);
        transactionRepository.save(new Transaction(
                TransactionType.WITHDRAWAL, request.getAmount(), account, null, null,
                request.getDescription(), newBalance));
        return toResponse(account);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, Long userId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyRecordRepository.findByKey(idempotencyKey);
            if (existing.isPresent()) {
                return rebuildTransferResponse(UUID.fromString(existing.get().getResourceId()));
            }
        }

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BusinessException("Cannot transfer to the same account");
        }

        Account from = getAccount(request.getFromAccountId());
        Account to = getAccount(request.getToAccountId());
        requireActive(from);
        requireActive(to);

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new BusinessException("Currency mismatch");
        }
        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient funds");
        }

        checkDailyLimit(from.getId(), request.getAmount());

        BigDecimal amount = request.getAmount();
        UUID transferGroupId = UUID.randomUUID();

        BigDecimal fromNew = from.getBalance().subtract(amount);
        from.setBalance(fromNew);
        accountRepository.save(from);

        BigDecimal toNew = to.getBalance().add(amount);
        to.setBalance(toNew);
        accountRepository.save(to);

        transactionRepository.save(new Transaction(
                TransactionType.TRANSFER_OUT, amount, from, to, transferGroupId,
                request.getDescription(), fromNew));
        transactionRepository.save(new Transaction(
                TransactionType.TRANSFER_IN, amount, to, from, transferGroupId,
                request.getDescription(), toNew));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyRecordRepository.save(
                    new IdempotencyRecord(idempotencyKey, userId, transferGroupId.toString()));
        }

        return new TransferResponse(transferGroupId, from.getId(), to.getId(), amount, fromNew, toNew);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            Long accountId,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        getAccount(accountId);

        Page<Transaction> page;
        if (type != null && from != null && to != null) {
            page = transactionRepository.findByAccountIdAndTypeAndCreatedAtBetween(
                    accountId, type, from, to, pageable);
        } else if (type != null) {
            page = transactionRepository.findByAccountIdAndType(accountId, type, pageable);
        } else if (from != null && to != null) {
            page = transactionRepository.findByAccountIdAndCreatedAtBetween(
                    accountId, from, to, pageable);
        } else {
            page = transactionRepository.findByAccountId(accountId, pageable);
        }

        return page.map(this::toTxResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByTransferGroupId(UUID transferGroupId) {
        List<Transaction> list = transactionRepository.findByTransferGroupId(transferGroupId);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("Transfer not found: " + transferGroupId);
        }
        return list.stream().map(this::toTxResponse).toList();
    }

    private void checkDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        BigDecimal alreadyUsed = transactionRepository.sumOutgoingForDay(
                accountId,
                List.of(TransactionType.WITHDRAWAL, TransactionType.TRANSFER_OUT),
                start,
                end
        );

        if (alreadyUsed.add(amount).compareTo(DAILY_LIMIT) > 0) {
            throw new BusinessException(
                    "Daily limit exceeded. Limit: " + DAILY_LIMIT + ", already used: " + alreadyUsed);
        }
    }

    private TransferResponse rebuildTransferResponse(UUID transferGroupId) {
        List<Transaction> legs = transactionRepository.findByTransferGroupId(transferGroupId);
        if (legs.size() < 2) {
            throw new BusinessException("Incomplete transfer record for idempotency key");
        }
        Transaction out = legs.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();
        Transaction in = legs.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();

        return new TransferResponse(
                transferGroupId,
                out.getAccount().getId(),
                in.getAccount().getId(),
                out.getAmount(),
                out.getBalanceAfter(),
                in.getBalanceAfter()
        );
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active: " + account.getId());
        }
    }

    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwner().getId(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    private TransactionResponse toTxResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getAccount().getId(),
                t.getRelatedAccount() != null ? t.getRelatedAccount().getId() : null,
                t.getTransferGroupId(),
                t.getDescription(),
                t.getBalanceAfter(),
                t.getCreatedAt()
        );
    }
}