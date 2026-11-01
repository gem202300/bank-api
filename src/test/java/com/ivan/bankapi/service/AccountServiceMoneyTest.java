package com.ivan.bankapi.service;

import com.ivan.bankapi.dto.*;
import com.ivan.bankapi.exception.BusinessException;
import com.ivan.bankapi.model.User;
import com.ivan.bankapi.model.enums.Role;
import com.ivan.bankapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AccountServiceMoneyTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    private Long ownerId;
    private Long accountAId;
    private Long accountBId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User(
                "test-" + System.nanoTime() + "@bank.com",
                "password",
                Role.USER
        ));
        ownerId = user.getId();

        CreateAccountRequest req = new CreateAccountRequest();
        req.setOwnerId(ownerId);
        req.setCurrency("PLN");

        accountAId = accountService.create(req).getId();
        accountBId = accountService.create(req).getId();
    }

    @Test
    void deposit_increasesBalance() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAmount(new BigDecimal("100.00"));

        AccountResponse result = accountService.deposit(accountAId, deposit);

        assertEquals(0, new BigDecimal("100.00").compareTo(result.getBalance()));
    }

    @Test
    void withdraw_failsWhenInsufficientFunds() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAmount(new BigDecimal("50.00"));
        accountService.deposit(accountAId, deposit);

        WithdrawRequest withdraw = new WithdrawRequest();
        withdraw.setAmount(new BigDecimal("100.00"));

        assertThrows(BusinessException.class,
                () -> accountService.withdraw(accountAId, withdraw));
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAmount(new BigDecimal("200.00"));
        accountService.deposit(accountAId, deposit);

        TransferRequest transfer = new TransferRequest();
        transfer.setFromAccountId(accountAId);
        transfer.setToAccountId(accountBId);
        transfer.setAmount(new BigDecimal("75.00"));

        TransferResponse result = accountService.transfer(transfer, ownerId, null);

        assertEquals(0, new BigDecimal("125.00").compareTo(
                accountService.findById(accountAId).getBalance()));
        assertEquals(0, new BigDecimal("75.00").compareTo(
                accountService.findById(accountBId).getBalance()));
        assertNotNull(result.getTransferGroupId());
    }

    @Test
    void transfer_sameIdempotencyKey_doesNotDoubleCharge() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAmount(new BigDecimal("100.00"));
        accountService.deposit(accountAId, deposit);

        TransferRequest transfer = new TransferRequest();
        transfer.setFromAccountId(accountAId);
        transfer.setToAccountId(accountBId);
        transfer.setAmount(new BigDecimal("40.00"));

        String key = "test-key-" + System.nanoTime();

        TransferResponse first = accountService.transfer(transfer, ownerId, key);
        TransferResponse second = accountService.transfer(transfer, ownerId, key);

        assertEquals(first.getTransferGroupId(), second.getTransferGroupId());
        assertEquals(0, new BigDecimal("60.00").compareTo(
                accountService.findById(accountAId).getBalance()));
        assertEquals(0, new BigDecimal("40.00").compareTo(
                accountService.findById(accountBId).getBalance()));
    }

    @Test
    void transfer_toSameAccount_fails() {
        TransferRequest transfer = new TransferRequest();
        transfer.setFromAccountId(accountAId);
        transfer.setToAccountId(accountAId);
        transfer.setAmount(new BigDecimal("10.00"));

        assertThrows(BusinessException.class,
                () -> accountService.transfer(transfer, ownerId, null));
    }
}