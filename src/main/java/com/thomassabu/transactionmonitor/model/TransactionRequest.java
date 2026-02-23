package com.thomassabu.transactionmonitor.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a transaction (request body).
 */
public class TransactionRequest {

    @NotNull(message = "userId must not be null")
    @Size(min = 1, max = 255)
    private String userId;

    @NotNull(message = "amount must not be null")
    @Min(value = 0, message = "amount must be non-negative")
    private Double amount;

    @NotNull(message = "timestamp must not be null")
    private java.time.LocalDateTime timestamp;

    @NotNull(message = "riskScore must not be null")
    @Min(value = 0, message = "riskScore must be between 0 and 100")
    @Max(value = 100, message = "riskScore must be between 0 and 100")
    private Integer riskScore;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.time.LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }
}
