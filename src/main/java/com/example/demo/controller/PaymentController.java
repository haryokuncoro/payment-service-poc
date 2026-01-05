package com.example.demo.controller;

import com.example.demo.model.Payment;
import com.example.demo.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public Payment pay(
            @RequestHeader("Idempotency-Key") String key,
            @RequestParam Long userId,
            @RequestParam String orderId,
            @RequestParam Long amount
    ) {
        return service.pay(userId, orderId, amount, key);
    }
}
