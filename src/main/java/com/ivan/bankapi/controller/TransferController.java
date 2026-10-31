package com.ivan.bankapi.controller;

import com.ivan.bankapi.dto.TransferRequest;
import com.ivan.bankapi.dto.TransferResponse;
import com.ivan.bankapi.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final AccountService accountService;

    public TransferController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return accountService.transfer(request);
    }
}