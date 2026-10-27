package com.ivan.bankapi.dto;

import jakarta.validation.constraints.NotNull;

public class CreateAccountRequest {

    @NotNull
    private Long ownerId;

    private String currency = "PLN";

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}