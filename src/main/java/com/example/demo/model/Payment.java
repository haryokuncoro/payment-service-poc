package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_id")
) @Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private Long amount;
    private String status;
}
