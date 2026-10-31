package com.ivan.bankapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferResponse {

    private UUID transferGroupId;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;

    public TransferResponse() {
    }

    public TransferResponse(UUID transferGroupId, Long fromAccountId, Long toAccountId,
                            BigDecimal amount, BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter) {
        this.transferGroupId = transferGroupId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceAfter = toBalanceAfter;
    }

    public UUID getTransferGroupId() {
        return transferGroupId;
    }

    public void setTransferGroupId(UUID transferGroupId) {
        this.transferGroupId = transferGroupId;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFromBalanceAfter() {
        return fromBalanceAfter;
    }

    public void setFromBalanceAfter(BigDecimal fromBalanceAfter) {
        this.fromBalanceAfter = fromBalanceAfter;
    }

    public BigDecimal getToBalanceAfter() {
        return toBalanceAfter;
    }

    public void setToBalanceAfter(BigDecimal toBalanceAfter) {
        this.toBalanceAfter = toBalanceAfter;
    }
}