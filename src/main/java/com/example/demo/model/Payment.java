package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_id")
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private Long amount;
    private String status;

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
