package com.ivan.bankapi.service;

import com.ivan.bankapi.dto.*;
import com.ivan.bankapi.exception.BusinessException;
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
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getOwnerId()));

        String currency = request.getCurrency() != null ? request.getCurrency() : "PLN";
        Account account = new Account(generateAccountNumber(), owner, currency);
        account = accountRepository.save(account);
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
                TransactionType.DEPOSIT,
                request.getAmount(),
                account,
                null,
                null,
                request.getDescription(),
                newBalance
        ));

        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, WithdrawRequest request) {
        Account account = getAccount(accountId);
        requireActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient funds");
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        transactionRepository.save(new Transaction(
                TransactionType.WITHDRAWAL,
                request.getAmount(),
                account,
                null,
                null,
                request.getDescription(),
                newBalance
        ));

        return toResponse(account);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
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

        BigDecimal amount = request.getAmount();
        UUID transferGroupId = UUID.randomUUID();

        BigDecimal fromNewBalance = from.getBalance().subtract(amount);
        from.setBalance(fromNewBalance);
        accountRepository.save(from);

        BigDecimal toNewBalance = to.getBalance().add(amount);
        to.setBalance(toNewBalance);
        accountRepository.save(to);

        transactionRepository.save(new Transaction(
                TransactionType.TRANSFER_OUT,
                amount,
                from,
                to,
                transferGroupId,
                request.getDescription(),
                fromNewBalance
        ));

        transactionRepository.save(new Transaction(
                TransactionType.TRANSFER_IN,
                amount,
                to,
                from,
                transferGroupId,
                request.getDescription(),
                toNewBalance
        ));

        return new TransferResponse(
                transferGroupId,
                from.getId(),
                to.getId(),
                amount,
                fromNewBalance,
                toNewBalance
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
}