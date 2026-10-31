package com.ivan.bankapi.service;

import com.ivan.bankapi.dto.AccountResponse;
import com.ivan.bankapi.dto.CreateAccountRequest;
import com.ivan.bankapi.dto.DepositRequest;
import com.ivan.bankapi.exception.ResourceNotFoundException;
import com.ivan.bankapi.model.Account;
import com.ivan.bankapi.model.Transaction;
import com.ivan.bankapi.model.User;
import com.ivan.bankapi.model.enums.AccountStatus;
import com.ivan.bankapi.model.enums.TransactionType;
import com.ivan.bankapi.repository.AccountRepository;
import com.ivan.bankapi.repository.TransactionRepository;
import com.ivan.bankapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository
            ;
    public AccountService(AccountRepository accountRepository, UserRepository userRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getOwnerId()));

        String currency = request.getCurrency() != null ? request.getCurrency() : "PLN";
        String accountNumber = generateAccountNumber();

        Account account = new Account(accountNumber, owner, currency);
        account = accountRepository.save(account);

        return toResponse(account);
    }

    @Transactional
    public AccountResponse deposit(Long accountId, DepositRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = new Transaction(
                TransactionType.DEPOSIT,
                request.getAmount(),
                account,
                null,
                null,
                request.getDescription(),
                newBalance
        );
        transactionRepository.save(tx);

        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findByOwnerId(Long ownerId) {
        return accountRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        return toResponse(account);
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
}