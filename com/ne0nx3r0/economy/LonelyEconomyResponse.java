package com.ne0nx3r0.economy;

import java.math.BigDecimal;


//Lonely Economy Response
public class LonelyEconomyResponse {
    private final LonelyEconomyResponseType response;
    private String message;
    private BigDecimal balance;
    
    public LonelyEconomyResponse(LonelyEconomyResponseType response) {
        this.response = response;
    }
    
    public LonelyEconomyResponse setMessage(String message) {
        this.message = message;
        
        return this;
    }
    
    public String getMessage() {
        return this.message;
    }
    
    public LonelyEconomyResponseType getType() {
        return this.response;
    }
    
    public boolean wasSuccessful() {
        return this.response == LonelyEconomyResponseType.SUCCESS;
    }

    public LonelyEconomyResponse setBalance(BigDecimal amount) {
        this.balance = amount;
        
        return this;
    }
    
    public BigDecimal getBalance() {
        return this.balance;
    }
}
