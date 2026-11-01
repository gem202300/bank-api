package com.ivan.bankapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class StatementResponse {

    private Long accountId;
    private String accountNumber;
    private LocalDateTime from;
    private LocalDateTime to;
    private BigDecimal openingBalance;   // можна null, якщо не рахуємо
    private BigDecimal closingBalance;
    private BigDecimal totalIn;
    private BigDecimal totalOut;
    private List<TransactionResponse> transactions;

    public StatementResponse() {
    }

    public StatementResponse(Long accountId, String accountNumber, LocalDateTime from, LocalDateTime to,
                             BigDecimal closingBalance, BigDecimal totalIn, BigDecimal totalOut,
                             List<TransactionResponse> transactions) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.from = from;
        this.to = to;
        this.closingBalance = closingBalance;
        this.totalIn = totalIn;
        this.totalOut = totalOut;
        this.transactions = transactions;
    }

    public Long getAccountId() { return accountId; }

    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountNumber() { return accountNumber; }

    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public LocalDateTime getFrom() { return from; }

    public void setFrom(LocalDateTime from) { this.from = from; }

    public LocalDateTime getTo() { return to; }

    public void setTo(LocalDateTime to) { this.to = to; }

    public BigDecimal getOpeningBalance() { return openingBalance; }

    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getClosingBalance() { return closingBalance; }

    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }

    public BigDecimal getTotalIn() { return totalIn; }

    public void setTotalIn(BigDecimal totalIn) { this.totalIn = totalIn; }

    public BigDecimal getTotalOut() { return totalOut; }

    public void setTotalOut(BigDecimal totalOut) { this.totalOut = totalOut; }

    public List<TransactionResponse> getTransactions() { return transactions; }

    public void setTransactions(List<TransactionResponse> transactions) { this.transactions = transactions; }
}
