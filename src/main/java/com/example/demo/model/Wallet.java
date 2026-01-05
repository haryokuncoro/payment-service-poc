package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private Long userId;

    private Long balance;

    @Version
    private Long version;

    public Long getUserId() {
        return userId;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
