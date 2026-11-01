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
    public TransferResponse transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam Long userId
    ) {
        return accountService.transfer(request, userId, idempotencyKey);
    }

    @GetMapping("/{transferGroupId}")
    public java.util.List<com.ivan.bankapi.dto.TransactionResponse> getTransfer(
            @PathVariable java.util.UUID transferGroupId
    ) {
        return accountService.getByTransferGroupId(transferGroupId);
    }
}